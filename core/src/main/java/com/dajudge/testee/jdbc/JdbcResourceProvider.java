package com.dajudge.testee.jdbc;

import com.dajudge.testee.exceptions.TesteeException;
import com.dajudge.testee.spi.ConnectionFactory;
import com.dajudge.testee.spi.ResourceProvider;
import com.dajudge.testee.utils.JdbcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.dajudge.testee.utils.AnnotationUtils.collectAnnotations;
import static java.util.stream.Collectors.toMap;

public class JdbcResourceProvider implements ResourceProvider {
    private static final Logger LOG = LoggerFactory.getLogger(JdbcResourceProvider.class);

    @Resource(mappedName = "testee/testSetupClass")
    private Class<?> testSetupClass;
    @Resource(mappedName = "testee/connectionFactoryManager")
    private ConnectionFactoryManager connectionFactoryManager;

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

    private Map<String, ConnectionFactory> discover(final Class<?> testClass) {
        return collectAnnotations(testClass, TestDataSource.class).stream()
                .collect(toMap(
                        it -> it.name(),
                        it -> connectionFactoryManager.getFactoryFor(it)
                ));
    }

    public void shutdown(final boolean rollback) {
        final JdbcUtils.JdbcConsumer<TesteeDataSource> action = rollback
                ? TesteeDataSource::rollback
                : TesteeDataSource::commit;
        dataSources.values().forEach(it -> {
            try {
                action.run(it);
                it.close();
            } catch (final SQLException e) {
                throw new TesteeException("Failed to shut down JDBC resources", e);
            }
        });

    }
}
