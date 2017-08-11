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

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import static java.util.stream.Collectors.toSet;
import static org.jboss.weld.resolution.CovariantTypes.isAssignableFrom;

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

    public void forEachType(final Collection<Type> types, final BiConsumer<? super Field, ? super Object> action) {
        types.stream()
                .map(this::findEntryFor)
                .filter(Objects::nonNull)
                .forEach(e -> action.accept(e.getKey(), e.getValue()));
    }

    private Map.Entry<Field, Object> findEntryFor(final Type type) {
        for (final Map.Entry<Field, Object> e : mocks.entrySet()) {
            // TODO handle ambiguous mocks
            if (isAssignableFrom(type, e.getValue().getClass())) {
                return e;
            }
        }
        return null;
    }

    public Object findFor(final Type type) {
        final Map.Entry<Field, Object> entry = findEntryFor(type);
        return entry == null ? null : entry.getValue();
    }

    public Collection<Class<?>> getMockClasses() {
        return mocks.values().stream().map(Object::getClass).collect(toSet());
    }
}
