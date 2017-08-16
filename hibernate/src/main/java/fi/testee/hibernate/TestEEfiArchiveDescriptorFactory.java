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
package fi.testee.hibernate;

import fi.testee.exceptions.TestEEfiException;
import fi.testee.utils.UrlUtils;
import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;

import java.net.URL;

import static fi.testee.utils.UrlUtils.isCompositeURL;
import static fi.testee.utils.UrlUtils.splitCompositeUrl;
import static java.util.stream.Collectors.toSet;

public class TestEEfiArchiveDescriptorFactory implements ArchiveDescriptorFactory {
    private static final StandardArchiveDescriptorFactory delegate = new StandardArchiveDescriptorFactory();

    @Override
    public ArchiveDescriptor buildArchiveDescriptor(final URL url) {
        return buildArchiveDescriptor(url, "");
    }

    @Override
    public ArchiveDescriptor buildArchiveDescriptor(final URL url, final String path) {
        if (isCompositeURL(url)) {
            return new CompositeArchiveDescriptor(splitCompositeUrl(url).stream()
                    .map(it -> delegate.buildArchiveDescriptor(it, path))
                    .collect(toSet()));
        }
        return delegate.buildArchiveDescriptor(url, path);
    }

    @Override
    public URL getJarURLFromURLEntry(final URL url, final String entry) throws IllegalArgumentException {
        if (isCompositeURL(url)) {
            throw new TestEEfiException("NONONOOOOOO");
        }
        return delegate.getJarURLFromURLEntry(url, entry);
    }

    @Override
    public URL getURLFromPath(final String jarPath) {
        return delegate.getURLFromPath(jarPath);
    }
}
