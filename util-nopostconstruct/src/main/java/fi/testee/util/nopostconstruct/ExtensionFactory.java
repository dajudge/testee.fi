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
package fi.testee.util.nopostconstruct;

import fi.testee.spi.CdiExtensionFactory;
import fi.testee.util.nopostconstruct.annotation.NoPostConstructFor;

import javax.enterprise.inject.spi.Extension;
import java.lang.reflect.Method;

import static java.util.Arrays.asList;

public class ExtensionFactory implements CdiExtensionFactory {
    @Override
    public NoPostConstructExtension create(final Method method) {
        if (method == null || method.getAnnotation(NoPostConstructFor.class) == null) {
            return null;
        }
        return new NoPostConstructExtension(asList(method.getAnnotation(NoPostConstructFor.class).value()));
    }
}