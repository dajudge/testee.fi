package com.dajudge.testee.runtime;

import com.dajudge.testee.deployment.BeanArchive;
import com.dajudge.testee.jdbc.ConnectionFactoryManager;
import com.dajudge.testee.jdbc.TestDataSource;
import com.dajudge.testee.spi.BeanModifier;
import com.dajudge.testee.spi.BeanModifierFactory;
import com.dajudge.testee.spi.ConnectionFactory;
import com.dajudge.testee.spi.DataSourceMigrator;
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
            txContext.run((clazz, realm) -> {
                final Set<DataSourceMigrator> migrators = realm.getInstancesOf(DataSourceMigrator.class);
                migrateDataSources(clazz, migrators, realm.getServiceRegistry());
                setupTestData(clazz, realm.getServiceRegistry());
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

    public Runnable prepareTestInstance(
            final String name,
            final Object testClassInstance
    ) {
        LOG.debug("Instantiating test run '{}' for class {}", name, testClassInstance.getClass().getName());
        final TransactionalContext txContext = realm.getInstanceOf(TransactionalContext.class);
        txContext.run((clazz, realm) -> {
            final Set<BeanModifier> instancesOf = realm.getInstancesOf(BeanModifierFactory.class).stream()
                    .map(it -> it.createBeanModifier(testClassInstance))
                    .collect(toSet());
            realm.getAllBeans().forEach(initializeBeans(instancesOf));
            realm.inject(testClassInstance);
        });
        return () -> txContext.rollback();
    }

    private Consumer<Bean<?>> initializeBeans(final Collection<BeanModifier> beanModifiers) {
        return bean -> beanModifiers.forEach(it -> it.initializeForBean(bean));
    }

    public void shutdown() {
        connectionFactories.values().forEach(ConnectionFactory::release);
        realm.shutdown();
    }
}
