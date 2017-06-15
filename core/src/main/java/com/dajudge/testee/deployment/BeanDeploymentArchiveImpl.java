package com.dajudge.testee.deployment;

import com.dajudge.testee.classpath.JavaArchive;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.ejb.spi.EjbDescriptor;

import java.util.Collection;
import java.util.Collections;

/**
 * Implementation of a {@link BeanDeploymentArchive} (or short <i>BDA</i>), which basically links a {@link JavaArchive}
 * to a {@link DeploymentImpl deployment}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
class BeanDeploymentArchiveImpl implements BeanDeploymentArchive {
    private final ServiceRegistry serviceRegistry;
    private final JavaArchive classpathEntry;

    /**
     * Constructor.
     *
     * @param serviceRegistry the {@link ServiceRegistry} for this BDA.
     * @param javaArchive     the {@link JavaArchive} for this BDA.
     */
    public BeanDeploymentArchiveImpl(
            final ServiceRegistry serviceRegistry,
            final JavaArchive javaArchive
    ) {
        this.serviceRegistry = serviceRegistry;
        this.classpathEntry = javaArchive;
    }

    @Override
    public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
        // TODO: find out what this is for
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getBeanClasses() {
        return classpathEntry.getClasses();
    }

    @Override
    public BeansXml getBeansXml() {
        // FIXME: actually read the beans.xml
        return BeansXml.EMPTY_BEANS_XML;
    }

    @Override
    public Collection<EjbDescriptor<?>> getEjbs() {
        // TODO: EJB support
        return Collections.emptyList();
    }

    @Override
    public ServiceRegistry getServices() {
        return serviceRegistry;
    }

    @Override
    public String getId() {
        return classpathEntry.getURL().toExternalForm();
    }

    @Override
    public String toString() {
        return "BeanDeploymentArchive for " + classpathEntry;
    }
}
