package com.dajudge.testee.runtime;

import com.dajudge.testee.spi.DataSourceMigrator;
import com.dajudge.testee.spi.PluginTestInstance;
import com.dajudge.testee.spi.PluginTestSetup;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.Bean;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.dajudge.testee.runtime.DatabaseMigration.migrateDataSources;
import static com.dajudge.testee.runtime.TestDataSetup.setupTestData;

/**
 * Setup for a test. It contains the state shared by all instances of the test setup.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class TestSetup {
    private static final Logger LOG = LoggerFactory.getLogger(TestSetup.class);
    private final Set<PluginTestSetup> plugins;
    private final DependencyInjectionRealm realm;

    public TestSetup(
            final Class<?> setupClass,
            final TestRuntime runtime
    ) {
        final ServiceRegistry serviceRegistry = new SimpleServiceRegistry();
        final Map<String, Object> params = new HashMap<>();
        params.put("testee/testSetupClass", setupClass);
        params.put("testee/beanArchiveDiscovery", runtime.getBeanArchiveDiscorvery());
        serviceRegistry.add(ResourceInjectionServices.class, new TestSetupResourceInjectionServices(params));
        realm = new DependencyInjectionRealm(serviceRegistry, runtime.getBeanArchiveDiscorvery());

        try {
            plugins = runtime.getPlugins().stream()
                    .map(it -> it.setup(setupClass))
                    .filter(Objects::nonNull) // Plugins return null when not interested in this class
                    .collect(Collectors.toSet());
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

    public Runnable prepareTestInstance(
            final String name,
            final Object testClassInstance
    ) {
        LOG.debug("Instantiating test run '{}' for class {}", name, testClassInstance.getClass().getName());
        final TransactionalContext txContext = realm.getInstanceOf(TransactionalContext.class);
        txContext.run((clazz, realm) -> {
            final Collection<PluginTestInstance> pluginTestInstances = plugins.stream()
                    .map(it -> it.instantiate(testClassInstance))
                    .filter(it -> it != null)
                    .collect(Collectors.toSet());
            realm.getAllBeans().forEach(initializeBeans(pluginTestInstances));
            realm.inject(testClassInstance);
        });
        return () -> txContext.rollback();
    }

    private Consumer<Bean<?>> initializeBeans(final Collection<PluginTestInstance> pluginTestInstances) {
        return bean -> pluginTestInstances.forEach(it -> it.initializeForBean(bean));
    }

    public void shutdown() {
        plugins.forEach(it -> it.shutdown());
    }
}
