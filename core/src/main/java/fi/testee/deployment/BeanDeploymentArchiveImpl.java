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

import fi.testee.classpath.JavaArchive;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.ejb.spi.EjbDescriptor;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

/**
 * Implementation of a {@link BeanDeploymentArchive} (or short <i>BDA</i>), which basically links a {@link JavaArchive}
 * to a {@link DeploymentImpl deployment}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class BeanDeploymentArchiveImpl implements BeanDeploymentArchive {
    private final ServiceRegistry serviceRegistry;
    private final BeanArchive beanArchive;
    private final Supplier<Collection<BeanDeploymentArchive>> archivesSupplier;

    public BeanDeploymentArchiveImpl(
            final ServiceRegistry serviceRegistry,
            final BeanArchive beanArchive,
            final Supplier<Collection<BeanDeploymentArchive>> archivesSupplier
            ) {
        this.serviceRegistry = serviceRegistry;
        this.beanArchive = beanArchive;
        this.archivesSupplier = archivesSupplier;
    }

    @Override
    public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
        return archivesSupplier.get();
    }

    @Override
    public Collection<String> getBeanClasses() {
        return beanArchive.getBeanClasses();
    }

    @Override
    public BeansXml getBeansXml() {
        // FIXME: actually read the beans.xml
        return BeansXml.EMPTY_BEANS_XML;
    }

    @Override
    public Collection<EjbDescriptor<?>> getEjbs() {
        return beanArchive.getEjbs();
    }

    @Override
    public ServiceRegistry getServices() {
        return serviceRegistry;
    }

    @Override
    public String getId() {
        return beanArchive.getClasspathEntry().getURL().toExternalForm();
    }
}
