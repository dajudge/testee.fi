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
package fi.testee;

import fi.testee.interceptor.TestInterceptor;
import fi.testee.interceptor.TestInterceptor.Type;
import fi.testee.runtime.TestRuntime;
import fi.testee.runtime.TestSetup;
import org.junit.Before;

import java.util.Iterator;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class BaseDependencyInjectionTest<T> {
    private final Class<T> clazz;
    protected T root;
    private TestSetup.TestInstance context;
    private TestSetup testSetup;

    protected BaseDependencyInjectionTest(final Class<T> clazz) {
        this.clazz = clazz;
    }

    abstract T instance();

    protected void reset() {
    }

    @Before
    public void setup() throws NoSuchMethodException {
        reset();
        TestInterceptor.INVOCATIONS.clear();
        testSetup = new TestSetup(clazz, TestRuntime.instance()).init();
        root = instance();
        context = testSetup.prepareTestInstance("myInstance", root, null);
    }

    public void shutdown() {
        if (context != null) {
            context.shutdown();
        }
        if (testSetup != null) {
            testSetup.shutdown();
        }
    }

    void runTest(final Runnable runtime, final Runnable afterShutdown) {
        try {
            runtime.run();
        } catch (final Exception e) {
            shutdown();
            throw e;
        }
        shutdown();
        if (afterShutdown != null) {
            afterShutdown.run();
        }
        assertEquals(
                "Not all interceptor invocations have been asserted",
                emptyList(),
                TestInterceptor.INVOCATIONS.stream().map(this::toString).collect(toList())
        );
    }

    protected void ensureInterception(
            final Class<?> target,
            final String methodName,
            final Type type
    ) {
        ensureInterception(target, methodName, type, false);
    }

    protected void ensureInterceptionUnordered(
            final Class<?> target,
            final String methodName,
            final Type type
    ) {
        ensureInterception(target, methodName, type, true);
    }

    private void ensureInterception(
            final Class<?> target,
            final String methodName,
            final Type type,
            final boolean anywhere
    ) {
        final String expectedString = "(" + target + " " + methodName + " " + type + ")";
        assertFalse("No invocations left when looking for " + expectedString, TestInterceptor.INVOCATIONS.isEmpty());
        if (anywhere) {
            final Iterator<TestInterceptor.Invocation> it = TestInterceptor.INVOCATIONS.iterator();
            while (it.hasNext()) {
                final TestInterceptor.Invocation invocation = it.next();
                try {

                    assertInvocation(target, methodName, type, expectedString, invocation);
                    it.remove();
                    return;
                } catch (final AssertionError e) {
                    // Try next one
                }
            }
            fail("No invocation matched " + expectedString);
        } else {
            final TestInterceptor.Invocation invocation = TestInterceptor.INVOCATIONS.remove(0);
            assertInvocation(target, methodName, type, expectedString, invocation);
        }
    }

    private void assertInvocation(Class<?> target, String methodName, Type type, String expectedString, TestInterceptor.Invocation invocation) {
        try {
            assertTrue(target + " != " + invocation.target.getClass(), target.isAssignableFrom(invocation.target.getClass()));
            assertEquals(methodName, invocation.method == null ? null : invocation.method.getName());
            assertEquals(type, invocation.type);
        } catch (final AssertionError e) {
            throw new AssertionError("Expected " + expectedString + " but got " + toString(invocation), e);
        }
    }

    private String toString(TestInterceptor.Invocation invocation) {
        return "(" + invocation.target + ", " + (invocation.method == null ? null : invocation.method.getName()) + ", "
                + invocation.type + ")";
    }
}
