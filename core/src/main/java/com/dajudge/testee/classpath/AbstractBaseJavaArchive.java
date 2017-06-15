package com.dajudge.testee.classpath;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

/**
 * Base class for real java archives.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public abstract class AbstractBaseJavaArchive implements JavaArchive {
    private Collection<String> classes;

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
    public synchronized final Collection<String> getClasses() {
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

    protected interface Callback<T> {
        T item(InputStreamSupplier zipInputStream, String name) throws IOException;
    }

    protected abstract <T> T iterate(Callback<T> cb);
}
