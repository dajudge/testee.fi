package com.dajudge.testee.deployment;

import com.dajudge.testee.classpath.JavaArchive;
import org.jboss.weld.ejb.spi.BusinessInterfaceDescriptor;
import org.jboss.weld.ejb.spi.EjbDescriptor;

import javax.ejb.Local;
import javax.ejb.Remove;
import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

public class BeanArchive {
    private final JavaArchive classpathEntry;
    private final Collection<EjbDescriptor<?>> ejbs;

    public BeanArchive(final JavaArchive javaArchive) {
        this.classpathEntry = javaArchive;
        ejbs = classpathEntry.annotatedWith(Singleton.class, Stateless.class, Stateful.class).stream()
                .map(this::toEjbDescriptor)
                .collect(Collectors.toSet());
    }

    private <T> EjbDescriptor<T> toEjbDescriptor(final Class<T> clazz) {
        return new EjbDescriptor<T>() {
            private final Collection<BusinessInterfaceDescriptor<?>> localBusinessInterfaces = new HashSet<>();
            private final Collection<BusinessInterfaceDescriptor<?>> remoteBusinessInterfaces = new HashSet<>();

            {
                localBusinessInterfaces.add((BusinessInterfaceDescriptor) () -> clazz);
                stream(clazz.getInterfaces()).forEach(iface -> {
                    if (iface.getAnnotation(Local.class) != null) {
                        localBusinessInterfaces.add((BusinessInterfaceDescriptor) () -> iface);
                    }
                    if (iface.getAnnotation(Remove.class) != null) {
                        remoteBusinessInterfaces.add((BusinessInterfaceDescriptor) () -> iface);
                    }
                });
            }

            @Override
            public Class<T> getBeanClass() {
                return clazz;
            }

            @Override
            public Collection<BusinessInterfaceDescriptor<?>> getLocalBusinessInterfaces() {
                return localBusinessInterfaces;
            }

            @Override
            public Collection<BusinessInterfaceDescriptor<?>> getRemoteBusinessInterfaces() {
                return remoteBusinessInterfaces;
            }

            @Override
            public String getEjbName() {
                // TODO do something sane here?
                return getBeanClass().getSimpleName();
            }

            @Override
            public Collection<Method> getRemoveMethods() {
                // TODO implement this
                return Collections.emptySet();
            }

            @Override
            public boolean isStateless() {
                return getBeanClass().getAnnotation(Stateless.class) != null;
            }

            @Override
            public boolean isSingleton() {
                return getBeanClass().getAnnotation(Singleton.class) != null;
            }

            @Override
            public boolean isStateful() {
                return getBeanClass().getAnnotation(Stateful.class) != null;
            }

            @Override
            public boolean isMessageDriven() {
                return false;
            }

            @Override
            public boolean isPassivationCapable() {
                return false;
            }
        };
    }

    public Collection<String> getBeanClasses() {
        return classpathEntry.getClasses();
    }

    public Collection<EjbDescriptor<?>> getEjbs() {
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
