package com.dajudge.testee.classpath;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

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
        if (!(classLoader instanceof URLClassLoader)) {
            return Collections.emptySet();
        }
        return Arrays.stream(((URLClassLoader) classLoader).getURLs())
                .map(ClassLoaderAnalyzer::collectTransitive)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
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
