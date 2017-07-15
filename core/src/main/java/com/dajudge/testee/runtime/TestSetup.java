package com.dajudge.testee.runtime;

import com.dajudge.testee.deployment.BeanDeploymentArchiveManagement;
import com.dajudge.testee.jpa.PersistenceUnitDiscovery;
import com.dajudge.testee.services.EjbInjectionServicesImpl;
import com.dajudge.testee.services.ExecutorServicesImpl;
import com.dajudge.testee.services.JpaInjectionServicesImpl;
import com.dajudge.testee.services.ProxyServicesImpl;
import com.dajudge.testee.services.ResourceInjectionServicesImpl;
import com.dajudge.testee.services.SecurityServicesImpl;
import com.dajudge.testee.services.TransactionServicesImpl;
import com.dajudge.testee.spi.PluginTestInstance;
import com.dajudge.testee.spi.PluginTestSetup;
import com.dajudge.testee.spi.ResourceProvider;
import com.dajudge.testee.spi.ResourceProviderFactory;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.injection.spi.EjbInjectionServices;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.manager.api.ExecutorServices;
import org.jboss.weld.security.spi.SecurityServices;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.jboss.weld.transaction.spi.TransactionServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dajudge.testee.utils.ProxyUtils.trace;
import static java.util.stream.Collectors.toSet;

/**
 * Setup for a test. It contains the state shared by all instances of the test setup.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class TestSetup {
    private static final Logger LOG = LoggerFactory.getLogger(TestSetup.class);
    private final Set<PluginTestSetup> plugins;
    private final TestRuntime runtime;
    private final ServiceRegistry globalServiceRegistry;
    private final Map<Class<? extends Service>, Service> guardedGlobalServices = new HashMap<>();

    private Set<ResourceProvider> resourceProviders;

    public TestSetup(
            final Class<?> setupClass,
            final TestRuntime runtime
    ) {
        this.runtime = runtime;
        try {
            plugins = runtime.getPlugins().stream()
                    .map(it -> it.setup(setupClass))
                    .filter(it -> it != null) // Plugins return null when not interested in this class
                    .collect(Collectors.toSet());
            globalServiceRegistry = createSetupServiceRegistry(setupClass);
            DatabaseMigration.migrateDataSources(setupClass, runtime, globalServiceRegistry);
            TestDataSetup.setupTestData(setupClass, globalServiceRegistry);
        } catch (final Exception e) {
            if (resourceProviders != null) {
                resourceProviders.forEach(ResourceProvider::shutdown);
            }
            throw e;
        }
    }

    private ServiceRegistry createSetupServiceRegistry(final Class<?> setupClass) {
        final SimpleServiceRegistry serviceRegistry = new SimpleServiceRegistry();
        // Resource injection
        final Set<ResourceProviderFactory> resourceProviderFactories =
                runtime.getInstancesOf(ResourceProviderFactory.class);
        resourceProviders = resourceProviderFactories.stream()
                .map(it -> it.create(setupClass))
                .collect(toSet());
        final ResourceInjectionServicesImpl resourceInjectionServices =
                new ResourceInjectionServicesImpl(resourceProviders);
        serviceRegistry.add(ResourceInjectionServices.class, resourceInjectionServices);
        guardedGlobalServices.put(
                ResourceInjectionServices.class,
                guard(resourceInjectionServices, ResourceInjectionServices.class)
        );

        // JPA injection
        final PersistenceUnitDiscovery persistenceUnitDiscovery = new PersistenceUnitDiscovery(
                runtime.getBeanArchiveDiscorvery(),
                serviceRegistry.get(ResourceInjectionServices.class)
        );
        final JpaInjectionServicesImpl jpaInjectionService = new JpaInjectionServicesImpl(persistenceUnitDiscovery);
        serviceRegistry.add(JpaInjectionServices.class, jpaInjectionService);
        guardedGlobalServices.put(
                JpaInjectionServices.class,
                guard(jpaInjectionService, JpaInjectionServices.class)
        );
        return serviceRegistry;
    }

    public TestInstance newInstance(
            final String name,
            final Object testClassInstance
    ) {
        LOG.debug("Instantiating test run '{}' for class {}", name, testClassInstance.getClass().getName());
        final ServiceRegistry serviceRegistry = createInstanceServiceRegistry();
        final BeanDeploymentArchiveManagement bdaManagement = new BeanDeploymentArchiveManagement(
                runtime.getBeanArchiveDiscorvery(),
                serviceRegistry
        );
        final Collection<PluginTestInstance> pluginTestInstances = plugins.stream()
                .map(it -> it.instantiate(testClassInstance))
                .filter(it -> it != null) // Plugins return null when not interested in this beanArchiveDiscovery
                .collect(Collectors.toSet());
        return new TestInstance(name, bdaManagement, serviceRegistry, pluginTestInstances);
    }

    private ServiceRegistry createInstanceServiceRegistry() {
        final ServiceRegistry serviceRegistry = trace(new SimpleServiceRegistry(), ServiceRegistry.class);
        // Inherit test setup wide services to instances
        guardedGlobalServices.forEach((k, v) -> {
            LOG.debug("Registering global service {}", k.getName());
            serviceRegistry.add((Class<Service>) k, v);
        });

        // Only stubs from here on
        serviceRegistry.add(TransactionServices.class, trace(new TransactionServicesImpl(), TransactionServices.class));
        serviceRegistry.add(SecurityServices.class, trace(new SecurityServicesImpl(), SecurityServices.class));
        serviceRegistry.add(EjbInjectionServices.class, trace(new EjbInjectionServicesImpl(), EjbInjectionServices.class));
        serviceRegistry.add(ProxyServices.class, trace(new ProxyServicesImpl(), ProxyServices.class));
        serviceRegistry.add(ExecutorServices.class, trace(new ExecutorServicesImpl(), ExecutorServices.class));

        return serviceRegistry;
    }

    private static <T extends Service> T guard(final T value, final Class<T> clazz) {
        return (T) Proxy.newProxyInstance(
                TestSetup.class.getClassLoader(),
                new Class<?>[]{clazz},
                (proxy, method, args) -> {
                    try {
                        if ("cleanup".equals(method.getName())) {
                            return null;
                        }
                        return method.invoke(value, args);
                    } catch (final InvocationTargetException e) {
                        throw e.getTargetException();
                    }
                }
        );
    }

    public void shutdown() {
        plugins.forEach(it -> it.shutdown());
    }
}
