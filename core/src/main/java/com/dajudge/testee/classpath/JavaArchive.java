package com.dajudge.testee.classpath;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;

/**
 * Interface for a classpath entry. Implementations must be thread safe.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public interface JavaArchive {
    /**
     * Finds a resource in the classpath entry.
     *
     * @param s the name of the resource to find.
     * @return the resource if found, <code>null</code> otherwise.
     */
    ClasspathResource findResource(final String s);

    /**
     * The URL to the classpath entry.
     *
     * @return the URl to the classpath entry.
     */
    URL getURL();

    /**
     * Returns all classes  in the java archive.
     *
     * @return all classes.
     */
    Collection<String> getClasses();

    interface InputStreamSupplier {
        InputStream get() throws IOException;
    }
}
