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
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

import java.util.function.Consumer;

public class RootSessionBeanFactory<T> implements SessionBeanFactory<T> {
    private final Consumer<? super T> injection;
    private final EjbDescriptorImpl<T> descriptor;
    private final EjbBridge.ContextFactory contextFactory;

    public RootSessionBeanFactory(
            final Consumer<? super T> injection,
            final EjbDescriptorImpl<T> descriptor,
            final EjbBridge.ContextFactory contextFactory
    ) {
        this.injection = injection;
        this.descriptor = descriptor;
        this.contextFactory = contextFactory;
    }

    @Override
    public EjbDescriptor<T> getDescriptor() {
        return descriptor;
    }

    @Override
    public ResourceReferenceFactory<T> getResourceReferenceFactory() {
        return new SingletonBeanContainer<>(descriptor.getBeanClass(), () -> {
            try {
                final T t = descriptor.getBeanClass().newInstance();
                injection.accept(t);
                return t;
            } catch (final InstantiationException | IllegalAccessException e) {
                throw new TestEEfiException("Failed to instantiate session bean", e);
            }
        }, descriptor.getInterceptorChain(contextFactory));
    }

}
