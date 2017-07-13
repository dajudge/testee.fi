package com.dajudge.testee.runtime;

import com.dajudge.testee.deployment.BeanDeploymentArchiveManagement;
import com.dajudge.testee.deployment.DeploymentImpl;
import com.dajudge.testee.spi.PluginTestInstance;
import org.jboss.weld.Container;
import org.jboss.weld.bean.AbstractClassBean;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.CDI11Deployment;
import org.jboss.weld.context.CreationalContextImpl;
import org.jboss.weld.exceptions.UnsatisfiedResolutionException;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resolution.ResolvableBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.Bean;
import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.dajudge.testee.utils.ProxyUtils.trace;

/**
 * The instance of a {@link TestSetup}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class TestInstance {
    private static final Logger LOG = LoggerFactory.getLogger(TestInstance.class);

    private final String contextId;
    private final BeanDeploymentArchiveManagement bdaManagement;
    private final ServiceRegistry serviceRegistry;

    // Lazy state
    private Bootstrap bootstrap;

    /**
     * Constructor.
     *
     * @param bdaManagement   the management of {@link BeanDeploymentArchive BDAs}.
     * @param serviceRegistry the {@link ServiceRegistry}.
     */
    public TestInstance(
            final String contextId,
            final BeanDeploymentArchiveManagement bdaManagement,
            final ServiceRegistry serviceRegistry,
            final Collection<PluginTestInstance> pluginTestInstances
    ) {
        this.contextId = contextId;
        this.bdaManagement = bdaManagement;
        this.serviceRegistry = serviceRegistry;

        final CDI11Deployment deployment = trace(
                new DeploymentImpl(bdaManagement, serviceRegistry),
                CDI11Deployment.class
        );
        bootstrap = new WeldBootstrap()
                .startContainer(contextId, Environments.EE_INJECT, deployment)
                .startInitialization()
                .deployBeans()
                .validateBeans()
                .endInitialization();

        container().beanDeploymentArchives().values().stream()
                .map(BeanManagerImpl::getBeans)
                .flatMap(Collection::stream)
                .forEach(initializeBeans(pluginTestInstances));
    }

    private Consumer<Bean<?>> initializeBeans(final Collection<PluginTestInstance> pluginTestInstances) {
        return bean -> pluginTestInstances.forEach(it -> it.initializeForBean(bean));
    }

    private Container container() {
        return Container.instance(contextId);
    }

    /**
     * Injects dependencies.
     *
     * @param o the object to inject into.
     */
    public void inject(final Object o) {
        final Bean<Object> bean = resolveUnique((Class<Object>) o.getClass());
        if (!(bean instanceof AbstractClassBean)) {
            throw new RuntimeException("Injection of " + bean + " is not supported");
        }
        ((AbstractClassBean) bean).getProducer().inject(o, emptyContext());
    }

    private <T> CreationalContextImpl<T> emptyContext() {
        return new CreationalContextImpl<T>(null);
    }

    private <T> Bean<T> resolveUnique(final Class<T> clazz) {
        Set<Bean<T>> set = container().beanDeploymentArchives().values().stream()
                .map(it -> {
                    try {
                        return (Bean<T>) it.getBean(new ResolvableBuilder(it).addType(clazz).create());
                    } catch (final UnsatisfiedResolutionException e) {
                        return null;
                    }
                })
                .filter(it -> it != null)
                .collect(Collectors.toSet());
        if (set.isEmpty()) {
            throw new RuntimeException("No matching bean found for class " + clazz.getName());
        } else if (set.size() > 1) {
            throw new RuntimeException("Multiple ambiguous beans found for class " + clazz.getName());
        } else {
            return set.iterator().next();
        }
    }

    /**
     * Clean up after the test instance is finished.
     */
    public void shutdown() {
        LOG.info("Shutting down test instance '{}'", contextId);
        bootstrap.shutdown();
    }
}
