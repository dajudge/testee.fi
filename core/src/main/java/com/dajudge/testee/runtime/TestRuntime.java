package com.dajudge.testee.runtime;

import com.dajudge.testee.deployment.BeanArchiveDiscovery;
import com.dajudge.testee.spi.RuntimeLifecycleListener;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The holder of the static test runtime context. The class is thread safe.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class TestRuntime {
    private static final Logger LOG = LoggerFactory.getLogger(TestRuntime.class);
    private static TestRuntime instance;

    private final BeanArchiveDiscovery beanArchiveDiscovery = new BeanArchiveDiscovery();
    private final DependencyInjectionRealm realm;

    /**
     * Access to the singleton plugin registry.
     *
     * @return the plugin registry.
     */
    public static synchronized TestRuntime instance() {
        if (instance == null) {
            instance = new TestRuntime();
        }
        return instance;
    }

    private TestRuntime() {
        final ServiceRegistry serviceRegistry = new SimpleServiceRegistry();
        realm = new DependencyInjectionRealm(serviceRegistry, beanArchiveDiscovery, Environments.SE);
        Runtime.getRuntime().addShutdownHook(new Thread(realm::shutdown));
        LOG.trace("Notifying runtime lifecycle listeners about start");
        realm.getInstancesOf(RuntimeLifecycleListener.class).forEach(RuntimeLifecycleListener::onRuntimeStarted);
    }

    public BeanArchiveDiscovery getBeanArchiveDiscorvery() {
        return beanArchiveDiscovery;
    }
}
