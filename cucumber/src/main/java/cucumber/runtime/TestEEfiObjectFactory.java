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
import fi.testee.spi.Releaser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.UUID.randomUUID;

public class TestEEfiObjectFactory implements ObjectFactory {
    private static final Logger LOG = LoggerFactory.getLogger(TestEEfiObjectFactory.class);

    private final TestSetup testSetup;

    private final Class<?> testSetupClass;

    private Map<Class<?>, Object> instances;
    private Object setupInstance;
    private TestSetup.TestInstance context;
    private Releaser releaser;

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
        testSetup = new TestSetup(testSetupClass, TestRuntime.instance()).init();
    }

    @Override
    public void start() {
        final String id = randomUUID().toString();
        LOG.debug("Starting test instance {}", id);
        if (instances != null || context != null || setupInstance != null || releaser != null) {
            throw new TestEEfiException(
                    "Failed to start cucumber test",
                    new IllegalStateException("Cucumber runtime has invalid state")
            );
        }
        instances = new HashMap<>();
        try {
            setupInstance = testSetupClass.newInstance();
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new TestEEfiException("Failed to instantiate cucumber test setup class", e);
        }
        try {
            context = testSetup.prepareTestInstance(id, setupInstance, null);
        }catch(final RuntimeException e) {
            testSetup.shutdown();
            throw e;
        }
        releaser = new Releaser();
    }

    @Override
    public void stop() {
        LOG.debug("Stopping test instance {}", context.getId());
        releaser.release();
        releaser = null;
        instances = null;
        setupInstance = null;
        context.shutdown();
        context = null;
    }

    @Override
    public boolean addClass(final Class<?> glueClass) {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getInstance(final Class<T> glueClass) {
        if (!instances.containsKey(glueClass)) {
            final T value = context.create(glueClass, releaser);
            LOG.trace("Created instance for {}: {}", glueClass, value);
            instances.put(glueClass, value);
        }
        return (T) instances.get(glueClass);
    }
}
