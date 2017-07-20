package com.dajudge.testee.runtime;

import com.dajudge.testee.deployment.BeanArchive;
import com.dajudge.testee.ejb.EjbBridge;
import com.dajudge.testee.jdbc.ConnectionFactoryManager;
import com.dajudge.testee.jdbc.TestDataSource;
import com.dajudge.testee.spi.BeanModifier;
import com.dajudge.testee.spi.BeanModifierFactory;
import com.dajudge.testee.spi.ConnectionFactory;
import com.dajudge.testee.spi.DataSourceMigrator;
import com.dajudge.testee.spi.SessionBeanFactory;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.Bean;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.dajudge.testee.ejb.EjbBridge.IDENTITY_SESSION_BEAN_MODIFIER;
import static com.dajudge.testee.runtime.DatabaseMigration.migrateDataSources;
import static com.dajudge.testee.runtime.TestDataSetup.setupTestData;
import static java.util.stream.Collectors.toSet;

/**
 * Setup for a test. It contains the state shared by all instances of the test setup.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class TestSetup {
    private static final Logger LOG = LoggerFactory.getLogger(TestSetup.class);
    private final DependencyInjectionRealm realm;
    private final Map<Class<? extends ConnectionFactory>, ConnectionFactory> connectionFactories = new HashMap<>();

    public TestSetup(
            final Class<?> setupClass,
            final TestRuntime runtime
    ) {
        final ServiceRegistry serviceRegistry = new SimpleServiceRegistry();
        final Map<String, Object> params = new HashMap<>();
        params.put("testee/testSetupClass", setupClass);
        params.put("testee/beanArchiveDiscovery", runtime.getBeanArchiveDiscorvery());
        params.put("testee/connectionFactoryManager", (ConnectionFactoryManager) this::connectionFactoryManager);
        params.put("testee/ejbDescriptors", getEjbDescriptors(runtime));
        serviceRegistry.add(ResourceInjectionServices.class, new TestSetupResourceInjectionServices(params));
        realm = new DependencyInjectionRealm(serviceRegistry, runtime.getBeanArchiveDiscorvery(), Environments.SE);

        try {
            final TransactionalContext txContext = realm.getInstanceOf(TransactionalContext.class);
            txContext.initialize(IDENTITY_SESSION_BEAN_MODIFIER);
            txContext.run((clazz, testDataSetupRealm) -> {
                final Set<DataSourceMigrator> migrators = testDataSetupRealm.getInstancesOf(DataSourceMigrator.class);
                migrateDataSources(clazz, migrators, testDataSetupRealm.getServiceRegistry());
                setupTestData(clazz, testDataSetupRealm.getServiceRegistry());
            });
            txContext.commit();
        } catch (final RuntimeException e) {
            realm.shutdown();
            throw e;
        }
    }

    private Set<EjbDescriptor<?>> getEjbDescriptors(final TestRuntime runtime) {
        return runtime.getBeanArchiveDiscorvery().getBeanArchives().stream()
                .map(BeanArchive::getEjbs)
                .flatMap(Collection::stream)
                .collect(toSet());
    }

    private synchronized ConnectionFactory connectionFactoryManager(TestDataSource testDataSource) {
        if (!connectionFactories.containsKey(testDataSource.factory())) {
            connectionFactories.put(testDataSource.factory(), realm.getInstanceOf(testDataSource.factory()));
        }
        return connectionFactories.get(testDataSource.factory());
    }

    public Runnable prepareTestInstance(final String name, final Object testInstance) {
        LOG.debug("Instantiating test run '{}' for class {}", name, testInstance.getClass().getName());
        final Set<BeanModifier> beanModifiers = realm.getInstancesOf(BeanModifierFactory.class).stream()
                .map(it -> it.createBeanModifier(testInstance))
                .collect(toSet());
        final TransactionalContext txContext = realm.getInstanceOf(TransactionalContext.class);
        txContext.initialize(new SessionBeanModifierImpl(beanModifiers));
        txContext.run((clazz, testInstanceRealm) -> {
            testInstanceRealm.getAllBeans().forEach(modifyCdiBeans(beanModifiers));
            testInstanceRealm.inject(testInstance);
        });
        return txContext::rollback;
    }

    private Consumer<Bean<?>> modifyCdiBeans(final Collection<BeanModifier> beanModifiers) {
        return bean -> beanModifiers.forEach(it -> it.modifyCdiBean(bean));
    }

    public void shutdown() {
        connectionFactories.values().forEach(ConnectionFactory::release);
        realm.shutdown();
    }

    private static class SessionBeanModifierImpl implements EjbBridge.SessionBeanModifier {
        private final Set<BeanModifier> beanModifiers;

        SessionBeanModifierImpl(final Set<BeanModifier> beanModifiers) {
            this.beanModifiers = beanModifiers;
        }

        @Override
        public <T> SessionBeanFactory<T> modify(SessionBeanFactory<T> factory) {
            for (final BeanModifier beanModifier : beanModifiers) {
                factory = beanModifier.modifySessionBean(factory);
            }
            return factory;
        }
    }
}
