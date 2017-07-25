package fi.testee.jdbc;

import fi.testee.exceptions.TestEEfiException;
import fi.testee.spi.ConnectionFactory;
import fi.testee.spi.ResourceProvider;
import fi.testee.utils.JdbcUtils;
import fi.testee.utils.AnnotationUtils;

import javax.annotation.Resource;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toMap;

public class JdbcResourceProvider implements ResourceProvider {
    @Resource(mappedName = "testeefi/testSetupClass")
    private Class<?> testSetupClass;
    @Resource(mappedName = "testeefi/connectionFactoryManager")
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

    public void shutdown(final boolean rollback) {
        final JdbcUtils.JdbcConsumer<TesteeDataSource> action = rollback
                ? TesteeDataSource::rollback
                : TesteeDataSource::commit;
        dataSources.values().forEach(it -> {
            try {
                action.run(it);
                it.close();
            } catch (final SQLException e) {
                throw new TestEEfiException("Failed to shut down JDBC resources", e);
            }
        });

    }
}
