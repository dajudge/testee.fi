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

import fi.testee.exceptions.TestEEfiException;
import fi.testee.mocking.spi.MockContributor;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import static java.util.stream.Collectors.toSet;
import static net.bytebuddy.matcher.ElementMatchers.any;
import static org.jboss.weld.resolution.CovariantTypes.isAssignableFrom;

@Singleton
public class MockStore {
    public static final String MOCK_OBJECT_FIELD = "__testeefi_mockObject__";
    @Inject
    @Any
    private Instance<MockContributor> mockContributors;

    private Map<Field, Pair<Object, Object>> mocks = new HashMap<>();

    @PostConstruct
    public void init() {
        mockContributors.forEach(c -> {
            c.contributeMocks().forEach((f, o) -> {
                mocks.put(f, new ImmutablePair<>(o, wrap(f, o)));
            });
        });

    }

    private Object wrap(final Field field, final Object mock) {
        final InvocationHandler invocationHandler = (proxy, method, args) -> method.invoke(mock, args);
        final Class<?> mockType = new ByteBuddy()
                .subclass(field.getType())
                .defineField(MOCK_OBJECT_FIELD, field.getType(), Modifier.PUBLIC)
                .method(any()).intercept(InvocationHandlerAdapter.of(invocationHandler))
                .make()
                .load(getClass().getClassLoader())
                .getLoaded();
        try {
            final Object o = mockType.newInstance();
            mockType.getField(MOCK_OBJECT_FIELD).set(o, mock);
            return o;
        } catch (final NoSuchFieldException | InstantiationException | IllegalAccessException e) {
            throw new TestEEfiException("Failed to instantiate mock proxy", e);
        }
    }

    public void forEachType(
            final Collection<Type> types,
            final boolean wrapped,
            final BiConsumer<? super Field, ? super Object> action
    ) {
        types.stream()
                .map(this::findEntryFor)
                .filter(Objects::nonNull)
                .forEach(e -> action.accept(e.getKey(), wrapped ? e.getValue().getRight() : e.getValue().getLeft()));
    }

    private Map.Entry<Field, Pair<Object, Object>> findEntryFor(final Type type) {
        for (final Map.Entry<Field, Pair<Object, Object>> e : mocks.entrySet()) {
            // TODO handle ambiguous mocks
            if (isAssignableFrom(type, e.getKey().getType())) {
                return e;
            }
        }
        return null;
    }

    public Object findFor(final Type type, final boolean wrapped) {
        final Map.Entry<Field, Pair<Object, Object>> entry = findEntryFor(type);
        if (entry == null) {
            return null;
        }
        return wrapped ? entry.getValue().getRight() : entry.getValue().getLeft();
    }

    public Collection<Class<?>> getMockClasses() {
        return mocks.values().stream()
                .map(it -> it.getRight())
                .map(Object::getClass)
                .collect(toSet());
    }
}
