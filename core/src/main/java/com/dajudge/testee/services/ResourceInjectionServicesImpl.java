package com.dajudge.testee.services;

import com.dajudge.testee.spi.ResourceProvider;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;
import org.jboss.weld.injection.spi.helpers.SimpleResourceReference;

import javax.enterprise.inject.spi.InjectionPoint;
import java.util.Collection;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * Implementation of {@link ResourceInjectionServices}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class ResourceInjectionServicesImpl implements ResourceInjectionServices {

    private final Collection<ResourceProvider> providers;

    public ResourceInjectionServicesImpl(final Collection<ResourceProvider> providers) {
        this.providers = providers;
    }

    @Override
    public ResourceReferenceFactory<Object> registerResourceInjectionPoint(
            final InjectionPoint injectionPoint
    ) {
        return () -> new SimpleResourceReference<>(findResource(providers, injectionPoint));
    }

    private Object findResource(final Collection<ResourceProvider> providers, final InjectionPoint injectionPoint) {
        final Set<Object> candidates = providers.stream()
                .map(it -> it.resolve(injectionPoint))
                .collect(toSet());
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Failed to resolve resource " + injectionPoint);
        }
        if (candidates.size() > 1) {
            throw new IllegalStateException("Ambiguous resource injection point " + injectionPoint + ": " + candidates);
        }
        return candidates.iterator().next();
    }

    @Override
    public ResourceReferenceFactory<Object> registerResourceInjectionPoint(
            final String s,
            final String s1
    ) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Object resolveResource(
            final InjectionPoint injectionPoint
    ) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Object resolveResource(
            final String s,
            final String s1
    ) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void cleanup() {
        providers.forEach(it -> it.cleanup());
    }
}
