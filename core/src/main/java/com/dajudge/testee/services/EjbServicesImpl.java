package com.dajudge.testee.services;

import com.dajudge.testee.utils.MutableContainer;
import org.jboss.weld.ejb.api.SessionObjectReference;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.ejb.spi.InterceptorBindings;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

import java.util.function.Function;

public class EjbServicesImpl implements EjbServices {

    private final Function<EjbDescriptor<?>, ResourceReferenceFactory<Object>> beanFactory;

    public EjbServicesImpl(final Function<EjbDescriptor<?>, ResourceReferenceFactory<Object>> beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public SessionObjectReference resolveEjb(final EjbDescriptor<?> ejbDescriptor) {
        final MutableContainer<Boolean> removed = new MutableContainer<>(false);
        final ResourceReferenceFactory<Object> reference = beanFactory.apply(ejbDescriptor);
        return new SessionObjectReference() {
            @Override
            public <S> S getBusinessObject(final Class<S> businessInterfaceType) {
                return (S) reference.createResource().getInstance();
            }

            @Override
            public void remove() {

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

    }
}
