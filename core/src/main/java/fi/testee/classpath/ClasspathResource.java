package fi.testee.classpath;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * A resource on the classpath. Thread safe.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class ClasspathResource {
    private byte[] data;
    private final JavaArchive.InputStreamSupplier streamSupplier;

    /**
     * Constructor.
     *
     * @param streamSupplier a {@link JavaArchive.InputStreamSupplier} supplying the input stream for the resource.
     */
    public ClasspathResource(final JavaArchive.InputStreamSupplier streamSupplier) {
        this.streamSupplier = streamSupplier;
    }

    public synchronized byte[] getBytes() throws IOException {
        if (data == null) {
            try (final InputStream is = streamSupplier.get()) {
                data = IOUtils.toByteArray(is);
            }
        }
        return data;
    }
}
