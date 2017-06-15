package com.dajudge.testee.runtime;

import com.dajudge.testee.deployment.BeanArchiveDiscovery;
import com.dajudge.testee.deployment.BeanDeploymentArchiveManagement;
import com.dajudge.testee.deployment.DeploymentImpl;
import com.dajudge.testee.persistence.PersistenceUnitDiscovery;
import com.dajudge.testee.services.EjbInjectionServicesImpl;
import com.dajudge.testee.services.ExecutorServicesImpl;
import com.dajudge.testee.services.JpaInjectionServicesImpl;
import com.dajudge.testee.services.ProxyServicesImpl;
import com.dajudge.testee.services.ResourceInjectionServicesImpl;
import com.dajudge.testee.services.SecurityServicesImpl;
import com.dajudge.testee.services.TransactionServicesImpl;
import com.dajudge.testee.spi.Plugin;
import com.dajudge.testee.spi.ResourceProvider;
import com.dajudge.testee.spi.ResourceProviderFactory;
import org.jboss.weld.Container;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.CDI11Deployment;
import org.jboss.weld.context.CreationalContextImpl;
import org.jboss.weld.injection.spi.EjbInjectionServices;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.manager.api.ExecutorServices;
import org.jboss.weld.security.spi.SecurityServices;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.jboss.weld.transaction.spi.TransactionServices;

import javax.enterprise.inject.spi.Bean;
import java.util.Collection;
import java.util.Set;

import static com.dajudge.testee.utils.ProxyUtils.trace;
import static java.util.stream.Collectors.toSet;

/**
 * The holder of the static test runtime context. The class is thread safe.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class TestRuntime {
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

    public ServiceRegistry createServiceRegistry(final Class<?> testSetupClass) {
        // Shared in test runtime scope
        final Set<ResourceProviderFactory> resourceProviderFactories = getInstancesOf(ResourceProviderFactory.class);
        final Set<ResourceProvider> resourceProviders = resourceProviderFactories.stream()
                .map(it -> it.create(testSetupClass))
                .collect(toSet());

        // Not-shared in test runtime scope
        final PersistenceUnitDiscovery persistenceUnitDiscovery = new PersistenceUnitDiscovery(beanArchiveDiscovery);

        // Create a new registry based on the objects
        return createServiceRegistry(persistenceUnitDiscovery, resourceProviders);
    }

    private <T> Set<T> getInstancesOf(final Class<T> clazz) {
        return resolve(clazz).stream()
                .map(this::newInstance)
                .collect(toSet());
    }

    private ServiceRegistry createServiceRegistry(
            final PersistenceUnitDiscovery persistenceUnitDiscovery,
            final Collection<ResourceProvider> managedResources
    ) {
        final ServiceRegistry serviceRegistry = trace(new SimpleServiceRegistry(), ServiceRegistry.class);

        // Register services required for JEE integration
        serviceRegistry.add(JpaInjectionServices.class, new JpaInjectionServicesImpl(persistenceUnitDiscovery));
        serviceRegistry.add(ResourceInjectionServices.class, new ResourceInjectionServicesImpl(managedResources));

        // Only stubs from here on
        serviceRegistry.add(TransactionServices.class, trace(new TransactionServicesImpl(), TransactionServices.class));
        serviceRegistry.add(SecurityServices.class, trace(new SecurityServicesImpl(), SecurityServices.class));
        serviceRegistry.add(EjbInjectionServices.class, trace(new EjbInjectionServicesImpl(), EjbInjectionServices.class));
        serviceRegistry.add(ProxyServices.class, trace(new ProxyServicesImpl(), ProxyServices.class));
        serviceRegistry.add(ExecutorServices.class, trace(new ExecutorServicesImpl(), ExecutorServices.class));

        return serviceRegistry;
    }

    public BeanArchiveDiscovery getBeanArchiveDiscorvery() {
        return beanArchiveDiscovery;
    }
}
