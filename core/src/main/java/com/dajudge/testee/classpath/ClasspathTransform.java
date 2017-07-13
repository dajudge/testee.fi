package com.dajudge.testee.classpath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Transforms the classpath if necessary to compensate for some situations where IDEs and/or build tools
 * do funny things with the classpath that prevent clean bean archive detection.
 * <p>
 * Fixes an issue with gradle which creates separate classpath entries for the resources and the classes
 * which prevents proper discovery of bean archives since the classes are not in the same classpath entry as
 * the <code>META-INF/beans.xml</code> file. This situation is fixed by merging the corresponding entries using
 * {@link CompositeJavaArchive composite java archives}.</p>
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class ClasspathTransform {
    private static final Logger LOG = LoggerFactory.getLogger(ClasspathTransform.class);

    private ClasspathTransform() {
    }

    /**
     * Transforms the classpath.
     *
     * @param in the set of {@link JavaArchive java archives} to transform.
     * @return the transformed archives.
     */
    public static Collection<JavaArchive> transform(final Collection<JavaArchive> in) {
        final Collection<JavaArchive> ret = new HashSet<>();

        // JAR archives can be used directly
        in.stream()
                .filter(it -> it instanceof JarJavaArchive)
                .forEach(ret::add);

        // Directory entries transformed if matching
        in.stream()
                .filter(it -> it instanceof DirectoryJavaArchive)
                .peek(it -> LOG.trace("Processing directory java archive{}", it))
                .map(it -> (DirectoryJavaArchive) it)
                .filter(it -> getBuildDir(it) != null)
                .collect(Collectors.groupingBy(
                        it -> getBuildDir(it)
                ))
                .entrySet().stream()
                .map(it -> new CompositeJavaArchive(it.getValue(), it.getKey()))
                .forEach(ret::add);

        // Other directory entries
        in.stream()
                .filter(it -> it instanceof DirectoryJavaArchive)
                .map(it -> (DirectoryJavaArchive) it)
                .filter(it -> getBuildDir(it) == null)
                .forEach(ret::add);

        return ret;
    }

    private static URL getBuildDir(final DirectoryJavaArchive it) {
        return getBuildDir(it.getURL().toExternalForm());
    }

    private static URL getBuildDir(final String path) {
        final Pattern pattern = Pattern.compile("(.*/build/)[^/]+(/[^/]+/)");
        final Matcher matcher = pattern.matcher(path);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return new URL(matcher.group(1) + "[any]" + matcher.group(2));
        } catch (final MalformedURLException e) {
            throw new RuntimeException("Failed to build composite java archive URL", e);
        }
    }
}
