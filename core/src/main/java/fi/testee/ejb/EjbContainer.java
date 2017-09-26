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
package fi.testee.ejb;

import fi.testee.exceptions.TestEEfiException;
import fi.testee.utils.MutableContainer;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jboss.weld.bean.SessionBean;
import org.jboss.weld.ejb.api.SessionObjectReference;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.ejb.spi.InterceptorBindings;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;
import org.jboss.weld.manager.BeanManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.inject.spi.Bean;
import javax.persistence.PersistenceContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.jboss.weld.resolution.CovariantTypes.isAssignableFrom;

/**
 * Bridge between CDI and EJB dependency injection.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class EjbContainer implements EjbServices {
    private static final Logger LOG = LoggerFactory.getLogger(EjbContainer.class);

    private final Set<SessionBeanHolder<?>> existingBeans = new HashSet<>();
    private Map<Type, EjbDescriptor<?>> ejbDescriptors;
    private final InterceptorInvocationUtil.InterceptorChain interceptorChain;
    private Map<EjbDescriptor<?>, ResourceReferenceFactory<?>> containers;
    private final Map<EjbDescriptor, InterceptorBindings> interceptorBindings = new HashMap<>();

    public EjbContainer(
            final Collection<EjbDescriptor<?>> ejbDescriptors,
            final InterceptorInvocationUtil.InterceptorChain interceptorChain
    ) {
        this.ejbDescriptors = ejbDescriptors.stream().collect(toMap(
                it -> it.getBeanClass(),
                it -> it
        ));
        this.interceptorChain = interceptorChain;
    }

    public void init(
            final Function<Object, Collection<ResourceReference<?>>> cdiInjection,
            final Injection<Resource> resourceInjection,
            final Injection<PersistenceContext> jpaInjection,
            final Injection<EJB> ejbInjection,
            final Function<Class<?>, BeanManagerImpl> archiveResolver
    ) {
        LOG.debug("Starting EJB container with descriptors {}", ejbDescriptors);
        final EjbInjection injection = (o, b, m) -> {
            final Collection<ResourceReference<?>> ret = new HashSet<>();
            ret.addAll(cdiInjection.apply(o));
            ret.addAll(ejbInjection(EJB.class, injectResources(ejbInjection)).instantiateAll(o, b, m));
            ret.addAll(ejbInjection(Resource.class, injectResources(resourceInjection)).instantiateAll(o, b, m));
            ret.addAll(ejbInjection(PersistenceContext.class, injectResources(jpaInjection)).instantiateAll(o, b, m));
            return ret;
        };

        final SessionBeanLifecycleListener lifecycleListener = new SessionBeanLifecycleListener() {
            @Override
            public void constructed(final SessionBeanHolder<?> holder) {
                synchronized (existingBeans) {
                    existingBeans.add(holder);
                }
            }

            @Override
            public void destroyed(final SessionBeanHolder<?> holder) {
                synchronized (existingBeans) {
                    existingBeans.remove(holder);
                }
            }
        };
        this.containers = ejbDescriptors.values().stream()
                .collect(toMap(
                        it -> it,
                        it -> createFactory(
                                injection,
                                lifecycleListener,
                                resolveHolder(it, archiveResolver)
                        ).getResourceReferenceFactory()
                ));
    }

    private <T> EjbDescriptorHolder<T> resolveHolder(
            final EjbDescriptor<T> desc,
            final Function<Class<?>, BeanManagerImpl> archiveResolver
    ) {
        final BeanManagerImpl archive = archiveResolver.apply(desc.getBeanClass());
        final SessionBean<T> sessionBean = archive.getBean(desc);
        if (sessionBean == null) {
            throw new TestEEfiException("Failed to find session bean for " + desc);
        }
        final InterceptorBindings interceptorBindings = getInterceptorBindings(desc);
        return new EjbDescriptorHolder<T>(desc, interceptorChain, sessionBean, archive, interceptorBindings);
    }

    @Override
    public SessionObjectReference resolveEjb(EjbDescriptor<?> ejbDescriptor) {
        final MutableContainer<Boolean> removed = new MutableContainer<>(false);
        final ResourceReferenceFactory<Object> reference = createInstance((EjbDescriptor<Object>) ejbDescriptor);
        return new SessionObjectReference() {
            @Override
            public <S> S getBusinessObject(final Class<S> businessInterfaceType) {
                return (S) reference.createResource().getInstance();
            }

            @Override
            public void remove() {
                // TODO do we have to do something here?
            }

            @Override
            public boolean isRemoved() {
                return removed.getObject();
            }
        };
    }

    @Override
    public void registerInterceptors(
            final EjbDescriptor<?> ejbDescriptor,
            final InterceptorBindings interceptorBindings
    ) {
        LOG.debug(
                "Registering interceptors {} on {}",
                interceptorBindings.getAllInterceptors(),
                ejbDescriptor.getBeanClass()
        );
        this.interceptorBindings.put(ejbDescriptor, interceptorBindings);
    }

    @Override
    public void cleanup() {

    }

    public <T> InterceptorBindings getInterceptorBindings(final EjbDescriptor<T> desc) {
        return interceptorBindings.get(desc);
    }

    private interface Injector {
        ResourceReference<?> instantiate(
                final Object o,
                final Field f,
                final Bean<?> bean,
                final BeanManagerImpl beanManager
        );
    }

    private <T extends Annotation> Injector injectResources(
            final Injection<T> injection
    ) {
        return (o, f, b, m) -> {
            final Object instance = injection.instantiate(f, b, m);
            inject(o, f, instance);
            return null;
        };
    }

    private EjbInjection ejbInjection(
            final Class<? extends Annotation> annotationClass,
            final Injector injector
    ) {
        return (o, b, m) -> stream(FieldUtils.getAllFields(o.getClass()))
                .filter(f -> f.getAnnotation(annotationClass) != null)
                .map(f -> injector.instantiate(o, f, b, m))
                .filter(Objects::nonNull)
                .collect(toSet());
    }

    private void inject(final Object o, final Field field, final Object instanceToInject) {
        try {
            field.setAccessible(true);
            field.set(o, instanceToInject);
        } catch (final IllegalAccessException e) {
            throw new TestEEfiException("Failed to inject into field", e);
        }
    }

    private <T> SessionBeanFactory<T> createFactory(
            final EjbInjection injection,
            final SessionBeanLifecycleListener lifecycleListener,
            final EjbDescriptorHolder<T> holder
    ) {
        return new SessionBeanFactory<T>(injection, holder, lifecycleListener);
    }

    public EjbDescriptor<?> lookupDescriptor(final Type type) {
        for (final Map.Entry<Type, EjbDescriptor<?>> e : ejbDescriptors.entrySet()) {
            if (isAssignableFrom(type, e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> ResourceReferenceFactory<T> createInstance(final EjbDescriptor<T> descriptor) {
        assert descriptor != null;
        return () -> (ResourceReference<T>) containers.get(descriptor).createResource();
    }

    public void shutdown() {
        // This cleans up remaining beans that were part of reference cycles
        while (!existingBeans.isEmpty()) {
            existingBeans.iterator().next().forceDestroy();
        }
    }

    public interface Injection<A extends Annotation> {
        Object instantiate(Field field, Bean<?> bean, BeanManagerImpl beanManager);
    }

    interface EjbInjection {
        Collection<ResourceReference<?>> instantiateAll(
                final Object o,
                final Bean<?> bean,
                final BeanManagerImpl beanManager
        );
    }
}
