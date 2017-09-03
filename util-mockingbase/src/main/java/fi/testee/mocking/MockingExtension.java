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

import org.jboss.weld.injection.ForwardingInjectionPoint;
import org.jboss.weld.util.annotated.AnnotatedTypeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
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
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.Producer;
import javax.enterprise.inject.spi.ProducerFactory;
import javax.interceptor.Interceptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

    public <X> void beans(
            final @Observes ProcessAnnotatedType<X> processBean
    ) {
        if (!processBean.getAnnotatedType().isAnnotationPresent(Interceptor.class)) {
            return;
        }
        final FilteringAnnotatedTypeWrapper<X> filtered = new FilteringAnnotatedTypeWrapper<>(
                processBean.getAnnotatedType(),
                it -> it != Priority.class
        );
        processBean.setAnnotatedType(filtered);
    }

    public <T, X> void injectionPoints(
            final @Observes ProcessInjectionPoint<T, X> processInjectionPoint
    ) {
        final Type type = processInjectionPoint.getInjectionPoint().getType();
        final Object mock = mockStore.findFor(type);
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
        mockStore.forEachType(types, (field, mock) -> {
            final Class<?> beanType = mock.getClass();
            final AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(beanType);
            final AnnotatedType<?> wrapped = new AnnotatedTypeWrapper<>(annotatedType, MOCKED);
            final BeanAttributes<?> attributes = beanManager.createBeanAttributes(wrapped);
            final Bean<?> bean = beanManager.createBean(attributes, beanType, factory(mock));
            LOG.trace("Creating CDI mock bean for {}", annotatedType);
            afterBeanDiscovery.addBean(bean);
        });
    }

    private <T> ProducerFactory<T> factory(final Object mock) {
        return new ProducerFactory<T>() {
            @Override
            public <T1> Producer<T1> createProducer(final Bean<T1> bean) {
                return new Producer<T1>() {

                    @Override
                    public T1 produce(final CreationalContext<T1> ctx) {
                        return (T1) mock;
                    }

                    @Override
                    public void dispose(final T1 instance) {
                    }

                    @Override
                    public Set<InjectionPoint> getInjectionPoints() {
                        return Collections.emptySet();
                    }
                };
            }
        };
    }
}
