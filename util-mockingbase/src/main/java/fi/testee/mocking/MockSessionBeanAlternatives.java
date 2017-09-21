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

import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

import javax.inject.Inject;
import java.lang.reflect.Type;

public class MockSessionBeanAlternatives implements fi.testee.spi.SessionBeanAlternatives {
    @Inject
    private MockStore mockStore;

    @Override
    public ResourceReferenceFactory<Object> alternativeFor(final Type type) {
        final Object mock = mockStore.findFor(type, true);
        if (mock == null) {
            return null;
        }
        return () -> new ResourceReference<Object>() {
            @Override
            public Object getInstance() {
                return mock;
            }

            @Override
            public void release() {
            }
        };
    }
}
