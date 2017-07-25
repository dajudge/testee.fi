package fi.testee.deployment;

import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.CDI11Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;

import javax.enterprise.inject.spi.Extension;
import java.util.Collection;
import java.util.Collections;

/**
 * Implementation of the {@link CDI11Deployment}, which is basically tying together the service
 * registry and the beans for a deployment, where a deployment is the context in which an application
 * runs.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class DeploymentImpl implements CDI11Deployment {
    private final ServiceRegistry serviceRegistry;
    private final BeanDeploymentArchiveManagement bdaManagement;

    /**
     * Constructor.
     *
     * @param bdaManagement   the management of the {@link BeanDeploymentArchive BDAs}.
     * @param serviceRegistry the {@link ServiceRegistry}.
     */
    public DeploymentImpl(
            final BeanDeploymentArchiveManagement bdaManagement,
            final ServiceRegistry serviceRegistry
    ) {
        this.bdaManagement = bdaManagement;
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public BeanDeploymentArchive getBeanDeploymentArchive(Class<?> aClass) {
        return null;
    }

    @Override
    public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
        return bdaManagement.getArchives();
    }

    @Override
    public BeanDeploymentArchive loadBeanDeploymentArchive(final Class<?> aClass) {
        return bdaManagement.findByClass(aClass);
    }

    @Override
    public ServiceRegistry getServices() {
        return serviceRegistry;
    }

    @Override
    public Iterable<Metadata<Extension>> getExtensions() {
        return Collections.emptyList();
    }
}
