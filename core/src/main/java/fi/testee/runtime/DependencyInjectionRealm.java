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
package fi.testee.runtime;

import fi.testee.deployment.BeanArchiveDiscovery;
import fi.testee.deployment.BeanDeploymentArchiveManagement;
import fi.testee.deployment.DeploymentImpl;
import fi.testee.exceptions.TestEEfiException;
import fi.testee.services.TransactionServicesImpl;
import fi.testee.spi.DependencyInjection;
import fi.testee.spi.Releasable;
import org.jboss.weld.Container;
import org.jboss.weld.bean.AbstractClassBean;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.context.CreationalContextImpl;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.manager.api.WeldManager;
import org.jboss.weld.transaction.spi.TransactionServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

/**
 * Wrapper around a Weld context.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class DependencyInjectionRealm implements DependencyInjection {
    private static final Logger LOG = LoggerFactory.getLogger(DependencyInjectionRealm.class);
    private final String contextId = UUID.randomUUID().toString();
    private final Bootstrap bootstrap;
    private final DeploymentImpl deployment;

    public DependencyInjectionRealm(
            final ServiceRegistry serviceRegistry,
            final BeanArchiveDiscovery beanArchiveDiscovery,
            final Environments environment
    ) {
        LOG.trace("Starting dependency injection realm {}", contextId);
        ensureTransactionServices(serviceRegistry);
        final BeanDeploymentArchiveManagement bdaManagement = new BeanDeploymentArchiveManagement(
                beanArchiveDiscovery,
                serviceRegistry
        );
        deployment = new DeploymentImpl(bdaManagement, serviceRegistry);
        bootstrap = new WeldBootstrap()
                .startContainer(contextId, environment, deployment)
                .startInitialization()
                .deployBeans()
                .validateBeans()
                .endInitialization();
    }

    private void ensureTransactionServices(ServiceRegistry serviceRegistry) {
        // Odd workaround for the message WELD-000101 that happens when you don't have transactional services
        if (serviceRegistry.get(TransactionServices.class) == null) {
            serviceRegistry.add(TransactionServices.class, new TransactionServicesImpl());
        }
    }

    void shutdown() {
        LOG.trace("Shutting down dependency injection realm {}", contextId);
        bootstrap.shutdown();
    }

    public <T> Collection<Bean<T>> resolve(final Class<T> clazz) {
        return deployment.getBeanDeploymentArchives().stream().map(bootstrap::getManager)
                .map(archive -> beansOf(clazz, archive))
                .flatMap(Collection::stream)
                .collect(toSet());
    }

    @SuppressWarnings("unchecked")
    private <T> Set<Bean<T>> beansOf(
            final Class<T> clazz,
            final WeldManager beanManager
    ) {
        return beanManager.getBeans(clazz).stream().map(bean -> (Bean<T>) bean).collect(toSet());
    }

    private Container container() {
        return Container.instance(contextId);
    }

    @Override
    public <T> Set<T> getInstancesOf(final Class<T> clazz) {
        return resolve(clazz).stream()
                .map(this::newInstance)
                .collect(toSet());
    }

    private <T> T newInstance(final Bean<T> bean) {
        return bean.create(emptyContext());
    }

    private <T> CreationalContextImpl<T> emptyContext() {
        return new CreationalContextImpl<>(null);
    }

    @Override
    public <T> T getInstanceOf(final Class<T> clazz) {
        return unique(clazz, getInstancesOf(clazz));
    }

    private <T> T unique(final Class clazz, final Collection<T> set) {
        if (set.isEmpty()) {
            final Set<Bean<?>> allBeans = container().beanDeploymentArchives().values().stream()
                    .map(BeanManagerImpl::getBeans)
                    .flatMap(Collection::stream)
                    .collect(toSet());
            throw new TestEEfiException("No matching bean found for class "
                    + clazz.getName()
                    + ", available beans are: "
                    + allBeans
            );
        } else if (set.size() > 1) {
            throw new TestEEfiException("Multiple ambiguous beans found for class " + clazz.getName());
        } else {
            return set.iterator().next();
        }
    }

    public ServiceRegistry getServiceRegistry() {
        return container().services();
    }

    public Collection<Bean<?>> getAllBeans() {
        return container().beanDeploymentArchives().values().stream()
                .map(BeanManagerImpl::getBeans)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    public <T> Bean<T> resolveUnique(Class<T> clazz) {
        return unique(clazz, resolve(clazz));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Releasable inject(final Object o) {
        final CreationalContextImpl ctx = withProducer(o, (b, p) -> {
            final CreationalContextImpl context = new CreationalContextImpl<>(b);
            p.inject(o, context);
            return context;
        });
        return () -> ctx.release();
    }

    public void postConstruct(final Object o) {
        withProducer(o, (b, p) -> {
            p.postConstruct(o);
            return null;
        });
    }

    public void preDestroy(final Object o) {
        withProducer(o, (b, p) -> {
            p.preDestroy(o);
            return null;
        });
    }

    private <T> T withProducer(Object o, BiFunction<Bean, InjectionTarget, T> consumer) {
        final Bean<Object> bean = resolveUnique((Class<Object>) o.getClass());
        if (!(bean instanceof AbstractClassBean)) {
            throw new TestEEfiException("Injection of " + bean + " is not supported");
        }
        return consumer.apply(bean, ((AbstractClassBean) bean).getProducer());
    }

}
