package com.dajudge.testee.jdbc;

import com.dajudge.testee.exceptions.TesteeException;
import com.dajudge.testee.spi.ConnectionFactory;
import com.dajudge.testee.spi.ResourceProvider;
import com.dajudge.testee.utils.AnnotationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toMap;

public class JdbcResourceProvider implements ResourceProvider {
    private static final Logger LOG = LoggerFactory.getLogger(JdbcResourceProvider.class);

    @Resource(mappedName = "testee/testSetupClass")
    private Class<?> testSetupClass;

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
            return dataSources.get(mappedName);
        }
    }

    private Map<String, ConnectionFactory> connectionFactories() {
        if (connectionFactories == null) {
            connectionFactories = discover(testSetupClass);
        }
        return connectionFactories;
    }

    private static Map<String, ConnectionFactory> discover(final Class<?> testClass) {
        return collectDataSources(testClass).stream()
                .collect(toMap(
                        it -> it.name(),
                        it -> initialize(it)
                ));
    }

    private static List<TestDataSource> collectDataSources(final Class<?> testClass) {
        return AnnotationUtils.collectAnnotations(testClass, TestDataSource.class);
    }

    private static ConnectionFactory initialize(final TestDataSource testDataSource) {
        final Class<? extends ConnectionFactory> factoryClass = testDataSource.factory();
        try {
            return factoryClass.newInstance();
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new TesteeException("Failed to instantiate " + factoryClass.getName(), e);
        }
    }

    public void shutdown(final boolean rollback) {
        final Consumer<TesteeDataSource> action = rollback
                ? TesteeDataSource::rollback
                : TesteeDataSource::commit;
        dataSources.values().forEach(action);
        dataSources.values().forEach(TesteeDataSource::close);
    }
}
