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

import fi.testee.spi.BeansXmlModifier;
import fi.testee.spi.DynamicArchiveContributor;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.CDI11Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;

import javax.enterprise.inject.spi.Extension;
import java.util.Collection;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * Implementation of the {@link CDI11Deployment}, which is basically tying together the service
 * registry and the beans for a deployment, where a deployment is the context in which an application
 * runs.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class DeploymentImpl implements CDI11Deployment {
    public static final BeansXmlModifier UNMODIFIED = it -> it;
    private final ServiceRegistry serviceRegistry;
    private final Collection<Metadata<Extension>> extensions;
    private final Map<BeanDeploymentArchive, BeanDeploymentArchive> archives;

    public DeploymentImpl(
            final Collection<DynamicArchiveContributor> dynamicArchiveContributors,
            final ServiceRegistry serviceRegistry,
            final Collection<Metadata<Extension>> extensions,
            final BeansXmlModifier modifier
    ) {
        archives = dynamicArchiveContributors.stream()
                .map(it -> it.contribute(serviceRegistry, this::archiveSupplier))
                .flatMap(Collection::stream)
                .collect(toMap(
                        it -> it,
                        it -> new WrappedBeanDeploymentArchive(it, modifier, this::mapper)
                ));
        this.serviceRegistry = serviceRegistry;
        this.extensions = extensions;
    }

    private Collection<BeanDeploymentArchive> archiveSupplier() {
        return archives.keySet();
    }

    private BeanDeploymentArchive mapper(final BeanDeploymentArchive bda) {
        assert archives.containsKey(bda);
        return archives.get(bda);
    }

    @Override
    public BeanDeploymentArchive getBeanDeploymentArchive(final Class<?> aClass) {
        return loadBeanDeploymentArchive(aClass);
    }

    @Override
    public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
        return archives.values();
    }

    @Override
    public BeanDeploymentArchive loadBeanDeploymentArchive(final Class<?> aClass) {
        for (final BeanDeploymentArchive archive : getBeanDeploymentArchives()) {
            if (archive.getBeanClasses().contains(aClass.getName())) {
                return archive;
            }
        }
        return null;
    }

    @Override
    public ServiceRegistry getServices() {
        return serviceRegistry;
    }

    @Override
    public Iterable<Metadata<Extension>> getExtensions() {
        return extensions;
    }
}
