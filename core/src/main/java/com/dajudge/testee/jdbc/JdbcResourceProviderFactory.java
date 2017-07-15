package com.dajudge.testee.jdbc;

import com.dajudge.testee.exceptions.TesteeException;
import com.dajudge.testee.spi.ConnectionFactory;
import com.dajudge.testee.spi.ResourceProvider;
import com.dajudge.testee.spi.ResourceProviderFactory;

import javax.annotation.Resource;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.sql.DataSource;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

/**
 * Contributes JDBC support to TestEE core.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class JdbcResourceProviderFactory implements ResourceProviderFactory {

    @Override
    public ResourceProvider create(final Class<?> setupClass) {
        class Container {
            private Map<String, ManagedDataSource> managedDataSources;
        }
        final Container container = new Container();
        final ResourceProvider resourceProvider = new ResourceProvider() {
            @Override
            public Object resolve(final InjectionPoint injectionPoint) {
                return resolve(map -> resolveDataSource(map, injectionPoint));
            }

            @Override
            public Object resolve(String jndiName, String mappedName) {
                return resolve(map -> resolveDataSource(map, mappedName));
            }

            private Object resolve(Function<Map<String, ManagedDataSource>, DataSource> resolver) {
                synchronized (container) {
                    if (container.managedDataSources == null) {
                        container.managedDataSources = discover(setupClass);
                    }
                    return resolver.apply(container.managedDataSources);
                }
            }

            @Override
            public void shutdown() {
                synchronized (container) {
                    if (container.managedDataSources != null) {
                        container.managedDataSources.values().forEach(ManagedDataSource::shutdown);
                        container.managedDataSources = null;
                    }
                }
            }
        };
        return resourceProvider;
    }

    private static DataSource resolveDataSource(
            final Map<String, ManagedDataSource> managedDataSources,
            final InjectionPoint injectionPoint
    ) {
        if (injectionPoint.getType() != DataSource.class) {
            return null;
        }
        final Resource annotation = injectionPoint.getAnnotated().getAnnotation(Resource.class);
        if (annotation == null) {
            return null;
        }
        return resolveDataSource(managedDataSources, annotation.mappedName());
    }

    private static DataSource resolveDataSource(
            final Map<String, ManagedDataSource> managedDataSources,
            final String mappedName
    ) {
        final ManagedDataSource ds = managedDataSources.get(mappedName);
        if (ds == null) {
            return null;
        }
        return new DataSourceImpl(ds.getConnection());
    }

    private Map<String, ManagedDataSource> discover(final Class<?> testClass) {
        final TestDataSource[] annotations = testClass.getAnnotationsByType(TestDataSource.class);
        if (annotations == null) {
            return Collections.emptyMap();
        }
        return stream(annotations)
                .map(this::initialize)
                .collect(toMap(
                        it -> it.getName(),
                        it -> it
                ));
    }

    private ManagedDataSource initialize(final TestDataSource testDataSource) {
        final Class<? extends ConnectionFactory> factoryClass = testDataSource.factory();
        try {
            return new ManagedDataSource(
                    testDataSource.name(),
                    factoryClass.newInstance()
            );
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new TesteeException("Failed to instantiate " + factoryClass.getName(), e);
        }
    }
}
