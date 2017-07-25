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
package fi.testee.services;

import fi.testee.utils.MutableContainer;
import org.jboss.weld.ejb.api.SessionObjectReference;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.ejb.spi.InterceptorBindings;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

public class EjbServicesImpl implements EjbServices {

    private final EjbInjectionServicesImpl.EjbFactory beanFactory;

    public EjbServicesImpl(final EjbInjectionServicesImpl.EjbFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SessionObjectReference resolveEjb(final EjbDescriptor<?> ejbDescriptor) {
        final MutableContainer<Boolean> removed = new MutableContainer<>(false);
        final ResourceReferenceFactory<Object> reference = beanFactory
                .createInstance((EjbDescriptor<Object>) ejbDescriptor);
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
    public void registerInterceptors(final EjbDescriptor<?> ejbDescriptor, final InterceptorBindings interceptorBindings) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void cleanup() {
        // TODO do we have to do something here?
    }
}
