/*
 * Copyright (C) 2017 Alex Stockinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fi.testee.eclipselink;

import org.eclipse.persistence.internal.jpa.deployment.ArchiveBase;
import org.eclipse.persistence.jpa.Archive;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static fi.testee.utils.IteratorUtils.composite;

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
