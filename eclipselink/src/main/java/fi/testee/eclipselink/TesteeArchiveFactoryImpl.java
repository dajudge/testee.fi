package fi.testee.eclipselink;

import fi.testee.utils.UrlUtils;
import org.eclipse.persistence.internal.jpa.deployment.ArchiveFactoryImpl;
import org.eclipse.persistence.jpa.Archive;
import org.eclipse.persistence.jpa.ArchiveFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static fi.testee.utils.UrlUtils.splitCompositeUrl;

/**
 * TestEE specific implementation of {@link ArchiveFactory}. This is necessary because some build tools use separate
 * classpath entries for resources and classes and a persistence unit thus is not necessarily contained in a single
 * classpath entry. This is handled via a custom URL protocol handler.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class TesteeArchiveFactoryImpl implements ArchiveFactory {
    public static final String COMPOSITE_URL_PREFIX = "file:/:testeeComposite/";
    private ArchiveFactory delegate = new ArchiveFactoryImpl();

    private interface ArchiveFromUrl {
        Archive create(URL url) throws URISyntaxException, IOException;
    }

    @Override
    public Archive createArchive(
            final URL rootUrl,
            final Map properties
    ) throws URISyntaxException, IOException {
        return createArchive(rootUrl, null, a -> delegate.createArchive(a, properties));
    }

    @Override
    public Archive createArchive(
            final URL rootUrl,
            final String descriptorLocation,
            final Map properties
    ) throws URISyntaxException, IOException {
        return createArchive(
                rootUrl,
                descriptorLocation,
                a -> delegate.createArchive(a, descriptorLocation, properties)
        );
    }

    private Archive createArchive(
            final URL rootUrl,
            final String descriptorLocation,
            final ArchiveFromUrl archiveFactory
    ) throws URISyntaxException, IOException {
        if (UrlUtils.isCompositeURL(rootUrl)) {
            final List<Archive> archives = new ArrayList<>();
            for (final URL url : splitCompositeUrl(rootUrl)) {
                archives.add(archiveFactory.create(url));
            }
            return new CompositeArchive(rootUrl, descriptorLocation, archives);
        } else {
            return archiveFactory.create(rootUrl);
        }
    }
}
