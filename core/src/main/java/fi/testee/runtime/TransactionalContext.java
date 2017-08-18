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

import fi.testee.deployment.BeanArchive;
import fi.testee.deployment.BeanArchiveDiscovery;
import fi.testee.deployment.EjbDescriptorImpl;
import fi.testee.deployment.InterceptorChain;
import fi.testee.ejb.EjbContainer;
import fi.testee.ejb.EjbDescriptorHolder;
import fi.testee.exceptions.TestEEfiException;
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
import fi.testee.spi.DynamicArchiveContributor;
import fi.testee.spi.PersistenceUnitPropertyContributor;
import fi.testee.spi.Releaser;
import fi.testee.spi.ResourceProvider;
import fi.testee.spi.SessionBeanAlternatives;
import fi.testee.utils.InjectionPointUtils;
import org.jboss.weld.bean.SessionBean;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.injection.spi.EjbInjectionServices;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.manager.api.ExecutorServices;
import org.jboss.weld.security.spi.SecurityServices;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.jboss.weld.transaction.spi.TransactionServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Extension;
import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

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
    private Instance<ResourceProvider> resourceProvidersInstance;
    @Inject
    @Any
    private Instance<PersistenceUnitPropertyContributor> propertyContributorInstance;

    private DependencyInjectionRealm realm;
    private EjbContainer ejbContainer;

    public void initialize(
            final Collection<Metadata<Extension>> extensions,
            final BeansXmlModifier beansXmlModifier,
            final Collection<ResourceProvider> setupResolvers,
            final Predicate<BeanArchive> archiveFilter,
            final SessionBeanAlternatives sessionBeanAlternatives,
            final Collection<DynamicArchiveContributor> archiveContributors,
            final Annotation... scopes
    ) {
        LOG.debug("Initializing new transactional context for {}", testSetupClass);
        Map<EjbDescriptor<?>, EjbDescriptorImpl<?>> ejbDescriptors = beanArchiveDiscovery.getBeanArchives().stream()
                .filter(archiveFilter)
                .map(BeanArchive::getEjbs)
                .flatMap(Collection::stream)
                .collect(toMap(
                        it -> it,
                        it -> it
                ));
        ejbContainer = new EjbContainer(ejbDescriptors.keySet());
        final Set<ResourceProvider> resourceProviders = new HashSet<>(setupResolvers);
        stream(scopes).forEach(it -> resourceProvidersInstance.select(it).forEach(resourceProviders::add));
        LOG.trace("Resource providers: {}", resourceProviders);
        final ServiceRegistry instanceServiceRegistry = createInstanceServiceRegistry(
                resourceProviders,
                beanArchiveDiscovery,
                sessionBeanAlternatives,
                ejbContainer::lookupDescriptor,
                ejbContainer::createInstance,
                propertyContributor());
        realm = new DependencyInjectionRealm().init(
                instanceServiceRegistry,
                beanArchiveDiscovery,
                Environments.EE_INJECT,
                extensions,
                beansXmlModifier,
                archiveFilter,
                archiveContributors
        );
        ejbContainer.init(
                new EjbContainer.EjbDescriptorHolderResolver() {
                    @Override
                    public <T> EjbDescriptorHolder<T> resolve(final EjbDescriptor<T> descriptor) {
                        return holderResolver(descriptor, ejbDescriptors);
                    }
                },
                this::cdiInjection,
                this::resourceInjection,
                this::jpaInjection,
                this::ejbInjection
        );
    }

    private <T> EjbDescriptorHolder<T> holderResolver(
            final EjbDescriptor<T> desc,
            final Map<EjbDescriptor<?>, EjbDescriptorImpl<?>> impls
    ) {
        final BeanManagerImpl archive = realm.findArchiveFor(desc.getBeanClass());
        final SessionBean<T> sessionBean = archive.getBean(desc);
        if (sessionBean == null) {
            throw new TestEEfiException("Failed to find session bean for " + desc);
        }
        final EjbDescriptorImpl<?> descImpl = impls.get(desc);
        final InterceptorChain chain = descImpl.getInterceptorChain(realm::contextFor);
        return new EjbDescriptorHolder<T>(desc, chain, sessionBean, archive);
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

    private Object jpaInjection(
            final Field field,
            final Bean<?> bean,
            final BeanManagerImpl beanManager
    ) {
        return realm.getServiceRegistry()
                .get(JpaInjectionServices.class)
                .registerPersistenceContextInjectionPoint(InjectionPointUtils.injectionPointOf(field, bean, beanManager))
                .createResource()
                .getInstance();
    }

    private Object ejbInjection(
            final Field field,
            final Bean<?> bean,
            final BeanManagerImpl beanManager
    ) {
        return realm.getServiceRegistry()
                .get(EjbInjectionServices.class)
                .registerEjbInjectionPoint(InjectionPointUtils.injectionPointOf(field, bean, beanManager))
                .createResource()
                .getInstance();
    }

    private Object resourceInjection(
            final Field field,
            final Bean<?> bean,
            final BeanManagerImpl beanManager
    ) {
        return realm.getServiceRegistry()
                .get(ResourceInjectionServices.class)
                .registerResourceInjectionPoint(InjectionPointUtils.injectionPointOf(field, bean, beanManager))
                .createResource()
                .getInstance();
    }

    private static ServiceRegistry createInstanceServiceRegistry(
            final Collection<ResourceProvider> resourceProviders,
            final BeanArchiveDiscovery beanArchiveDiscovery,
            final SessionBeanAlternatives alternatives,
            final EjbInjectionServicesImpl.EjbLookup ejbLookup,
            final EjbInjectionServicesImpl.EjbFactory ejbFactory,
            final PersistenceUnitPropertyContributor propertyContributor
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
                serviceRegistry.get(ResourceInjectionServices.class),
                propertyContributor
        );
        serviceRegistry.add(JpaInjectionServices.class, jpaInjectionService);
        serviceRegistry.add(JpaInjectionServicesImpl.class, jpaInjectionService);
        serviceRegistry.add(EjbInjectionServices.class, new EjbInjectionServicesImpl(
                ejbLookup,
                ejbFactory,
                alternatives
        ));
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
            final ResourceInjectionServices resourceInjectionServices,
            final PersistenceUnitPropertyContributor propertyContributor
    ) {
        final PersistenceUnitDiscovery persistenceUnitDiscovery = new PersistenceUnitDiscovery(
                beanArchiveDiscovery,
                resourceInjectionServices,
                propertyContributor
        );
        return new JpaInjectionServicesImpl(persistenceUnitDiscovery);
    }

    private PersistenceUnitPropertyContributor propertyContributor() {
        final Collection<PersistenceUnitPropertyContributor> contributors = new HashSet<>();
        propertyContributorInstance.forEach(contributors::add);
        return (properties, provider) -> contributors.forEach(it -> it.contribute(properties, provider));
    }

    public <T> T run(final TransactionRunnable<T> runnable) {
        return runnable.run(testSetupClass, realm);
    }

    @PreDestroy
    public void shutdown() {
        LOG.debug("Shutting down transactional context for {}", testSetupClass);
        if (ejbContainer != null) {
            ejbContainer.shutdown();
        }
        if (realm != null) {
            realm.shutdown();
        }
    }

    public void flushEntityManagers() {
        realm.getServiceRegistry().get(JpaInjectionServicesImpl.class).flush();
    }

    public DependencyInjection getDependencyInjection() {
        return realm;
    }

    public interface TransactionRunnable<T> {
        T run(
                Class<?> testSetupClass,
                DependencyInjectionRealm realm
        );
    }
}
