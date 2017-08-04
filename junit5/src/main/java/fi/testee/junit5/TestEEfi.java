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
package fi.testee.junit5;

import fi.testee.exceptions.TestEEfiException;
import fi.testee.runtime.TestRuntime;
import fi.testee.runtime.TestSetup;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import static java.util.UUID.randomUUID;

/**
 * JUnit 5 extension for TestEE.fi.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class TestEEfi implements
        Extension,
        AfterEachCallback,
        AfterAllCallback,
        BeforeAllCallback,
        TestInstancePostProcessor {
    private static final ExtensionContext.Namespace NS = ExtensionContext.Namespace.create(randomUUID());

    @Override
    public void afterAll(final ExtensionContext context) throws Exception {
        final TestSetup testSetup = (TestSetup) context.getStore(NS).get(TestSetup.class);
        if (testSetup != null) {
            testSetup.shutdown();
        }

    }

    @Override
    public void afterEach(final ExtensionContext context) throws Exception {
        final TestSetup.TestContext testContext = (TestSetup.TestContext) context
                .getStore(NS)
                .get(TestSetup.TestContext.class);
        if (testContext != null) {
            testContext.shutdown();
        }
    }

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        final Class<?> testClass = testClassOf(context);
        final TestSetup testSetup = new TestSetup(testClass, TestRuntime.instance());
        context.getStore(NS).put(TestSetup.class, testSetup);
    }

    @Override
    public void postProcessTestInstance(final Object testInstance, final ExtensionContext context) throws Exception {
        final TestSetup testSetup = (TestSetup) context.getStore(NS).get(TestSetup.class);
        final TestSetup.TestContext testContext = testSetup.prepareTestInstance(
                randomUUID().toString(),
                testInstance,
                context.getTestMethod().orElse(null)
        );
        context.getStore(NS).put(TestSetup.TestContext.class, testContext);
    }

    private static Class<?> testClassOf(final ExtensionContext context) {
        return context.getTestClass().orElseThrow(() -> new TestEEfiException("No test class found"));
    }

}
