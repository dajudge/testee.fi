package com.dajudge.testee.classpath;

import com.dajudge.testee.exceptions.TesteeException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * JAR-file based {@link JavaArchive}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class JarJavaArchive extends AbstractBaseJavaArchive {
    private final File file;

    /**
     * Constructor.
     *
     * @param file the JAR file.
     */
    public JarJavaArchive(final File file) {
        this.file = file;
    }

    protected <T> T iterate(final Callback<T> cb) {
        try (final ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                final T ret = cb.item(() -> zis, entry.getName());
                if (ret != null) {
                    return ret;
                }
                entry = zis.getNextEntry();
            }
            return null;
        } catch (final IOException e) {
            throw new TesteeException("Could not read JAR file " + file.getAbsolutePath(), e);
        }
    }

    @Override
    public URL getURL() {
        try {
            return file.toURI().toURL();
        } catch (final MalformedURLException e) {
            throw new TesteeException("Failed to create URL for classpath entry: " + file.getAbsolutePath(), e);
        }
    }

    @Override
    public String toString() {
        return "JarClasspathEntry{" +
                "file=" + file +
                '}';
    }
}
