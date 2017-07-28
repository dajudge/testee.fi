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
package fi.testee.classpath;

import fi.testee.exceptions.TestEEfiException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static fi.testee.utils.UrlUtils.toUrl;

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
        super(toUrl(file));
        this.file = file;
    }

    protected <T> T iterate(final Callback<T> cb) {
        try (final ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                final T ret = cb.item(streamProvider(entry), entry.getName());
                if (ret != null) {
                    return ret;
                }
                entry = zis.getNextEntry();
            }
            return null;
        } catch (final IOException e) {
            throw new TestEEfiException("Could not read JAR file " + file.getAbsolutePath(), e);
        }
    }

    private InputStreamSupplier streamProvider(final ZipEntry needle) {
        return () -> {
            final ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
            while (!zis.getNextEntry().getName().equals(needle.getName())) {
            }
            return zis;
        };
    }

    @Override
    public URL getURL() {
        return toUrl(file);
    }

    @Override
    public String toString() {
        return "JarClasspathEntry{" +
                "file=" + file +
                '}';
    }
}
