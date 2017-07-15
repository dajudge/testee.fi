package com.dajudge.testee.runtime;

import com.dajudge.testee.deployment.BeanArchiveDiscovery;
import com.dajudge.testee.deployment.BeanDeploymentArchiveManagement;
import com.dajudge.testee.deployment.DeploymentImpl;
import com.dajudge.testee.spi.Plugin;
import com.dajudge.testee.spi.RuntimeLifecycleListener;
import org.jboss.weld.Container;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.CDI11Deployment;
import org.jboss.weld.context.CreationalContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.Bean;
import java.util.Collection;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * The holder of the static test runtime context. The class is thread safe.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class TestRuntime {
    private static final Logger LOG = LoggerFactory.getLogger(TestRuntime.class);
    private static final String CONTEXT_ID = TestRuntime.class.getName();
    private static TestRuntime instance;

    private final BeanArchiveDiscovery beanArchiveDiscovery = new BeanArchiveDiscovery();
    private Set<Plugin> plugins;

    /**
     * Access to the singleton plugin registry.
     *
     * @return the plugin registry.
     */
    public synchronized static TestRuntime instance() {
        if (instance == null) {
            instance = new TestRuntime();
        }
        return instance;
    }

    private TestRuntime() {
        final ServiceRegistry serviceRegistry = new SimpleServiceRegistry();
        final BeanDeploymentArchiveManagement bdaManagement = new BeanDeploymentArchiveManagement(
                beanArchiveDiscovery,
                serviceRegistry
        );
        final CDI11Deployment deployment = new DeploymentImpl(bdaManagement, serviceRegistry);
        final Bootstrap bootstrap = new WeldBootstrap()
                .startContainer(CONTEXT_ID, Environments.SE, deployment)
                .startInitialization()
                .deployBeans()
                .validateBeans()
                .endInitialization();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> bootstrap.shutdown()));
        LOG.trace("Notifying runtime lifecycle listeners about start");
        getInstancesOf(RuntimeLifecycleListener.class).forEach(it -> it.onRuntimeStarted());
    }

    public synchronized Collection<Plugin> getPlugins() {
        if (plugins == null) {
            plugins = getInstancesOf(Plugin.class);
        }
        return plugins;
    }

    private <T> T newInstance(final Bean<T> bean) {
        return bean.create(new CreationalContextImpl<>(null));
    }

    private <T> Collection<Bean<T>> resolve(final Class<T> clazz) {
        return Container.instance(CONTEXT_ID).beanDeploymentArchives().values().stream()
                .map(archive -> archive.getBeans(clazz).stream().map(bean -> (Bean<T>) bean).collect(toSet()))
                .flatMap(Collection::stream)
                .collect(toSet());
    }

    <T> Set<T> getInstancesOf(final Class<T> clazz) {
        return resolve(clazz).stream()
                .map(this::newInstance)
                .collect(toSet());
    }

    public BeanArchiveDiscovery getBeanArchiveDiscorvery() {
        return beanArchiveDiscovery;
    }
}
