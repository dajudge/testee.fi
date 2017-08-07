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
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.context.spi.Contextual;
import javax.persistence.PersistenceContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

/**
 * Bridge between CDI and EJB dependency injection.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class EjbContainer {
    private static final Logger LOG = LoggerFactory.getLogger(EjbContainer.class);
    private final Map<Type, EjbDescriptorImpl<?>> ejbDescriptors;
    private final Map<EjbDescriptor<?>, ResourceReferenceFactory<?>> containers;
    private final Set<SessionBeanHolder<?>> existingBeans = new HashSet<>();

    public interface ContextFactory {
        <T> CreationalContextImpl<T> create(Contextual<T> ctx, ReleaseCallbackHandler releaser);
    }

    public EjbContainer(
            final Set<EjbDescriptorImpl<?>> ejbDescriptors,
            final Function<Object, Collection<ResourceReference<?>>> cdiInjection,
            final Function<Resource, Object> resourceInjection,
            final Function<PersistenceContext, Object> jpaInjection,
            final SessionBeanModifier modifier,
            final ContextFactory contextFactory
    ) {
        LOG.debug("Starting EJB container with EJB descriptors {}", ejbDescriptors);
        final Function<Object, Collection<ResourceReference<?>>> injection = o -> {
            final Collection<ResourceReference<?>> ret = new HashSet<>();
            ret.addAll(cdiInjection.apply(o));
            ret.addAll(ejbInjection(EJB.class, this::injectEjb).apply(o));
            ret.addAll(ejbInjection(Resource.class, injectResources(Resource.class, resourceInjection)).apply(o));
            ret.addAll(ejbInjection(PersistenceContext.class, injectResources(PersistenceContext.class, jpaInjection)).apply(o));
            return ret;
        };

        this.ejbDescriptors = ejbDescriptors.stream().collect(toMap(
                EjbDescriptor::getBeanClass,
                it -> it
        ));

        SessionBeanLifecycleListener lifecycleListener = new SessionBeanLifecycleListener() {
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
        this.containers = ejbDescriptors.stream()
                .collect(toMap(
                        it -> it,
                        it -> createFactory(it, injection, modifier, contextFactory, lifecycleListener)
                                .getResourceReferenceFactory()
                ));
    }

    private <T extends Annotation> BiFunction<Object, Field, ResourceReference<?>> injectResources(
            final Class<T> clazz,
            final Function<T, Object> resourceInjection
    ) {
        return (o, f) -> {
            inject(o, f, resourceInjection.apply(f.getAnnotation(clazz)));
            return null;
        };
    }

    private Function<Object, Collection<ResourceReference<?>>> ejbInjection(
            final Class<? extends Annotation> annotationClass,
            final BiFunction<Object, Field, ResourceReference<?>> injector
    ) {
        return o -> stream(FieldUtils.getAllFields(o.getClass()))
                .filter(it -> it.getAnnotation(annotationClass) != null)
                .map(it -> injector.apply(o, it))
                .filter(Objects::nonNull)
                .collect(toSet());
    }

    private ResourceReference<?> injectEjb(final Object o, final Field field) {
        final ResourceReference<?> ref = createInstance(lookupDescriptor(field.getType())).createResource();
        inject(o, field, ref.getInstance());
        return ref;
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
            final EjbDescriptorImpl<T> desc,
            final Function<? super T, Collection<ResourceReference<?>>> injection,
            final SessionBeanModifier modifier,
            final ContextFactory contextFactory,
            final SessionBeanLifecycleListener lifecycleListener
    ) {
        final RootSessionBeanFactory<T> root = new RootSessionBeanFactory<T>(
                injection,
                desc,
                contextFactory,
                lifecycleListener
        );
        return modifier.modify(root);
    }

    public EjbDescriptor<?> lookupDescriptor(final Type type) {
        return ejbDescriptors.get(type);
    }

    @SuppressWarnings("unchecked")
    public <T> ResourceReferenceFactory<T> createInstance(final EjbDescriptor<T> descriptor) {
        return (ResourceReferenceFactory<T>) containers.get(descriptor);
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

    public static final SessionBeanModifier IDENTITY = new IdentitySessionBeanModifier();

    public void shutdown() {
        // This cleans up remaining beans that were part of reference cycles
        while (!existingBeans.isEmpty()) {
            existingBeans.iterator().next().forceDestroy();
        }
    }
}
