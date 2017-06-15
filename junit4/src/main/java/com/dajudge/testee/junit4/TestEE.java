package com.dajudge.testee.junit4;

import com.dajudge.testee.runtime.TestRuntime;
import com.dajudge.testee.runtime.TestInstance;
import com.dajudge.testee.runtime.TestSetup;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.util.HashMap;
import java.util.Map;

/**
 * JUnit 4 {@link Runner} integrating TestEE.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class TestEE extends BlockJUnit4ClassRunner {
    private final TestSetup testSetup;
    private final Map<FrameworkMethod, TestInstance> instances = new HashMap<>();

    /**
     * Creates a TestEE test runner to run {@code klass}.
     *
     * @param klass the test class to run.
     * @throws InitializationError if the test class is malformed.
     */
    public TestEE(final Class<?> klass) throws InitializationError {
        super(klass);
        testSetup = new TestSetup(klass, TestRuntime.instance());
    }

    @Override
    protected Statement withBefores(
            final FrameworkMethod method,
            final Object target,
            final Statement statement
    ) {
        createInstanceFor(method, target);
        return super.withBefores(method, target, statement);
    }

    private synchronized void createInstanceFor(final FrameworkMethod method, final Object target) {
        final String instanceId = target.getClass().getName()
                + ":"
                + method.getMethod().toString()
                + ":"
                + System.identityHashCode(target);
        final TestInstance testInstance = testSetup.newInstance(instanceId, target);
        testInstance.inject(target);
        instances.put(method, testInstance);

    }

    private synchronized void shutdownInstanceFor(final FrameworkMethod method) {
        instances.get(method).shutdown();
    }

    @Override
    protected Statement withAfters(
            final FrameworkMethod method,
            final Object target,
            final Statement statement
    ) {
        try {
            return super.withAfters(method, target, statement);
        } finally {
            shutdownInstanceFor(method);
        }
    }

    @Override
    public void run(final RunNotifier notifier) {
        try {
            super.run(notifier);
        } finally {
            testSetup.shutdown();
        }
    }
}
