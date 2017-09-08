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

import fi.testee.spi.ResourceProvider;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;
import org.jboss.weld.injection.spi.helpers.SimpleResourceReference;

import javax.enterprise.inject.spi.InjectionPoint;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

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
        return registerInjectionPoint(o -> o.resolve(injectionPoint), injectionPoint.toString());
    }

    private ResourceReferenceFactory<Object> registerInjectionPoint(
            final Function<ResourceProvider, Object> resolver,
            final String description
    ) {
        return () -> new SimpleResourceReference<>(findResource(providers, new Function<ResourceProvider, Object>() {
            @Override
            public Object apply(final ResourceProvider o) {
                return resolver.apply(o);
            }

            @Override
            public String toString() {
                return description;
            }
        }));
    }

    private static Object findResource(
            final Collection<ResourceProvider> providers,
            final Function<ResourceProvider, Object> resolver
    ) {
        final Set<Object> candidates = providers.stream()
                .map(resolver)
                .filter(Objects::nonNull)
                .collect(toSet());
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Failed to resolve resource " + resolver);
        }
        if (candidates.size() > 1) {
            throw new IllegalStateException("Ambiguous resource " + resolver + ": " + candidates);
        }
        return candidates.iterator().next();
    }

    @Override
    public ResourceReferenceFactory<Object> registerResourceInjectionPoint(
            final String jndiName,
            final String mappedName
    ) {
        return registerInjectionPoint(
                o -> o.resolve(jndiName, mappedName),
                "jndiName: " + jndiName + ", mappedName: " + mappedName
        );
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
        // TODO do we have to do something here?
    }
}
