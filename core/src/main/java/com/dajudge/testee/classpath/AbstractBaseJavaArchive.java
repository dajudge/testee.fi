package com.dajudge.testee.classpath;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;

import static java.util.Arrays.asList;

/**
 * Base class for real java archives.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
abstract class AbstractBaseJavaArchive implements JavaArchive {
    private Collection<String> classes;
    private final AnnotationScanner annotationScanner;

    protected AbstractBaseJavaArchive(final URL... urls) {
        this.annotationScanner = new AnnotationScanner(urls);
    }

    @Override
    public final synchronized ClasspathResource findResource(final String s) {
        final Callback<ClasspathResource> cb = (is, name) -> {
            if (name.equals(s)) {
                return new ClasspathResource(is);
            }
            return null;
        };
        return iterate(cb);
    }

    @Override
    public final synchronized Collection<String> getClasses() {
        if (classes != null) {
            return classes;
        }
        classes = new HashSet<>();
        iterate((is, name) -> {
            if (name.endsWith(".class")) {
                classes.add(StringUtils.removeEnd(name.replace("/", "."), ".class"));
            }
            return null;
        });
        return classes;
    }

    protected abstract <T> T iterate(Callback<T> cb);

    protected interface Callback<T> {
        T item(InputStreamSupplier zipInputStream, String name) throws IOException;
    }

    @Override
    public Collection<Class<?>> annotatedWith(final Class<? extends Annotation>[] annotations) {
        return annotationScanner.scanFor(annotations);
    }
}
