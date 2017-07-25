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
package fi.testee.deployment;

import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.CDI11Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;

import javax.enterprise.inject.spi.Extension;
import java.util.Collection;
import java.util.Collections;

/**
 * Implementation of the {@link CDI11Deployment}, which is basically tying together the service
 * registry and the beans for a deployment, where a deployment is the context in which an application
 * runs.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class DeploymentImpl implements CDI11Deployment {
    private final ServiceRegistry serviceRegistry;
    private final BeanDeploymentArchiveManagement bdaManagement;

    /**
     * Constructor.
     *
     * @param bdaManagement   the management of the {@link BeanDeploymentArchive BDAs}.
     * @param serviceRegistry the {@link ServiceRegistry}.
     */
    public DeploymentImpl(
            final BeanDeploymentArchiveManagement bdaManagement,
            final ServiceRegistry serviceRegistry
    ) {
        this.bdaManagement = bdaManagement;
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public BeanDeploymentArchive getBeanDeploymentArchive(Class<?> aClass) {
        return null;
    }

    @Override
    public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
        return bdaManagement.getArchives();
    }

    @Override
    public BeanDeploymentArchive loadBeanDeploymentArchive(final Class<?> aClass) {
        return bdaManagement.findByClass(aClass);
    }

    @Override
    public ServiceRegistry getServices() {
        return serviceRegistry;
    }

    @Override
    public Iterable<Metadata<Extension>> getExtensions() {
        return Collections.emptyList();
    }
}
