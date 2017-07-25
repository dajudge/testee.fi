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
