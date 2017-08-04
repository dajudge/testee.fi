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
import fi.testee.ejb.EjbContainer;
import fi.testee.jpa.PersistenceUnitDiscovery;
import fi.testee.services.EjbInjectionServicesImpl;
import fi.testee.services.EjbServicesImpl;
import fi.testee.services.ExecutorServicesImpl;
import fi.testee.services.JpaInjectionServicesImpl;
import fi.testee.services.ProxyServicesImpl;
import fi.testee.services.ResourceInjectionServicesImpl;
import fi.testee.services.SecurityServicesImpl;
import fi.testee.services.TransactionServicesImpl;
import fi.testee.spi.BeansXmlModifier;
import fi.testee.spi.DependencyInjection;
import fi.testee.spi.ReleaseCallbackHandler;
import fi.testee.spi.Releaser;
import fi.testee.spi.ResourceProvider;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.context.CreationalContextImpl;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.injection.spi.EjbInjectionServices;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.manager.api.ExecutorServices;
import org.jboss.weld.security.spi.SecurityServices;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.jboss.weld.transaction.spi.TransactionServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Extension;
import javax.inject.Inject;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * A transactional context.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class TransactionalContext {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionalContext.class);

    @Resource(mappedName = "testeefi/setup/class")
    private Class<?> testSetupClass;
    @Resource(mappedName = "testeefi/setup/beanArchiveDiscovery")
    private BeanArchiveDiscovery beanArchiveDiscovery;
    @Inject
    @Any
    private Instance<ResourceProvider> resourceProviderInstances;

    private DependencyInjectionRealm realm;
    private Collection<ResourceProvider> resourceProviders;
    private EjbContainer ejbContainer;

    public void initialize(
            final EjbContainer.SessionBeanModifier sessionBeanModifier,
            final Map<String, Object> additionalResources,
            final Collection<Metadata<Extension>> extensions,
            final BeansXmlModifier beansXmlModifier
    ) {
        LOG.trace("Creating new transactional context for {}", testSetupClass);
        resourceProviders = new ArrayList<>();
        resourceProviderInstances.forEach(resourceProviders::add);
        resourceProviders.add(createLocationResourceProvider(additionalResources));
        ejbContainer = new EjbContainer(
                beanArchiveDiscovery.getSessionBeans(),
                this::cdiInjection,
                this::resourceInjection,
                this::jpaInjection,
                sessionBeanModifier,
                this::contextFor
        );
        realm = new DependencyInjectionRealm(
                createInstanceServiceRegistry(
                        resourceProviders,
                        beanArchiveDiscovery,
                        ejbContainer::lookupDescriptor,
                        ejbContainer::createInstance
                ),
                beanArchiveDiscovery,
                Environments.EE_INJECT,
                extensions,
                beansXmlModifier
        );
    }

    private Collection<ResourceReference<?>> cdiInjection(final Object o) {
        final Releaser r = new Releaser();
        realm.inject(o, r);
        return asList(new ResourceReference<Object>() {
            @Override
            public Object getInstance() {
                return o;
            }

            @Override
            public void release() {
                r.release();
            }
        });
    }

    private <T> CreationalContextImpl<T> contextFor(final Contextual<T> ctx, final ReleaseCallbackHandler releaser) {
        return realm.contextFor(ctx, releaser);
    }

    private SimpleResourceProvider createLocationResourceProvider(final Map<String, Object> additionalResources) {
        final Map<String, Object> localResources = new HashMap<>();
        localResources.putAll(additionalResources);
        localResources.put("testeefi/instance/dependencyInjection", dependencyInjection());
        return new SimpleResourceProvider(localResources);
    }

    public DependencyInjection dependencyInjection() {
        return new DeferredDependencyInjection(() -> realm);
    }

    private Object jpaInjection(final PersistenceContext persistenceContext) {
        return realm.getServiceRegistry()
                .get(JpaInjectionServicesImpl.class)
                .resolvePersistenceContext(persistenceContext);
    }

    private Object resourceInjection(final Resource resource) {
        return realm.getServiceRegistry()
                .get(ResourceInjectionServices.class)
                .registerResourceInjectionPoint(null, resource.mappedName())
                .createResource()
                .getInstance();
    }

    private static ServiceRegistry createInstanceServiceRegistry(
            final Collection<ResourceProvider> resourceProviders,
            final BeanArchiveDiscovery beanArchiveDiscovery,
            final EjbInjectionServicesImpl.EjbLookup ejbLookup,
            final EjbInjectionServicesImpl.EjbFactory ejbFactory
    ) {
        final ServiceRegistry serviceRegistry = new SimpleServiceRegistry();
        // Resource injection
        serviceRegistry.add(
                ResourceInjectionServices.class,
                new ResourceInjectionServicesImpl(resourceProviders)
        );
        // JPA injection
        JpaInjectionServicesImpl jpaInjectionService = createJpaInjectionService(
                beanArchiveDiscovery,
                serviceRegistry.get(ResourceInjectionServices.class)
        );
        serviceRegistry.add(JpaInjectionServices.class, jpaInjectionService);
        serviceRegistry.add(JpaInjectionServicesImpl.class, jpaInjectionService);
        serviceRegistry.add(EjbInjectionServices.class, new EjbInjectionServicesImpl(ejbLookup, ejbFactory));
        // Only stubs from here on
        serviceRegistry.add(TransactionServices.class, new TransactionServicesImpl());
        serviceRegistry.add(SecurityServices.class, new SecurityServicesImpl());
        serviceRegistry.add(ProxyServices.class, new ProxyServicesImpl());
        serviceRegistry.add(ExecutorServices.class, new ExecutorServicesImpl());
        serviceRegistry.add(EjbServices.class, new EjbServicesImpl(ejbFactory));
        return serviceRegistry;
    }

    private static JpaInjectionServicesImpl createJpaInjectionService(
            final BeanArchiveDiscovery beanArchiveDiscovery,
            final ResourceInjectionServices resourceInjectionServices
    ) {
        final PersistenceUnitDiscovery persistenceUnitDiscovery = new PersistenceUnitDiscovery(
                beanArchiveDiscovery,
                resourceInjectionServices
        );
        return new JpaInjectionServicesImpl(persistenceUnitDiscovery);
    }

    public <T> T run(final TransactionRunnable<T> runnable) {
        return runnable.run(testSetupClass, realm);
    }

    public void rollback() {
        shutdown(true);
    }

    public void commit() {
        shutdown(false);
    }

    private void shutdown(final boolean rollback) {
        if (realm != null) {
            realm.getServiceRegistry().get(JpaInjectionServicesImpl.class).flush();
        }
        if (ejbContainer != null) {
            ejbContainer.shutdown();
        }
        resourceProviders.forEach(it -> it.shutdown(rollback));
        if (realm != null) {
            realm.shutdown();
        }
    }

    public interface TransactionRunnable<T> {
        T run(
                Class<?> testSetupClass,
                DependencyInjectionRealm realm
        );
    }

}
