package com.dajudge.testee.classpath;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import static com.dajudge.testee.utils.UrlUtils.createCompositeUrl;
import static java.util.stream.Collectors.toList;

/**
 * A {@link JavaArchive} merging multiple delegate archives to one.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class CompositeJavaArchive implements JavaArchive {
    private final Collection<JavaArchive> archives;
    private final URL url;

    /**
     * Constructor.
     *
     * @param archives the archives to make look like one.
     */
    public CompositeJavaArchive(final Collection<? extends JavaArchive> archives) {
        this.archives = Collections.unmodifiableCollection(archives);
        this.url = createCompositeUrl(archives.stream()
                .map(JavaArchive::getURL)
                .collect(toList())
        );
    }


    @Override
    public ClasspathResource findResource(final String s) {
        for (final JavaArchive archive : archives) {
            final ClasspathResource ret = archive.findResource(s);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public Collection<String> getClasses() {
        return archives.stream().map(JavaArchive::getClasses).flatMap(Collection::stream).collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return "CompositeJavaArchive{" +
                "url=" + url +
                '}';
    }
}
