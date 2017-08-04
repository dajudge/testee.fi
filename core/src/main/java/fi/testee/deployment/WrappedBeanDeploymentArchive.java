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
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.ejb.spi.EjbDescriptor;

import java.util.Collection;
import java.util.function.Function;

import static java.util.stream.Collectors.toSet;

public class WrappedBeanDeploymentArchive implements BeanDeploymentArchive {
    private final BeanDeploymentArchive delegate;
    private final BeansXmlModifier modifier;
    private final Function<BeanDeploymentArchive, BeanDeploymentArchive> mapper;

    public WrappedBeanDeploymentArchive(
            final BeanDeploymentArchive delegate,
            final BeansXmlModifier modifier,
            final Function<BeanDeploymentArchive, BeanDeploymentArchive> mapper
    ) {
        this.delegate = delegate;
        this.modifier = modifier;
        this.mapper = mapper;
    }

    @Override
    public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
        return delegate.getBeanDeploymentArchives().stream()
                .map(mapper)
                .collect(toSet());
    }

    @Override
    public Collection<String> getBeanClasses() {
        return delegate.getBeanClasses();
    }

    @Override
    public BeansXml getBeansXml() {
        return modifier.apply(delegate.getBeansXml());
    }

    @Override
    public Collection<EjbDescriptor<?>> getEjbs() {
        return delegate.getEjbs();
    }

    @Override
    public ServiceRegistry getServices() {
        return delegate.getServices();
    }

    @Override
    public String getId() {
        return delegate.getId();
    }
}
