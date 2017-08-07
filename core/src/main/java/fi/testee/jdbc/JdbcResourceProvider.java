/*
 * Copyright (C) 2017 Alex Stockinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fi.testee.jdbc;

import fi.testee.exceptions.TestEEfiException;
import fi.testee.spi.ConnectionFactory;
import fi.testee.spi.ResourceProvider;
import fi.testee.spi.scope.TestSetupScope;
import fi.testee.utils.AnnotationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toMap;

@TestSetupScope
public class JdbcResourceProvider implements ResourceProvider {
    private static final Logger LOG = LoggerFactory.getLogger(JdbcResourceProvider.class);

    @Resource(mappedName = "testeefi/setup/class")
    private Class<?> testSetupClass;
    @Resource(mappedName = "testeefi/setup/connectionFactoryManager")
    private ConnectionFactoryManager connectionFactoryManager;
    @Resource(mappedName = "testeefi/setup/rollbackTransactions")
    private boolean rollbackTransactions;

    private final Map<String, TesteeDataSource> dataSources = new HashMap<>();
    private Map<String, ConnectionFactory> connectionFactories;

    @Override
    public Object resolve(final InjectionPoint injectionPoint) {
        if (injectionPoint.getType() != DataSource.class) {
            return null;
        }
        final Resource annotation = injectionPoint.getAnnotated().getAnnotation(Resource.class);
        if (annotation == null) {
            return null;
        }
        return resolve(null, annotation.mappedName());
    }

    @Override
    public Object resolve(final String jndiName, final String mappedName) {
        synchronized (this) {
            if (!dataSources.containsKey(mappedName) && connectionFactories().containsKey(mappedName)) {
                final Supplier<Connection> factory = () -> connectionFactories()
                        .get(mappedName)
                        .createConnection(testSetupClass.getName() + ":" + mappedName);
                dataSources.put(mappedName, new TesteeDataSource(mappedName, factory));
            }
            if (!dataSources.containsKey(mappedName)) {
                throw new TestEEfiException("Unknown JDBC data source: " + mappedName);
            }
            return dataSources.get(mappedName);
        }
    }

    private Map<String, ConnectionFactory> connectionFactories() {
        if (connectionFactories == null) {
            connectionFactories = discover(testSetupClass);
        }
        return connectionFactories;
    }

    private Map<String, ConnectionFactory> discover(final Class<?> testClass) {
        return AnnotationUtils.collectAnnotations(testClass, TestDataSource.class).stream()
                .collect(toMap(
                        TestDataSource::name,
                        it -> connectionFactoryManager.getFactoryFor(it)
                ));
    }

    @PreDestroy
    public void shutdown() {
        dataSources.values().forEach(it -> {
            try {
                if (rollbackTransactions) {
                    LOG.trace("Rolling back {}", it);
                    it.rollback();
                } else {
                    LOG.trace("Committing {}", it);
                    it.commit();
                }
            } catch (final SQLException e) {
                throw new TestEEfiException("Failed to shut down JDBC resources", e);
            }
        });
    }
}
