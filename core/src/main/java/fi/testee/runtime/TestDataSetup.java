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
package fi.testee.runtime;

import fi.testee.exceptions.TestEEfiException;
import fi.testee.jdbc.TestData;
import fi.testee.spi.TestDataSetupAccessorFactory;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

/**
 * Control of {@link TestData @TestData setup}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class TestDataSetup {
    private static final Logger LOG = LoggerFactory.getLogger(TestDataSetup.class);

    @Inject
    @Any
    private Instance<TestDataSetupAccessorFactory> providers;

    public void setupTestData(
            final Class<?> setupClass,
            final ServiceRegistry serviceRegistry
    ) {
        final Set<Object> testDataSetupAccessors = new HashSet<>();
        providers.forEach(it -> testDataSetupAccessors.add(it.createTestDataSetupAccessor(serviceRegistry)));
        setupTestData(setupClass, testDataSetupAccessors);
    }

    public static void setupTestData(final Class<?> setupClass, final Collection<Object> testDataSetupAccessors) {
        Class<?> currentClass = setupClass;
        final List<Method> methodsToInvoke = new ArrayList<>();
        while (currentClass != null && currentClass != Object.class) {
            final Set<Method> candidates = stream(currentClass.getDeclaredMethods())
                    .filter(it -> it.getAnnotation(TestData.class) != null)
                    .collect(toSet());
            currentClass = currentClass.getSuperclass();
            if (candidates.isEmpty()) {
                continue;
            }
            if (candidates.size() > 1) {
                throw new IllegalStateException("Only one @TestData method allowed per class");
            }
            final Method candidate = candidates.iterator().next();
            if (!Modifier.isStatic(candidate.getModifiers())) {
                throw new IllegalStateException("Methods annotated with @TestData must be static");
            }
            methodsToInvoke.add(0, candidate);
        }
        methodsToInvoke.forEach(it -> {
            LOG.debug("Invoking @TestData method {}", it.toString());
            try {
                it.setAccessible(true);
                final Object[] arguments = resolveArguments(it.getParameterTypes(), testDataSetupAccessors);
                it.invoke(null, arguments);
            } catch (final IllegalAccessException | InvocationTargetException e) {
                throw new TestEEfiException("Failed to invoke @TestData method", e);
            }
        });
    }

    private static Object[] resolveArguments(final Class<?>[] types, final Collection<Object> candidates) {
        final Object[] values = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            values[i] = find(types[i], candidates);
        }
        return values;
    }

    private static Object find(final Class<?> type, final Collection<Object> candidates) {
        final Set<Object> matches = new HashSet<>();
        for (final Object candidate : candidates) {
            if (type.isAssignableFrom(candidate.getClass())) {
                matches.add(candidate);
            }
        }
        if (matches.isEmpty()) {
            throw new TestEEfiException("Unkonwn type for @TestData method parameter: " + type.getName());
        }
        if (matches.size() > 1) {
            throw new TestEEfiException("Ambiguous @TestData method parameter type: " + type.getName());
        }
        return matches.iterator().next();
    }

}
