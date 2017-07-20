package com.dajudge.testee.eclipselink;

import org.eclipse.persistence.internal.jpa.deployment.ArchiveBase;
import org.eclipse.persistence.jpa.Archive;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.dajudge.testee.utils.IteratorUtils.composite;

/**
 * Composite pattern applied to {@link Archive}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class CompositeArchive extends ArchiveBase implements Archive {
    private final List<Archive> archives;

    /**
     * Constructor.
     *
     * @param rootUrl            the root URL.
     * @param descriptorLocation the descriptor location.
     * @param archives           the archives.
     */
    public CompositeArchive(
            final URL rootUrl,
            final String descriptorLocation,
            final List<Archive> archives
    ) {
        super(rootUrl, descriptorLocation);
        this.archives = archives;
    }

    @Override
    public Iterator<String> getEntries() {
        return composite(new ArrayList<>(archives), Archive::getEntries);
    }

    @Override
    public InputStream getEntry(final String entryPath) throws IOException {
        return findFirst(a -> a.getEntry(entryPath));
    }

    @Override
    public URL getEntryAsURL(final String entryPath) throws IOException {
        return findFirst(a -> a.getEntryAsURL(entryPath));
    }

    @Override
    public void close() {
        archives.forEach(Archive::close);
    }

    public interface ArchiveFunction<T> {
        T apply(Archive a) throws IOException;
    }

    private <T> T findFirst(final ArchiveFunction<T> f) throws IOException {
        for (final Archive archive : archives) {
            final T entry = f.apply(archive);
            if (entry != null) {
                return entry;
            }
        }
        return null;
    }
}
