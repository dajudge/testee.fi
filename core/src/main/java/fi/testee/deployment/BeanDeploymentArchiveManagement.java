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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Management of bean deployment archives. Thread safe.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class BeanDeploymentArchiveManagement {
    private static final Logger LOG = LoggerFactory.getLogger(BeanDeploymentArchiveManagement.class);

    private final BeanArchiveDiscovery beanArchiveDiscovery;
    private final ServiceRegistry serviceRegistry;
    private Collection<BeanDeploymentArchive> archives = null;

    public BeanDeploymentArchiveManagement(
            final BeanArchiveDiscovery beanArchiveDiscovery,
            final ServiceRegistry serviceRegistry
    ) {
        this.beanArchiveDiscovery = beanArchiveDiscovery;
        this.serviceRegistry = serviceRegistry;
    }

    public synchronized Collection<BeanDeploymentArchive> getArchives() {
        if (archives == null) {
            archives = beanArchiveDiscovery.getBeanArchives().stream()
                    .peek(it -> LOG.debug("Found bean archive: {}", it))
                    .map(it -> new BeanDeploymentArchiveImpl(serviceRegistry, it, () -> archives))
                    .collect(Collectors.toSet());
        }
        return archives;
    }

    public BeanDeploymentArchive findByClass(final Class<?> aClass) {
        for (final BeanDeploymentArchive archive : archives) {
            if (archive.getBeanClasses().contains(aClass.getName())) {
                return archive;
            }
        }
        return null;
    }
}
