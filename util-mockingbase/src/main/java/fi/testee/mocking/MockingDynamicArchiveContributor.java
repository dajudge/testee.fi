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
package fi.testee.mocking;

import fi.testee.spi.DynamicArchiveContributor;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.ejb.spi.EjbDescriptor;

import javax.inject.Inject;
import java.util.Collection;
import java.util.function.Supplier;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.jboss.weld.bootstrap.spi.BeansXml.EMPTY_BEANS_XML;

public class MockingDynamicArchiveContributor implements DynamicArchiveContributor {
    private static final String ID = "MockingBeanDeploymentArchive";

    @Inject
    private MockStore mockStore;

    @Override
    public Collection<BeanDeploymentArchive> contribute(
            final ServiceRegistry serviceRegistry,
            final Supplier<Collection<BeanDeploymentArchive>> archives
    ) {
        return singletonList(new BeanDeploymentArchive() {

            @Override
            public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
                return archives.get();
            }

            @Override
            public Collection<String> getBeanClasses() {
                return mockStore.getMockClasses().stream().map(Class::getCanonicalName).collect(toSet());
            }

            @Override
            public BeansXml getBeansXml() {
                return EMPTY_BEANS_XML;
            }

            @Override
            public Collection<EjbDescriptor<?>> getEjbs() {
                return emptySet();
            }

            @Override
            public ServiceRegistry getServices() {
                return serviceRegistry;
            }

            @Override
            public String getId() {
                return ID;
            }
        });
    }
}
