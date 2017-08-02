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
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;
import org.jboss.weld.Container;
import org.jboss.weld.bean.AbstractClassBean;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.context.CreationalContextImpl;
import org.jboss.weld.context.api.ContextualInstance;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.transaction.spi.TransactionServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
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
    private final CreationalContextImpl<Object> rootContext = new CreationalContextImpl<>(null);

    DependencyInjectionRealm(
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

    private void ensureTransactionServices(final ServiceRegistry serviceRegistry) {
        // Odd workaround for the message WELD-000101 that happens when you don't have transactional services
        if (serviceRegistry.get(TransactionServices.class) == null) {
            serviceRegistry.add(TransactionServices.class, new TransactionServicesImpl());
        }
    }

    void shutdown() {
        LOG.trace("Shutting down dependency injection realm {}", contextId);
        contexts.forEach(CreationalContext::release);
        rootContext.release();
        bootstrap.shutdown();
    }

    private <T> Collection<Bean<T>> resolve(final Class<T> clazz) {
        return deployment.getBeanDeploymentArchives().stream().map(bootstrap::getManager)
                .map(archive -> beansOf(clazz, (BeanManagerImpl) archive))
                .flatMap(Collection::stream)
                .collect(toSet());
    }

    @SuppressWarnings("unchecked")
    private <T> Set<Bean<T>> beansOf(
            final Class<T> clazz,
            final BeanManagerImpl beanManager
    ) {
        return beanManager.getBeans(clazz).stream()
                .map(bean -> (Bean<T>) bean)
                .collect(toSet());
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
        final CreationalContextImpl<T> ctx = contextFor(bean);
        final T instance = bean.create(ctx);
        ctx.addDependentInstance(new ContextualInstance<T>() {
            @Override
            public T getInstance() {
                return instance;
            }

            @Override
            public CreationalContext<T> getCreationalContext() {
                return ctx;
            }

            @Override
            public Contextual<T> getContextual() {
                return bean;
            }
        });
        return instance;
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

    ServiceRegistry getServiceRegistry() {
        return container().services();
    }

    Collection<Bean<?>> getAllBeans() {
        return container().beanDeploymentArchives().values().stream()
                .map(BeanManagerImpl::getBeans)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private <T> Bean<T> resolveUnique(Class<T> clazz) {
        return unique(clazz, resolve(clazz));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void inject(final Object o) {
        withProducer(o, (b, p) -> {
            p.inject(o, contextFor(b));
        });
    }

    private static CreationalContextImpl wrap(CreationalContextImpl creationalContext) {
        try {
            final ProxyFactory factory = new ProxyFactory();
            factory.setSuperclass(CreationalContextImpl.class);
            factory.setFilter(m -> m.getDeclaringClass() != Object.class);
            final CreationalContextImpl proxy = (CreationalContextImpl) factory.create(new Class[]{Contextual.class}, new Object[]{null});
            ((Proxy) proxy).setHandler((self, thisMethod, proceed, args) -> {
                try {
                    LOG.info("{} {}", thisMethod, args);
                    if (thisMethod.getName().equals("release")) {
                        LOG.info("RELEASE");
                    }
                    Object ret = thisMethod.invoke(creationalContext, args);
                    if (ret instanceof CreationalContextImpl) {
                        return wrap((CreationalContextImpl) ret);
                    }
                    return ret;
                } catch (final InvocationTargetException e) {
                    throw e.getTargetException();
                }
            });
            return proxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    void postConstruct(final Object o) {
        withProducer(o, (b, p) -> p.postConstruct(o));
    }

    @SuppressWarnings("unchecked")
    void preDestroy(final Object o) {
        withProducer(o, (b, p) -> p.preDestroy(o));
    }

    @SuppressWarnings("unchecked")
    private void withProducer(final Object o, final BiConsumer<Bean, InjectionTarget> consumer) {
        final Bean<Object> bean = resolveUnique((Class<Object>) o.getClass());
        if (!(bean instanceof AbstractClassBean)) {
            throw new TestEEfiException("Injection of " + bean + " is not supported");
        }
        consumer.accept(bean, ((AbstractClassBean) bean).getProducer());
    }

    <T> CreationalContextImpl<T> contextFor(final Contextual<T> ctx) {
        return remember(rootContext.getCreationalContext(ctx));
    }

    private final Collection<CreationalContext<?>> contexts = new ArrayList<>();

    private <T> CreationalContextImpl<T> remember(final CreationalContextImpl<T> creationalContext) {
        // TODO this seems a bit hacky - do we really have to remember all creational contexts ourselves?
        contexts.add(creationalContext);
        return creationalContext;
    }
}
