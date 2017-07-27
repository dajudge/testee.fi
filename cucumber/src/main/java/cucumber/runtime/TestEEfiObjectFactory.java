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
package cucumber.runtime;

import cucumber.api.java.ObjectFactory;
import fi.testee.cucumber.annotation.CucumberSetup;
import fi.testee.exceptions.TestEEfiException;
import fi.testee.runtime.TestRuntime;
import fi.testee.runtime.TestSetup;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class TestEEfiObjectFactory implements ObjectFactory {
    private final TestSetup testSetup;

    private final Collection<Class<?>> glueClasses = new HashSet<>();
    private final Class<?> testSetupClass;

    private Map<Class<?>, Object> instances;
    private Object setupInstance;
    private TestSetup.TestContext context;

    public TestEEfiObjectFactory() {
        final Collection<Class<?>> candidates = TestRuntime.instance()
                .getBeanArchiveDiscorvery()
                .getClassesWith(CucumberSetup.class);
        if (candidates.isEmpty()) {
            throw new TestEEfiException("No class annotated with @CucumberSetup was found");
        }
        if (candidates.size() > 1) {
            throw new TestEEfiException("Multiple classes annotated with @CucumberSetup were found: " + candidates);
        }
        testSetupClass = candidates.iterator().next();
        testSetup = new TestSetup(testSetupClass, TestRuntime.instance());
    }

    @Override
    public void start() {
        if (instances != null || context != null || setupInstance != null) {
            throw new TestEEfiException(
                    "Failed to start cucumber test",
                    new IllegalStateException("Cucumber runtime has invalid state: " + instances + ", " + context)
            );
        }
        instances = new HashMap<>();
        try {
            setupInstance = testSetupClass.newInstance();
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new TestEEfiException("Failed to instantiate cucumber test setup class", e);
        }
        context = testSetup.prepareTestInstance(UUID.randomUUID().toString(), setupInstance);
    }

    @Override
    public void stop() {
        instances = null;
        setupInstance = null;
        context.shutdown();
        context = null;
    }

    @Override
    public boolean addClass(final Class<?> glueClass) {
        glueClasses.add(glueClass);
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getInstance(final Class<T> glueClass) {
        if (!instances.containsKey(glueClass)) {
            instances.put(glueClass, context.create(glueClass));
        }
        return (T) instances.get(glueClass);
    }
}
