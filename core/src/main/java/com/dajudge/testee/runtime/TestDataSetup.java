package com.dajudge.testee.runtime;

import com.dajudge.testee.exceptions.TesteeException;
import com.dajudge.testee.jdbc.TestData;
import com.dajudge.testee.jdbc.TestDataSources;
import com.dajudge.testee.jpa.TestPersistenceUnits;
import org.jboss.weld.bean.CommonBean;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.serialization.spi.BeanIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceProperty;
import javax.persistence.SynchronizationType;
import javax.sql.DataSource;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;

/**
 * Control of {@link TestData @TestData setup}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
final class TestDataSetup {
    private static final Logger LOG = LoggerFactory.getLogger(TestDataSetup.class);

    static void setupTestData(
            final Class<?> setupClass,
            final ServiceRegistry serviceRegistry
    ) {
        final Set<Object> testDataSetupAccessors = new HashSet<>(asList(
                testDataSources(serviceRegistry.get(ResourceInjectionServices.class)),
                testPersistenceUnits(serviceRegistry.get(JpaInjectionServices.class))
        ));
        setupTestData(setupClass, testDataSetupAccessors);
    }

    private static TestPersistenceUnits testPersistenceUnits(final JpaInjectionServices jpaInjectionServices) {
        final Object object = new Object();
        final BeanAttributes<Object> attribuites = new BeanAttributes<Object>() {
            @Override
            public Set<Type> getTypes() {
                return new HashSet<>(Collections.singletonList(Object.class));
            }

            @Override
            public Set<Annotation> getQualifiers() {
                return emptySet();
            }

            @Override
            public Class<? extends Annotation> getScope() {
                return Singleton.class;
            }

            @Override
            public String getName() {
                return Object.class.getName();
            }

            @Override
            public Set<Class<? extends Annotation>> getStereotypes() {
                return emptySet();
            }

            @Override
            public boolean isAlternative() {
                return false;
            }
        };
        final BeanIdentifier identifier = new BeanIdentifier() {
            @Override
            public String asString() {
                return "SomeFakeBean";
            }
        };
        final Set<InjectionPoint> injectionPoints = new HashSet<>();
        final Bean<Object> bean = new CommonBean<Object>(attribuites, identifier) {
            @Override
            public Class<?> getBeanClass() {
                return Object.class;
            }

            @Override
            public Set<InjectionPoint> getInjectionPoints() {
                return injectionPoints;
            }

            @Override
            public Object create(CreationalContext<Object> creationalContext) {
                return object;
            }

            @Override
            public void destroy(Object instance, CreationalContext<Object> creationalContext) {

            }
        };
        return s -> {
            final PersistenceContext persistenceContext = new PersistenceContext() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return PersistenceContext.class;
                }

                @Override
                public String name() {
                    return null;
                }

                @Override
                public String unitName() {
                    return s;
                }

                @Override
                public PersistenceContextType type() {
                    return null;
                }

                @Override
                public SynchronizationType synchronization() {
                    return null;
                }

                @Override
                public PersistenceProperty[] properties() {
                    return new PersistenceProperty[0];
                }
            };
            final Annotated annotated = new Annotated() {
                @Override
                public Type getBaseType() {
                    return Object.class;
                }

                @Override
                public Set<Type> getTypeClosure() {
                    return new HashSet<>(singletonList(Object.class));
                }

                @Override
                public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
                    return annotationType == PersistenceContext.class ? (T) persistenceContext : null;
                }

                @Override
                public Set<Annotation> getAnnotations() {
                    return new HashSet<>(singletonList(persistenceContext));
                }

                @Override
                public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
                    return annotationType == PersistenceContext.class;
                }
            };
            final InjectionPoint injectionPoint = new InjectionPoint() {

                @Override
                public Type getType() {
                    return EntityManager.class;
                }

                @Override
                public Set<Annotation> getQualifiers() {
                    return new HashSet<>();
                }

                @Override
                public Bean<?> getBean() {
                    return bean;
                }

                @Override
                public Member getMember() {
                    return new Member() {
                        @Override
                        public Class<?> getDeclaringClass() {
                            return Object.class;
                        }

                        @Override
                        public String getName() {
                            return "FakeMember";
                        }

                        @Override
                        public int getModifiers() {
                            return 0;
                        }

                        @Override
                        public boolean isSynthetic() {
                            return false;
                        }
                    };
                }

                @Override
                public Annotated getAnnotated() {
                    return annotated;
                }

                @Override
                public boolean isDelegate() {
                    return false;
                }

                @Override
                public boolean isTransient() {
                    return false;
                }
            };
            return (EntityManager) jpaInjectionServices
                    .registerPersistenceContextInjectionPoint(injectionPoint)
                    .createResource()
                    .getInstance();
        };
    }

    private static TestDataSources testDataSources(final ResourceInjectionServices resourceInjectionServices) {
        return mappedName -> (DataSource) resourceInjectionServices
                .registerResourceInjectionPoint(null, mappedName)
                .createResource()
                .getInstance();
    }

    public static void setupTestData(final Class<?> setupClass, final Collection<Object> testDataSetupAccessors) {
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
            LOG.debug("Invoking @TestData method {}", it.toString());
            try {
                it.setAccessible(true);
                final Object[] arguments = resolveArguments(it.getParameterTypes(), testDataSetupAccessors);
                it.invoke(null, arguments);
            } catch (final IllegalAccessException | InvocationTargetException e) {
                throw new TesteeException("Failed to invoke @TestData method", e);
            }
        });
    }

    private static Object[] resolveArguments(final Class<?>[] types, final Collection<Object> candidates) {
        final Object[] values = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            values[i] = find(types[i], candidates);
        }
        return values;
    }

    private static Object find(final Class<?> type, final Collection<Object> candidates) {
        final Set<Object> matches = new HashSet<>();
        for (final Object candidate : candidates) {
            if (type.isAssignableFrom(candidate.getClass())) {
                matches.add(candidate);
            }
        }
        if (matches.isEmpty()) {
            throw new TesteeException("Unkonwn type for @TestData method parameter: " + type.getName());
        }
        if (matches.size() > 1) {
            throw new TesteeException("Ambiguous @TestData method parameter type: " + type.getName());
        }
        return matches.iterator().next();
    }

}
