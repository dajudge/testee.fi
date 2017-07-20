package com.dajudge.testee.deployment;

import com.dajudge.testee.classpath.Classpath;
import com.dajudge.testee.classpath.ClasspathTransform;
import com.dajudge.testee.classpath.JavaArchive;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * Discovery of bean archives on the classpath. Thread safe. Singleton, since classpath is constant at runtime.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class BeanArchiveDiscovery {
    private static final Logger LOG = LoggerFactory.getLogger(BeanArchiveDiscovery.class);

    private final Collection<BeanArchive> beanArchives;

    /**
     * Returns the available bean archives on the classpath.
     *
     * @return the available bean archives in an immutable collection.
     */
    public synchronized Collection<BeanArchive> getBeanArchives() {
        return beanArchives;
    }

    public BeanArchiveDiscovery() {
        final long start = System.currentTimeMillis();
        final Classpath cp = new Classpath(BeanArchiveDiscovery.class.getClassLoader());
        final Collection<JavaArchive> classpathEntries = cp.getAll();
        LOG.trace("Bean archive discovery using these classpath entries: {}", classpathEntries);
        final Collection<JavaArchive> transformed = ClasspathTransform.transform(classpathEntries);
        beanArchives = transformed.parallelStream()
                .map(BeanArchive::new)
                .filter(BeanArchive::isRelevant)
                .collect(toSet());
        LOG.debug("Bean archive discovery completed in {}ms", System.currentTimeMillis() - start);
    }

    public Set<EjbDescriptor<?>> getSessionBeans() {
        return beanArchives.stream().map(BeanArchive::getEjbs).flatMap(Collection::stream).collect(toSet());
    }
}
