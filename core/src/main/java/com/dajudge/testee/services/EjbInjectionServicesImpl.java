package com.dajudge.testee.services;

import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.injection.spi.EjbInjectionServices;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

import javax.ejb.EJB;
import javax.enterprise.inject.spi.InjectionPoint;
import java.lang.reflect.Type;
import java.util.function.Function;

/**
 * Implementation of {@link EjbInjectionServices}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class EjbInjectionServicesImpl implements EjbInjectionServices {
    private final Function<Type, EjbDescriptor<?>> descriptorLookup;
    private final Function<EjbDescriptor<?>, ResourceReferenceFactory<Object>> beanFactory;

    public EjbInjectionServicesImpl(
            final Function<Type, EjbDescriptor<?>> descriptorLookup,
            final Function<EjbDescriptor<?>, ResourceReferenceFactory<Object>> beanFactory
    ) {
        this.descriptorLookup = descriptorLookup;
        this.beanFactory = beanFactory;
    }

    @Override
    public ResourceReferenceFactory<Object> registerEjbInjectionPoint(final InjectionPoint injectionPoint) {
        if (injectionPoint.getAnnotated().getAnnotation(EJB.class) != null) {
            final Type type = injectionPoint.getType();
            return beanFactory.apply(descriptorLookup.apply(type));
        }
        throw new IllegalStateException("Unhandled injection point: " + injectionPoint);
    }

    @Override
    public Object resolveEjb(final InjectionPoint injectionPoint) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void cleanup() {

    }
}
