package com.dajudge.testee.deployment;

import com.dajudge.testee.classpath.JavaArchive;
import org.jboss.weld.ejb.spi.EjbDescriptor;

import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.stream.Collectors;

public class BeanArchive {
    private final JavaArchive classpathEntry;
    private final Collection<Class<? extends Annotation>> qualifyingAnnotations;
    private Collection<EjbDescriptor<?>> ejbs;

    @SuppressWarnings("unchecked")
    public BeanArchive(
            final JavaArchive javaArchive,
            final Collection<Class<? extends Annotation>> qualifyingAnnotations
    ) {
        this.classpathEntry = javaArchive;
        this.qualifyingAnnotations = qualifyingAnnotations;
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
        return hasBeansXml() || hasCdiExtension() || hasEjbs() || hasAdditionalQualification();
    }

    @SuppressWarnings("unchecked")
    private boolean hasAdditionalQualification() {
        return !classpathEntry.annotatedWith(qualifyingAnnotations.toArray(new Class[]{})).isEmpty();
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
