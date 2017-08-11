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

import fi.testee.deployment.EjbDescriptorImpl;
import fi.testee.exceptions.TestEEfiException;
import fi.testee.spi.ReleaseCallbackHandler;
import fi.testee.spi.SessionBeanFactory;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jboss.weld.context.CreationalContextImpl;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.injection.spi.EjbInjectionServices;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;
import org.jboss.weld.manager.BeanManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.PersistenceContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
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
public class EjbContainer {

    private static final Logger LOG = LoggerFactory.getLogger(EjbContainer.class);
    private final Set<SessionBeanHolder<?>> existingBeans = new HashSet<>();
    private Map<Type, EjbDescriptorImpl<?>> ejbDescriptors;
    private Map<EjbDescriptor<?>, ResourceReferenceFactory<?>> containers;
    private EjbInjectionServices ejbInjectionServices;

    public interface EjbDescriptorHolderResolver {

        <T> EjbDescriptorHolder<T> resolve(EjbDescriptorImpl<T> descriptor);
    }

    public interface Injection<A extends Annotation> {
        Object instantiate(final Field field, final Bean<?> bean, final BeanManagerImpl beanManager);
    }

    public interface ContextFactory {
        <T> CreationalContextImpl<T> create(Contextual<T> ctx, ReleaseCallbackHandler releaser);
    }

    public EjbContainer(
            final Set<EjbDescriptorImpl<?>> ejbDescriptors
    ) {
        this.ejbDescriptors = ejbDescriptors.stream().collect(toMap(
                it -> it.getBeanClass(),
                it -> it
        ));
    }

    public void init(
            final EjbDescriptorHolderResolver holderResolver,
            final Function<Object, Collection<ResourceReference<?>>> cdiInjection,
            final Injection<Resource> resourceInjection,
            final Injection<PersistenceContext> jpaInjection,
            final Injection<EJB> ejbInjection,
            final ContextFactory contextFactory
    ) {
        LOG.debug("Starting EJB container with EJB descriptors {}", ejbDescriptors);
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
                        it -> createFactory(holderResolver.resolve(it), injection, contextFactory, lifecycleListener)
                                .getResourceReferenceFactory()
                ));
    }

    private ResourceReference<?> injectEjb(
            final Object o,
            final Field field,
            final Bean<?> bean,
            final BeanManager beanManager
    ) {
        final ResourceReference<?> ref = createInstance(lookupDescriptor(field.getType())).createResource();
        inject(o, field, ref.getInstance());
        return ref;
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

    interface EjbInjection {
        Collection<ResourceReference<?>> instantiateAll(
                final Object o,
                final Bean<?> bean,
                final BeanManagerImpl beanManager
        );
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
            final EjbDescriptorHolder<T> desc,
            final EjbInjection injection,
            final ContextFactory contextFactory,
            final SessionBeanLifecycleListener lifecycleListener
    ) {
        return new RootSessionBeanFactory<T>(
                injection,
                desc.getBean(),
                desc.getBeanManager(),
                desc.getDescriptor(),
                contextFactory,
                lifecycleListener
        );
    }

    public EjbDescriptor<?> lookupDescriptor(final Type type) {
        for (final Map.Entry<Type, EjbDescriptorImpl<?>> e : ejbDescriptors.entrySet()) {
            if(isAssignableFrom(type, e.getKey())) {
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


    public interface SessionBeanModifier {
        <T> SessionBeanFactory<T> modify(SessionBeanFactory<T> factory);
    }

    private static class IdentitySessionBeanModifier implements SessionBeanModifier {

        @Override
        public <T> SessionBeanFactory<T> modify(final SessionBeanFactory<T> factory) {
            return factory;
        }
    }

    public void shutdown() {
        // This cleans up remaining beans that were part of reference cycles
        while (!existingBeans.isEmpty()) {
            existingBeans.iterator().next().forceDestroy();
        }
    }
}
