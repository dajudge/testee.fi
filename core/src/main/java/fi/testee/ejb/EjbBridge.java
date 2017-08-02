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
import fi.testee.spi.SessionBeanFactory;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jboss.weld.context.CreationalContextImpl;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.context.spi.Contextual;
import javax.persistence.PersistenceContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

/**
 * Bridge between CDI and EJB dependency injection.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class EjbBridge {
    private final Map<Type, EjbDescriptorImpl<?>> ejbDescriptors;
    private final Map<EjbDescriptor<?>, ResourceReferenceFactory<?>> containers;

    public interface ContextFactory {
        <T> CreationalContextImpl<T> create(Contextual<T> ctx);
    }

    public EjbBridge(
            final Set<EjbDescriptorImpl<?>> ejbDescriptors,
            final Consumer<Object> cdiInjection,
            final Function<Resource, Object> resourceInjection,
            final Function<PersistenceContext, Object> jpaInjection,
            final SessionBeanModifier modifier,
            final ContextFactory contextFactory
            ) {
        final Consumer<Object> injection = cdiInjection
                .andThen(ejbInjection(EJB.class, this::injectEjb))
                .andThen(ejbInjection(Resource.class, injectResources(Resource.class, resourceInjection)))
                .andThen(ejbInjection(PersistenceContext.class, injectResources(PersistenceContext.class, jpaInjection)));

        this.ejbDescriptors = ejbDescriptors.stream().collect(toMap(
                EjbDescriptor::getBeanClass,
                it -> it
        ));

        this.containers = ejbDescriptors.stream().collect(toMap(
                it -> it,
                it -> toBeanContainer(it, injection, modifier, contextFactory)
        ));
    }

    private <T extends Annotation> BiConsumer<Object, Field> injectResources(
            final Class<T> clazz,
            final Function<T, Object> resourceInjection
    ) {
        return (o, f) -> inject(o, f, resourceInjection.apply(f.getAnnotation(clazz)));
    }

    private Consumer<Object> ejbInjection(
            final Class<? extends Annotation> annotationClass,
            final BiConsumer<Object, Field> injector
    ) {
        return o -> stream(FieldUtils.getAllFields(o.getClass()))
                .filter(it -> it.getAnnotation(annotationClass) != null)
                .forEach(it -> injector.accept(o, it));
    }

    private void injectEjb(final Object o, final Field field) {
        inject(o, field, createInstance(lookupDescriptor(field.getType())).createResource().getInstance());
    }

    private void inject(final Object o, final Field field, final Object instanceToInject) {
        try {
            field.setAccessible(true);
            field.set(o, instanceToInject);
        } catch (final IllegalAccessException e) {
            throw new TestEEfiException("Failed to inject into field", e);
        }
    }

    private <T> ResourceReferenceFactory<T> toBeanContainer(
            final EjbDescriptorImpl<T> desc,
            final Consumer<? super T> injection,
            final SessionBeanModifier modifier,
            final ContextFactory contextFactory
    ) {
        final RootSessionBeanFactory<T> root = new RootSessionBeanFactory<T>(
                injection,
                desc,
                contextFactory
        );
        return modifier.modify(root).getResourceReferenceFactory();
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

    public static final SessionBeanModifier IDENTITY_SESSION_BEAN_MODIFIER = new IdentitySessionBeanModifier();
}
