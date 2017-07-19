package com.dajudge.testee.classpath;

import com.dajudge.testee.exceptions.TesteeException;
import org.scannotation.AnnotationDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Annotation scanning.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class AnnotationScanner {
    private final Supplier<Collection<URL>> classpathProvider;
    private Map<String, Set<String>> index;

    public AnnotationScanner(final Supplier<Collection<URL>> classpathProvider) {
        this.classpathProvider = classpathProvider;
    }

    public Collection<Class<?>> scanFor(Class<? extends Annotation>... annotations) {
        final Collection<Class<?>> ret = new ArrayList<>();
        Arrays.stream(annotations).forEach(annotation -> {
                    final Set<String> classNames = index().get(annotation.getName());
                    if (classNames != null) {
                        classNames.forEach(clazz -> {
                                    try {
                                        ret.add(Class.forName(clazz));
                                    } catch (final ClassNotFoundException e) {
                                        throw new TesteeException("Failed to load class", e);
                                    }
                                }
                        );
                    }
                }
        );
        return ret;
    }

    private synchronized Map<String, Set<String>> index() {
        if (index == null) {
            final AnnotationDB db = new AnnotationDB();
            final URL urls[] = classpathProvider.get().toArray(new URL[]{});
            try {
                db.scanArchives(urls);
            } catch (final IOException e) {
                throw new TesteeException("Failed to perform annotation scanning", e);
            }
            index = db.getAnnotationIndex();

        }
        return index;
    }
}
