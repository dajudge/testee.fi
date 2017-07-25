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
package fi.testee.classpath;

import fi.testee.utils.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Utilities on classpath. Thread safe.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class Classpath {
    private static final Logger LOG = LoggerFactory.getLogger(Classpath.class);
    private final ClassLoader classLoader;

    private Collection<JavaArchive> entries;

    /**
     * Constructor.
     *
     * @param classLoader the {@link ClassLoader} to be used for retrieving the classpath entries.
     */
    public Classpath(final ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Returns all {@link JavaArchive java archives} on the classpath.
     *
     * @return all {@link JavaArchive java archives} on the classpath.
     */
    public synchronized Collection<JavaArchive> getAll() {
        if (entries == null) {
            entries = ClassLoaderAnalyzer.getClasspath(classLoader).stream()
                    .peek(entry -> LOG.trace("Classpath entry: {}", entry.getFile()))
                    .filter(url -> UrlUtils.toFile(url).exists())
                    .peek(entry -> LOG.trace("Existing classpath entry: {}", entry))
                    .map(Classpath::toClasspathEntry)
                    .collect(Collectors.toSet());
        }
        return entries;
    }

    private static JavaArchive toClasspathEntry(final URL url) {
        final File file = UrlUtils.toFile(url);
        if (file.isDirectory()) {
            return new DirectoryJavaArchive(file);
        } else {
            return new JarJavaArchive(file);
        }
    }
}
