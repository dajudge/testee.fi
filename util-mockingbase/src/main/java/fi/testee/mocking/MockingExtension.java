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
package fi.testee.mocking;

import fi.testee.exceptions.TestEEfiException;
import fi.testee.mocking.annotation.InjectMock;
import org.jboss.weld.injection.ForwardingInjectionPoint;
import org.jboss.weld.util.annotated.AnnotatedTypeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.ProducerFactory;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

public class MockingExtension implements Extension {
    private static final Logger LOG = LoggerFactory.getLogger(MockingExtension.class);
    private static final Annotation MOCKED = new Mocked() {
        @Override
        public Class<? extends Annotation> annotationType() {
            return Mocked.class;
        }
    };

    private final MockingDynamicArchiveContributor contributor;
    private final MockStore mockStore;
    private final Collection<InjectionPoint> mockedInjectionPoints = new HashSet<>();

    public MockingExtension(
            final MockingDynamicArchiveContributor contributor,
            final MockStore mockStore
    ) {
        this.contributor = contributor;
        this.mockStore = mockStore;
    }

    public <T> void initializePropertyLoading(final @Observes ProcessInjectionTarget<T> pit) {
        final InjectionTarget<T> delegate = pit.getInjectionTarget();
        final InjectionTarget<T> wrapped = new ForwardingInjectionTarget<T>(delegate) {
            @Override
            public void inject(final T instance, final CreationalContext<T> ctx) {
                super.inject(instance, ctx);
                injectMocks(instance, instance.getClass());
            }
        };
        pit.setInjectionTarget(wrapped);
    }

    private <T> void injectMocks(final T instance, final Class<?> clazz) {
        if (clazz == Object.class || clazz == null) {
            return;
        }
        stream(clazz.getDeclaredFields())
                .peek(it -> it.setAccessible(true))
                .filter(it -> injectsMock(instance, it))
                .forEach(it -> injectMock(instance, it));
    }

    private <T> void injectMock(final T instance, final Field field) {
        mockStore.forEachType(asList(field.getType()), false, (mockField, mock) -> {
            try {
                field.set(instance, mock);
            } catch (final IllegalAccessException e) {
                throw new TestEEfiException("Failed to inject mock", e);
            }
        });
    }

    private boolean injectsMock(final Object instance, final Field field) {
        try {
            if (field.get(instance) != null) {
                return false;
            }
            if (field.getAnnotation(InjectMock.class) == null) {
                return false;
            }
            return true;
        } catch (final IllegalAccessException e) {
            throw new TestEEfiException("Failed access member while processing @InjectMock annotations", e);
        }
    }


    public <T, X> void injectionPoints(
            final @Observes ProcessInjectionPoint<T, X> processInjectionPoint
    ) {
        final Type type = processInjectionPoint.getInjectionPoint().getType();
        final Object mock = mockStore.findFor(type, true);
        if (mock == null) {
            return;
        }

        LOG.debug("Mocking injection point: {}", processInjectionPoint.getInjectionPoint());
        final InjectionPoint original = processInjectionPoint.getInjectionPoint();
        processInjectionPoint.setInjectionPoint(new ForwardingInjectionPoint() {
            @Override
            public Set<Annotation> getQualifiers() {
                final Set<Annotation> ret = new HashSet<>(super.getQualifiers()).stream()
                        .filter(it -> !(it instanceof Default))
                        .collect(toSet());
                ret.add(MOCKED);
                return ret;
            }

            @Override
            protected InjectionPoint delegate() {
                return original;
            }
        });
        mockedInjectionPoints.add(original);
    }

    public void afterBeanDiscovery(
            final @Observes AfterBeanDiscovery afterBeanDiscovery,
            final BeanManager beanManager
    ) {
        final Collection<Type> types = mockedInjectionPoints.stream()
                .map(it -> it.getType())
                .collect(toSet());
        mockStore.forEachType(types, true, (field, mock) -> {
            final Class<?> beanType = mock.getClass();
            final AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(field.getType());
            final AnnotatedType<?> wrapped = new AnnotatedTypeWrapper<>(annotatedType, MOCKED);
            LOG.info("Creating mock bean for {} -> {}", field.getType(), wrapped.getAnnotations());
            final BeanAttributes<?> attributes = beanManager.createBeanAttributes(wrapped);
            final Bean<?> bean = beanManager.createBean(attributes, beanType, factory(mock));
            LOG.trace("Creating CDI mock bean for {}", annotatedType);
            afterBeanDiscovery.addBean(bean);
        });
    }

    private <T> ProducerFactory<T> factory(final Object mock) {
        return new MockProducerFactory<>(mock);
    }

}
