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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class SessionBeanFactory<T> {
    private static final Logger LOG = LoggerFactory.getLogger(SessionBeanFactory.class);

    private final EjbContainer.EjbInjection injection;
    private final EjbDescriptorHolder<T> holder;
    private final SessionBeanLifecycleListener lifecycleListener;

    public SessionBeanFactory(
            final EjbContainer.EjbInjection injection,
            final EjbDescriptorHolder<T> holder,
            final SessionBeanLifecycleListener lifecycleListener
    ) {
        this.injection = injection;
        this.holder = holder;
        this.lifecycleListener = lifecycleListener;
    }

    public ResourceReferenceFactory<T> getResourceReferenceFactory() {
        final EjbDescriptor<T> descriptor = holder.getDescriptor();
        LOG.debug("Creating session bean holder for {}", descriptor.getBeanClass());
        final SingletonHolder<T> singletonHolder = new SingletonHolder<>(
                descriptor.getBeanClass(),
                this::createNewInstance,
                holder.getInterceptorChain(),
                holder.getInterceptorBindings()
        );
        singletonHolder.addLifecycleListener(lifecycleListener);
        return singletonHolder;
    }

    private Pair<T, Collection<ResourceReference<?>>> createNewInstance() {
        final EjbDescriptor<T> descriptor = holder.getDescriptor();
        LOG.debug("Creating new instance of {}", descriptor.getBeanClass());
        try {
            final T beanInstance = descriptor.getBeanClass().newInstance();
            final Collection<ResourceReference<?>> references = injection.instantiateAll(
                    beanInstance,
                    holder.getBean(),
                    holder.getBeanManager()
            );
            return new ImmutablePair<>(beanInstance, references);
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new TestEEfiException("Failed to instantiate session bean", e);
        }
    }

}
