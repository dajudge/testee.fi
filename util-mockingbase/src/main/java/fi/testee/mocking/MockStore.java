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

import fi.testee.mocking.spi.MockContributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

@Singleton
public class MockStore {
    @Inject
    @Any
    private Instance<MockContributor> mockContributors;

    private Map<Field, Object> mocks = new HashMap<>();

    @PostConstruct
    public void init() {
        mockContributors.forEach(c -> {
            mocks.putAll(c.contributeMocks());
        });

    }

    public void forEach(final BiConsumer<? super Field, ? super Object> action) {
        mocks.forEach(action);
    }

    public Object findFor(final Type type) {
        if (!(type instanceof Class)) {
            // TODO handle more cases
            return null;
        }
        final Class<?> clazz = (Class<?>) type;
        for (final Object o : mocks.values()) {
            // TODO handle ambiguous mocks
            if (clazz.isAssignableFrom(o.getClass())) {
                return o;
            }
        }
        return null;
    }
}
