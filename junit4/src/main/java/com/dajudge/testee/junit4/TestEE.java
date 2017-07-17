package com.dajudge.testee.junit4;

import com.dajudge.testee.runtime.TestRuntime;
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
    private final Map<FrameworkMethod, Runnable> instances = new HashMap<>();

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
        final Runnable testShutdown = testSetup.prepareTestInstance(instanceId, target);
        instances.put(method, testShutdown);

    }

    private synchronized void shutdownInstanceFor(final FrameworkMethod method) {
        instances.get(method).run();
    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        try {
            super.runChild(method, notifier);
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
