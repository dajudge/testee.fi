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
package fi.testee.rest;

import fi.testee.spi.DependencyInjection;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.ServiceBindingBuilder;
import org.glassfish.jersey.internal.inject.Injections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static fi.testee.rest.RestServerImpl.ATTR_CLASSES;
import static fi.testee.rest.RestServerImpl.ATTR_DI;

public class ComponentProvider implements org.glassfish.jersey.server.spi.ComponentProvider {
    private static final Logger LOG = LoggerFactory.getLogger(ComponentProvider.class);

    private ServiceLocator locator;

    @Override
    public void initialize(final ServiceLocator locator) {
        this.locator = locator;
    }

    private DependencyInjection dependencyInjection() {
        return (DependencyInjection) servletContext().getAttribute(ATTR_DI);
    }

    @SuppressWarnings("unchecked")
    private Set<Class<?>> managedClasses() {
        return (Set<Class<?>>) servletContext().getAttribute(ATTR_CLASSES);
    }

    private ServletContext servletContext() {
        return locator.getService(ServletContext.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean bind(final Class<?> component, final Set<Class<?>> providerContracts) {
        if (!managedClasses().contains(component)) {
            return false;
        }
        LOG.info("Binding {} {} to dependency injection", component, providerContracts);
        final DynamicConfiguration dynamicConfig = Injections.getConfiguration(locator);
        final DependencyInjectionFactory<Object> factory = new DependencyInjectionFactory<>(
                (Class<Object>) component,
                dependencyInjection()
        );
        final ServiceBindingBuilder<Object> binding = Injections.newFactoryBinder(factory)
                .to(component);
        (providerContracts == null ? Collections.<Class<?>>emptySet() : providerContracts).forEach(binding::to);
        Injections.addBinding(binding, dynamicConfig);
        dynamicConfig.commit();
        return true;
    }

    @Override
    public void done() {

    }

}
