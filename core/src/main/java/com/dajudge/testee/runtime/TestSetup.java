package com.dajudge.testee.runtime;

import com.dajudge.testee.deployment.BeanDeploymentArchiveManagement;
import com.dajudge.testee.spi.PluginTestInstance;
import com.dajudge.testee.spi.PluginTestSetup;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Setup for a test. It contains the state shared by all instances of the test setup.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class TestSetup {
    private static final Logger LOG = LoggerFactory.getLogger(TestSetup.class);
    private final Set<PluginTestSetup> plugins;
    private final TestRuntime runtime;

    public TestSetup(
            final Class<?> setupClass,
            final TestRuntime runtime
    ) {
        this.runtime = runtime;
        plugins = runtime.getPlugins().stream()
                .map(it -> it.setup(setupClass))
                .filter(it -> it != null) // Plugins return null when not interested in this class
                .collect(Collectors.toSet());
    }

    public TestInstance newInstance(
            final String name,
            final Object testClassInstance
    ) {
        LOG.info("Instantiating test run '{}' for class {}", name, testClassInstance.getClass().getName());
        final ServiceRegistry serviceRegistry = runtime.createServiceRegistry(testClassInstance.getClass());
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

    public void shutdown() {
        plugins.forEach(it -> it.shutdown());
    }
}
