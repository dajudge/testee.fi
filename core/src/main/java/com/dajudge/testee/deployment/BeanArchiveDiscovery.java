package com.dajudge.testee.deployment;

import com.dajudge.testee.classpath.Classpath;
import com.dajudge.testee.classpath.ClasspathResource;
import com.dajudge.testee.classpath.ClasspathTransform;
import com.dajudge.testee.classpath.JavaArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.Collections.unmodifiableCollection;
import static java.util.stream.Collectors.toSet;

/**
 * Discovery of bean archives on the classpath. Thread safe. Singleton, since classpath is constant at runtime.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class BeanArchiveDiscovery {
    private static final Logger LOG = LoggerFactory.getLogger(BeanArchiveDiscovery.class);
    private static final Predicate<JavaArchive> HAS_BEANS_XML =
            cp -> cp.findResource("META-INF/beans.xml") != null;
    private static final Predicate<JavaArchive> HAS_CDI_EXTENSION =
            cp -> cp.findResource("META-INF/services/javax.enterprise.inject.spi.CDIProvider") != null;

    private Collection<JavaArchive> beanArchives = null;

    /**
     * Constructor.
     */
    public BeanArchiveDiscovery() {
    }

    /**
     * Returns the available bean archives on the classpath.
     *
     * @return the available bean archives in an immutable collection.
     */
    public synchronized Collection<JavaArchive> getBeanArchives() {
        if (beanArchives == null) {
            beanArchives = unmodifiableCollection(discoverBeanArchives());
        }
        return beanArchives;
    }

    public Set<ClasspathResource> findResource(final String name) {
        return getBeanArchives().stream()
                .map(it -> it.findResource(name))
                .filter(it -> it != null)
                .collect(toSet());
    }

    private static Collection<JavaArchive> discoverBeanArchives() {
        final long start = System.currentTimeMillis();
        final Classpath cp = new Classpath(BeanArchiveDiscovery.class.getClassLoader());
        final Collection<JavaArchive> classpathEntries = cp.getAll();
        LOG.trace("Bean archive discovery using these classpath entries: {}", classpathEntries);
        final Collection<JavaArchive> transformed = ClasspathTransform.transform(classpathEntries);
        final Collection<JavaArchive> beanArchiveClasspathEntries = transformed.stream()
                .filter(HAS_BEANS_XML.or(HAS_CDI_EXTENSION))
                .collect(toSet());
        LOG.info("Bean archive discovery completed in {}ms", System.currentTimeMillis() - start);
        return beanArchiveClasspathEntries;
    }
}
