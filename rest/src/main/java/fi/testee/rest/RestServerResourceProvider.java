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

import fi.testee.spi.AnnotationScanner;
import fi.testee.spi.DependencyInjection;
import fi.testee.spi.ResourceProvider;
import fi.testee.spi.scope.TestInstanceScope;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.enterprise.inject.spi.InjectionPoint;
import java.util.Collection;
import java.util.HashSet;

@TestInstanceScope
public class RestServerResourceProvider implements ResourceProvider {
    @Resource(mappedName = "testeefi/setup/annotationScanner")
    private AnnotationScanner annotationScanner;
    @Resource(mappedName = "testeefi/instance/dependencyInjection")
    private DependencyInjection dependencyInjection;

    private Collection<RestServerImpl> servers = new HashSet<>();

    @Override
    public Object resolve(final InjectionPoint injectionPoint) {
        if (RestServer.class != injectionPoint.getType()) {
            return null;
        }
        final RestServerImpl server = new RestServerImpl(annotationScanner, dependencyInjection);
        servers.add(server);
        return server;
    }

    @Override
    public Object resolve(final String jndiName, final String mappedName) {
        return null;
    }

    @PreDestroy
    public void shutdown() {
        servers.forEach(RestServerImpl::shutdown);
    }
}
