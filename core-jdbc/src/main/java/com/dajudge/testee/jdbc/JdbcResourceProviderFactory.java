package com.dajudge.testee.jdbc;

import com.dajudge.testee.exceptions.TesteeException;
import com.dajudge.testee.jdbc.spi.DataSourceFactory;
import com.dajudge.testee.spi.ResourceProvider;
import com.dajudge.testee.spi.ResourceProviderFactory;

import javax.annotation.Resource;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.dajudge.testee.utils.ProxyUtils.lazy;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

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
                synchronized (container) {
                    if (container.managedDataSources == null) {
                        container.managedDataSources = discover(setupClass);
                    }
                    return JdbcResourceProviderFactory.resolve(container.managedDataSources, injectionPoint);
                }
            }

            @Override
            public void cleanup() {
                synchronized (container) {
                    if (container.managedDataSources != null) {
                        container.managedDataSources.values().forEach(ManagedDataSource::shutdown);
                        container.managedDataSources = null;
                    }
                }
            }
        };
        setupTestData(setupClass, resourceProvider);
        return resourceProvider;
    }

    private void setupTestData(
            final Class<?> setupClass,
            final ResourceProvider resourceProvider
    ) {
        Class<?> currentClass = setupClass;
        final List<Method> methodsToInvoke = new ArrayList<>();
        while (currentClass != null && currentClass != Object.class) {
            final Set<Method> candidates = stream(currentClass.getDeclaredMethods())
                    .filter(it -> it.getAnnotation(TestData.class) != null)
                    .collect(toSet());
            currentClass = currentClass.getSuperclass();
            if (candidates.isEmpty()) {
                continue;
            }
            if (candidates.size() > 1) {
                throw new IllegalStateException("Only one @TestData method allowed per class");
            }
            final Method candidate = candidates.iterator().next();
            if (!Modifier.isStatic(candidate.getModifiers())) {
                throw new IllegalStateException("Methods annotated with @TestData must be static");
            }
            methodsToInvoke.add(0, candidate);
        }
        methodsToInvoke.forEach(it -> {
            try {
                it.setAccessible(true);
                it.invoke(null);
            } catch (final IllegalAccessException | InvocationTargetException e) {
                throw new TesteeException("Failed to invoke @TestData method", e);
            }
        });
    }

    private static DataSource resolve(
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
        final ManagedDataSource ds = managedDataSources.get(annotation.mappedName());
        if (ds == null) {
            return null;
        }
        return lazy(() -> ds.factory.create(), DataSource.class);
    }

    private static class ManagedDataSource {
        private final String name;
        private final DataSourceFactory factory;

        ManagedDataSource(final String name, final DataSourceFactory factory) {
            this.name = name;
            this.factory = factory;
        }

        private void shutdown() {
            factory.shutdown();
        }
    }

    private Map<String, ManagedDataSource> discover(final Class<?> testClass) {
        final TestDataSource[] annotations = testClass.getAnnotationsByType(TestDataSource.class);
        if (annotations == null) {
            return Collections.emptyMap();
        }
        return stream(annotations)
                .map(this::initialize)
                .collect(toMap(
                        it -> it.name,
                        it -> it
                ));
    }

    private ManagedDataSource initialize(final TestDataSource testDataSource) {
        final Class<? extends DataSourceFactory> factoryClass = testDataSource.factory();
        try {
            return new ManagedDataSource(testDataSource.name(), factoryClass.newInstance());
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new TesteeException("Failed to instantiate " + factoryClass.getName(), e);
        }
    }
}
