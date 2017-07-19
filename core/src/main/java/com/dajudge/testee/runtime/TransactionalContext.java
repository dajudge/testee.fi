package com.dajudge.testee.runtime;

import com.dajudge.testee.deployment.BeanArchiveDiscovery;
import com.dajudge.testee.ejb.EjbBridge;
import com.dajudge.testee.jpa.PersistenceUnitDiscovery;
import com.dajudge.testee.services.EjbInjectionServicesImpl;
import com.dajudge.testee.services.EjbServicesImpl;
import com.dajudge.testee.services.ExecutorServicesImpl;
import com.dajudge.testee.services.JpaInjectionServicesImpl;
import com.dajudge.testee.services.ProxyServicesImpl;
import com.dajudge.testee.services.ResourceInjectionServicesImpl;
import com.dajudge.testee.services.SecurityServicesImpl;
import com.dajudge.testee.services.TransactionServicesImpl;
import com.dajudge.testee.spi.ResourceProvider;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.injection.spi.EjbInjectionServices;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;
import org.jboss.weld.manager.api.ExecutorServices;
import org.jboss.weld.security.spi.SecurityServices;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.jboss.weld.transaction.spi.TransactionServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

/**
 * A transactional context.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class TransactionalContext {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionalContext.class);

    @Resource(mappedName = "testee/testSetupClass")
    private Class<?> testSetupClass;
    @Resource(mappedName = "testee/beanArchiveDiscovery")
    private BeanArchiveDiscovery beanArchiveDiscovery;
    @Inject
    @Any
    private Instance<ResourceProvider> resourceProviderInstances;

    private DependencyInjectionRealm realm;
    private Collection<ResourceProvider> resourceProviders;

    @PostConstruct
    private void setupRealm() {
        LOG.trace("Creating new transactional context for {}", testSetupClass);
        resourceProviders = new ArrayList<>();
        resourceProviderInstances.forEach(resourceProviders::add);
        final Set<EjbDescriptor<?>> sessionBeans = beanArchiveDiscovery.getSessionBeans();
        final EjbBridge ejbBridge = new EjbBridge(sessionBeans, this::cdiInjection, this::resourceInjection);
        realm = new DependencyInjectionRealm(
                createInstanceServiceRegistry(
                        resourceProviders,
                        beanArchiveDiscovery,
                        ejbBridge::lookupDescriptor,
                        ejbBridge::createInstance
                ),
                beanArchiveDiscovery,
                Environments.EE_INJECT
        );
    }

    private Object resourceInjection(final Resource resource) {
        return realm.getServiceRegistry()
                .get(ResourceInjectionServices.class)
                .registerResourceInjectionPoint(null, resource.mappedName())
                .createResource()
                .getInstance();
    }

    private void cdiInjection(final Object o) {
        realm.inject(o);
    }

    private static ServiceRegistry createInstanceServiceRegistry(
            final Collection<ResourceProvider> resourceProviders,
            final BeanArchiveDiscovery beanArchiveDiscovery,
            final Function<Type, EjbDescriptor<?>> descriptorLookup,
            final Function<EjbDescriptor<?>, ResourceReferenceFactory<Object>> ejbFactory
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
        serviceRegistry.add(EjbInjectionServices.class, new EjbInjectionServicesImpl(descriptorLookup, ejbFactory));
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

    public void run(final TransactionRunnable runnable) {
        runnable.run(testSetupClass, realm);
    }

    public void rollback() {
        shutdown(true);
    }

    public void commit() {
        shutdown(false);
    }

    private void shutdown(final boolean rollback) {
        realm.getServiceRegistry().get(JpaInjectionServicesImpl.class).flush();
        resourceProviders.forEach(it -> it.shutdown(rollback));
        realm.shutdown();
    }

    public interface TransactionRunnable {
        void run(
                Class<?> testSetupClass,
                DependencyInjectionRealm realm
        );
    }
}
