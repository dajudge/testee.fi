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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

/**
 * Retrieving the URLs of the classpath JARs from a
 * {@link URLClassLoader} implementation as provided by Oracle's JRE8.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class ClassLoaderAnalyzer {
    private static final Logger LOG = LoggerFactory.getLogger(ClassLoaderAnalyzer.class);

    private ClassLoaderAnalyzer() {
    }

    public static Set<URL> getClasspath(final ClassLoader classLoader) {
        LOG.trace("System properties: {}", System.getProperties());
        final Set<URL> collect = collectFromClasspath(classLoader);
        collect.addAll(collectFromLibraryPath());
        return collect;
    }

    private static Set<URL> collectFromLibraryPath() {
        return stream(StringUtils.split(System.getProperty("java.class.path"), File.pathSeparatorChar))
                .map(it -> new File(it).getAbsoluteFile())
                .filter(File::exists) // Early out non-existent files
                .map(UrlUtils::toUrl)
                .collect(toSet());
    }

    private static Set<URL> collectFromClasspath(ClassLoader classLoader) {
        if (!(classLoader instanceof URLClassLoader)) {
            return Collections.emptySet();
        }
        final Set<URL> ret = new HashSet<>();
        ret.addAll(stream(((URLClassLoader) classLoader).getURLs())
                .map(ClassLoaderAnalyzer::collectTransitive)
                .flatMap(Collection::stream)
                .collect(toSet()));
        if (classLoader.getParent() != classLoader && classLoader.getParent() != null) {
            ret.addAll(collectFromClasspath(classLoader.getParent()));
        }
        return ret;
    }

    private static List<URL> collectTransitive(final URL start) {
        final File startFile = new File(start.getFile());
        if (!startFile.exists()) {
            return Collections.emptyList();
        }
        final List<URL> ret = new ArrayList<>();
        ret.add(start);
        if (startFile.isFile()) {
            getClassPathsFromManifest(start).stream()
                    .map(ClassLoaderAnalyzer::collectTransitive)
                    .forEach(ret::addAll);
        }
        return ret;
    }

    private static List<URL> getClassPathsFromManifest(final URL jarUrl) {
        try {
            final String classPath = safeGetClassPath(new JarFile(jarUrl.getFile()));
            if (!StringUtils.isBlank(classPath)) {
                final StringTokenizer tokenizer = new StringTokenizer(classPath);
                final List<URL> urls = new ArrayList<>(tokenizer.countTokens());
                while (tokenizer.hasMoreTokens()) {
                    urls.add(new URL(jarUrl, tokenizer.nextToken()));
                }
                return urls;
            }
        } catch (IOException e) {
            LOG.warn("Could not open JAR for URL " + jarUrl, e);
        }
        return Collections.emptyList();
    }

    private static String safeGetClassPath(final JarFile jar) throws IOException {
        final Manifest mf = jar.getManifest();
        if (mf != null && mf.getMainAttributes() != null) {
            return mf.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
        }
        return null;
    }
}
