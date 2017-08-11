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
import fi.testee.spi.DynamicArchiveContributor;
import fi.testee.spi.BeansXmlModifier;
import fi.testee.spi.DependencyInjection;
import fi.testee.spi.ReleaseCallbackHandler;
import fi.testee.spi.Releaser;
import fi.testee.spi.ResourceProvider;
import fi.testee.spi.SessionBeanAlternatives;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedField;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.bean.SessionBean;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.context.CreationalContextImpl;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.injection.FieldInjectionPoint;
import org.jboss.weld.injection.InjectionPointFactory;
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
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Extension;
import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;

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

    private DependencyInjectionRealm realm;
    private EjbContainer ejbContainer;

    public TransactionalContext() {
        LOG.info("New transactional context");
    }

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
        ejbContainer = new EjbContainer(beanArchiveDiscovery.getBeanArchives().stream()
                .filter(archiveFilter)
                .map(BeanArchive::getEjbs)
                .flatMap(Collection::stream)
                .collect(toSet()));
        final Set<ResourceProvider> resourceProviders = new HashSet<>(setupResolvers);
        Arrays.stream(scopes).forEach(it -> resourceProvidersInstance.select(it).forEach(resourceProviders::add));
        LOG.trace("Resource providers: {}", resourceProviders);
        ServiceRegistry instanceServiceRegistry = createInstanceServiceRegistry(
                resourceProviders,
                beanArchiveDiscovery,
                sessionBeanAlternatives,
                ejbContainer::lookupDescriptor,
                ejbContainer::createInstance
        );
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
                this::holderResolver,
                this::cdiInjection,
                this::resourceInjection,
                this::jpaInjection,
                this::ejbInjection,
                this::contextFor
        );
    }

    private <T> EjbDescriptorHolder<T> holderResolver(final EjbDescriptorImpl<T> desc) {
        final BeanManagerImpl archive = realm.findArchiveFor(desc.getBeanClass());
        final SessionBean<T> sessionBean = archive.getBean(desc);
        if (sessionBean == null) {
            throw new TestEEfiException("Failed to find session bean for " + desc);
        }
        return new EjbDescriptorHolder(desc, sessionBean, archive);
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

    private Object jpaInjection(
            final Field field,
            final Bean<?> bean,
            final BeanManagerImpl beanManager
    ) {
        return realm.getServiceRegistry()
                .get(JpaInjectionServices.class)
                .registerPersistenceContextInjectionPoint(injectionPointOf(field, bean, beanManager))
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
                .registerEjbInjectionPoint(injectionPointOf(field, bean, beanManager))
                .createResource()
                .getInstance();
    }

    @SuppressWarnings("unchecked")
    private <T> FieldInjectionPoint<Object, T> injectionPointOf(
            final Field field,
            final Bean<T> bean,
            final BeanManagerImpl beanManager
    ) {
        final EnhancedAnnotatedType<T> type = beanManager.createEnhancedAnnotatedType((Class<T>) bean.getBeanClass());
        final Collection<EnhancedAnnotatedField<?, ? super T>> enhancedFields = type.getEnhancedFields();
        final EnhancedAnnotatedField<Object, T> eaf = (EnhancedAnnotatedField<Object, T>) enhancedFields.stream()
                .filter(it -> field.equals(it.getJavaMember()))
                .findFirst()
                .orElseThrow(() -> new TestEEfiException("Failed to get enhanced annotated field for " + field));
        return InjectionPointFactory.instance()
                .createFieldInjectionPoint(eaf, bean, bean.getBeanClass(), beanManager);
    }

    private <T, X> Object resourceInjection(
            final Field field,
            final Bean<?> bean,
            final BeanManagerImpl beanManager
    ) {
        return realm.getServiceRegistry()
                .get(ResourceInjectionServices.class)
                .registerResourceInjectionPoint(injectionPointOf(field, bean, beanManager))
                .createResource()
                .getInstance();
    }

    private static ServiceRegistry createInstanceServiceRegistry(
            final Collection<ResourceProvider> resourceProviders,
            final BeanArchiveDiscovery beanArchiveDiscovery,
            final SessionBeanAlternatives alternatives,
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
