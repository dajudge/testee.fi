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
public class BeanDeploymentArchiveImpl implements BeanDeploymentArchive {
    private final ServiceRegistry serviceRegistry;
    private final BeanArchive beanArchive;

    public BeanDeploymentArchiveImpl(
            final ServiceRegistry serviceRegistry,
            final BeanArchive beanArchive
    ) {
        this.serviceRegistry = serviceRegistry;
        this.beanArchive = beanArchive;
    }

    @Override
    public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getBeanClasses() {
        return beanArchive.getBeanClasses();
    }

    @Override
    public BeansXml getBeansXml() {
        // FIXME: actually read the beans.xml
        return BeansXml.EMPTY_BEANS_XML;
    }

    @Override
    public Collection<EjbDescriptor<?>> getEjbs() {
        return beanArchive.getEjbs();
    }

    @Override
    public ServiceRegistry getServices() {
        return serviceRegistry;
    }

    @Override
    public String getId() {
        return beanArchive.getClasspathEntry().getURL().toExternalForm();
    }
}
