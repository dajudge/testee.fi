package com.dajudge.testee.services;

import org.jboss.weld.injection.spi.EjbInjectionServices;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

import javax.enterprise.inject.spi.InjectionPoint;

/**
 * Implementation of {@link EjbInjectionServices}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class EjbInjectionServicesImpl implements EjbInjectionServices {
    // TODO: find out what this class is supposed to do.
    @Override
    public ResourceReferenceFactory<Object> registerEjbInjectionPoint(final InjectionPoint injectionPoint) {
        return null;
    }

    @Override
    public Object resolveEjb(final InjectionPoint injectionPoint) {
        return null;
    }

    @Override
    public void cleanup() {

    }
}
