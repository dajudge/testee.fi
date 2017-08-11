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
package fi.testee.rest;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.function.Consumer;

import static org.apache.commons.io.IOUtils.copy;

public class StaticResolver extends AutoDetectingStaticResourceResolver implements StaticResourceResolver {
    private static final Logger LOG = LoggerFactory.getLogger(StaticResolver.class);

    @Override
    protected Consumer<OutputStream> resolveResource(final String path) {
        final String resource = "static" + path;
        LOG.info("Resolving resource {}", resource);
        final URL url = getClass().getClassLoader().getResource(resource);
        return url == null ? null : os -> {
            try (final InputStream is = url.openStream()) {
                copy(is, os);
            } catch (final IOException e) {
                throw new RuntimeException("Failed to read resource", e);
            }
        };
    }
}
