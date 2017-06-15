package com.dajudge.testee;

import com.dajudge.testee.runtime.TestRuntime;
import com.dajudge.testee.runtime.TestInstance;
import com.dajudge.testee.runtime.TestSetup;
import org.junit.After;
import org.junit.Test;

import javax.inject.Inject;

import static org.junit.Assert.assertNotNull;

public class DependencyInjectionTest {
    private final TestSetup testSetup = new TestSetup(TestBean.class, TestRuntime.instance());
    private final TestBean testClassInstance = new TestBean();
    private final TestInstance testInstance = testSetup.newInstance(
            "myInstance",
            testClassInstance
    );

    @Test
    public void injects_well() {
        // When
        testInstance.inject(testClassInstance);

        // Then
        assertNotNull(testClassInstance.bean1); // Injection works
        assertNotNull(testClassInstance.bean1.bean2); // Injection works transitively
    }

    @After
    public void cleanup() {
        testInstance.shutdown();
    }

    static class ExampleBean2 {
    }

    static class ExampleBean1 {
        @Inject
        private ExampleBean2 bean2;
    }

    static class TestBean {
        @Inject
        private ExampleBean1 bean1;
    }

}
