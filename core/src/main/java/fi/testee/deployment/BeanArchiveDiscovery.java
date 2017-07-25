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

import fi.testee.classpath.Classpath;
import fi.testee.classpath.ClasspathTransform;
import fi.testee.classpath.JavaArchive;
import fi.testee.spi.QualifyingAnnotationExtension;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.ServiceLoader.load;
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
                .map(it ->new BeanArchive(it, collectQualifyingAnnotations()))
                .filter(BeanArchive::isRelevant)
                .peek(archive -> LOG.trace("Relevant bean archive: {}", archive.getClasspathEntry().getURL()))
                .collect(toSet());
        LOG.debug("Bean archive discovery completed in {}ms", System.currentTimeMillis() - start);
    }

    private Collection<Class<? extends Annotation>> collectQualifyingAnnotations() {
        final Collection<Class<? extends Annotation>> ret = new HashSet<>();
        load(QualifyingAnnotationExtension.class)
                .iterator()
                .forEachRemaining(it -> ret.addAll(it.getQualifyingAnnotations()));
        return ret ;
    }

    public Set<EjbDescriptor<?>> getSessionBeans() {
        return beanArchives.stream().map(BeanArchive::getEjbs).flatMap(Collection::stream).collect(toSet());
    }
}
