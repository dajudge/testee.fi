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

import javax.activation.MimetypesFileTypeMap;
import java.io.OutputStream;
import java.util.function.Consumer;

public abstract class AutoDetectingStaticResourceResolver implements StaticResourceResolver {
    @Override
    public ResolvedResource resolve(final String path) {
        final Consumer<OutputStream> ret = resolveResource(path);
        return ret == null ? null : new ResolvedResource() {
            @Override
            public String getContentType() {
                return AutoDetectingStaticResourceResolver.this.getContentType(path);
            }

            @Override
            public void getContent(final OutputStream out) {
                ret.accept(out);
            }
        };
    }

    private String getContentType(final String path) {
        return new MimetypesFileTypeMap().getContentType(path);
    }

    protected abstract Consumer<OutputStream> resolveResource(String path);
}
