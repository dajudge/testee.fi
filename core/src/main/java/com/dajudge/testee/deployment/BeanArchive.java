package com.dajudge.testee.deployment;

import com.dajudge.testee.classpath.JavaArchive;
import org.jboss.weld.ejb.spi.EjbDescriptor;

import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import java.util.Collection;
import java.util.stream.Collectors;

public class BeanArchive {
    private final JavaArchive classpathEntry;
    private Collection<EjbDescriptor<?>> ejbs;

    @SuppressWarnings("unchecked")
    public BeanArchive(final JavaArchive javaArchive) {
        this.classpathEntry = javaArchive;
    }

    @SuppressWarnings("unchecked")
    private <T> EjbDescriptor<T> toEjbDescriptor(final Class<T> clazz) {
        return new EjbDescriptorImpl<>(clazz);
    }

    public Collection<String> getBeanClasses() {
        return classpathEntry.getClasses();
    }

    public synchronized Collection<EjbDescriptor<?>> getEjbs() {
        if (ejbs == null) {
            ejbs = classpathEntry.annotatedWith(Singleton.class, Stateless.class, Stateful.class).stream()
                    .map(this::toEjbDescriptor)
                    .collect(Collectors.toSet());
        }
        return ejbs;
    }

    public JavaArchive getClasspathEntry() {
        return classpathEntry;
    }

    public boolean isRelevant() {
        return hasBeansXml() || hasCdiExtension() || hasEjbs();
    }

    private boolean hasEjbs() {
        return !getEjbs().isEmpty();
    }

    private boolean hasCdiExtension() {
        return getClasspathEntry().findResource("META-INF/services/javax.enterprise.inject.spi.CDIProvider") != null;
    }

    private boolean hasBeansXml() {
        return getClasspathEntry().findResource("META-INF/beans.xml") != null;
    }

}
