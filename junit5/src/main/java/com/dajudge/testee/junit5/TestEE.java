package com.dajudge.testee.junit5;

import com.dajudge.testee.exceptions.TesteeException;
import com.dajudge.testee.runtime.TestRuntime;
import com.dajudge.testee.runtime.TestSetup;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import static java.util.UUID.randomUUID;

/**
 * JUnit 5 extension for TestEE.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class TestEE implements
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
        final Runnable shutdown = (Runnable) context.getStore(NS).get("Shutdown");
        if (shutdown != null) {
            shutdown.run();
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
        Runnable shutdown = testSetup.prepareTestInstance(randomUUID().toString(), testInstance);
        context.getStore(NS).put("Shutdown", shutdown);
    }

    private static Class<?> testClassOf(final ExtensionContext context) {
        return context.getTestClass().orElseThrow(() -> new TesteeException("No test class found"));
    }

}
