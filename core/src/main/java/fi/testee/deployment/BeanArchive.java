/*
 * Copyright (C) 2017 Alex Stockinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fi.testee.deployment;

import fi.testee.classpath.JavaArchive;
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
    private Collection<EjbDescriptorImpl<?>> ejbs;

    @SuppressWarnings("unchecked")
    public BeanArchive(
            final JavaArchive javaArchive,
            final Collection<Class<? extends Annotation>> qualifyingAnnotations
    ) {
        this.classpathEntry = javaArchive;
        this.qualifyingAnnotations = qualifyingAnnotations;
    }

    @SuppressWarnings("unchecked")
    private <T> EjbDescriptorImpl<T> toEjbDescriptor(final Class<T> clazz) {
        return new EjbDescriptorImpl<>(clazz);
    }

    public Collection<String> getBeanClasses() {
        return classpathEntry.getClasses();
    }

    public synchronized Collection<EjbDescriptorImpl<?>> getEjbs() {
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

    @Override
    public String toString() {
        return "BeanArchive{" +
                "classpathEntry=" + classpathEntry +
                '}';
    }

    public Collection<Class<?>> getClassesWith(final Class<? extends Annotation> annotation) {
        return classpathEntry.annotatedWith(annotation);
    }
}
