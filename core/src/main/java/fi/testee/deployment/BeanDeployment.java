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

import fi.testee.spi.DynamicArchiveContributor;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toSet;

public class BeanDeployment implements DynamicArchiveContributor {
    private final BeanArchiveDiscovery beanArchiveDiscovery;
    private final Predicate<BeanArchive> beanArchiveFilter;

    public BeanDeployment(
            final BeanArchiveDiscovery beanArchiveDiscovery,
            final Predicate<BeanArchive> beanArchiveFilter
    ) {
        this.beanArchiveDiscovery = beanArchiveDiscovery;
        this.beanArchiveFilter = beanArchiveFilter;
    }

    @Override
    public Collection<BeanDeploymentArchive> contribute(
            final ServiceRegistry serviceRegistry,
            final Supplier<Collection<BeanDeploymentArchive>> archives
    ) {
        final Set<BeanArchive> beanArchives = beanArchiveDiscovery.getBeanArchives().stream().filter(beanArchiveFilter).collect(toSet());
        return beanArchives.stream()
                .map(it -> new BeanDeploymentArchiveImpl(serviceRegistry, it, archives))
                .collect(toSet());
    }

    public static <T> Collection<T> and(Collection<T> c, T t) {
        final HashSet<T> ts = new HashSet<>(c);
        ts.add(t);
        return ts;
    }
}
