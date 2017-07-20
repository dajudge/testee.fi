package com.dajudge.testee.services;

import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.injection.spi.EjbInjectionServices;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

import javax.ejb.EJB;
import javax.enterprise.inject.spi.InjectionPoint;
import java.lang.reflect.Type;

/**
 * Implementation of {@link EjbInjectionServices}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class EjbInjectionServicesImpl implements EjbInjectionServices {
    private final EjbLookup lookup;
    private final EjbFactory factory;

    public EjbInjectionServicesImpl(
            final EjbLookup lookup,
            final EjbFactory factory
    ) {
        this.lookup = lookup;
        this.factory = factory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ResourceReferenceFactory<Object> registerEjbInjectionPoint(final InjectionPoint injectionPoint) {
        if (injectionPoint.getAnnotated().getAnnotation(EJB.class) != null) {
            final Type type = injectionPoint.getType();
            final EjbDescriptor<Object> descriptor = (EjbDescriptor<Object>) lookup.lookup(type);
            return factory.createInstance(descriptor);
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

    public interface EjbLookup {
        EjbDescriptor<?> lookup(Type type);
    }

    public interface EjbFactory {
        <T> ResourceReferenceFactory<T> createInstance(EjbDescriptor<T> descriptor);
    }
}
