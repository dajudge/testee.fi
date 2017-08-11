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
package fi.testee.mocking;

import fi.testee.spi.CdiExtensionFactory;

import javax.enterprise.inject.spi.Extension;
import javax.inject.Inject;
import java.lang.reflect.Method;

public class MockingExtensionFactory implements CdiExtensionFactory {
    @Inject
    private MockStore mockStore;
    @Inject
    private MockingDynamicArchiveContributor contributor;

    @Override
    public Extension create(final Method method) {
        return new MockingExtension(contributor, mockStore);
    }
}
