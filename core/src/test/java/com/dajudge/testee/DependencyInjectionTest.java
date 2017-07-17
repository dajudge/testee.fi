package com.dajudge.testee;

import com.dajudge.testee.runtime.TestRuntime;
import com.dajudge.testee.runtime.TestSetup;
import org.junit.Test;

import javax.inject.Inject;

import static org.junit.Assert.assertNotNull;

public class DependencyInjectionTest {
    private final TestSetup testSetup = new TestSetup(TestBean.class, TestRuntime.instance());
    private final TestBean testClassInstance = new TestBean();

    @Test
    public void injects_well() {
        // When
        testSetup.prepareTestInstance(
                "myInstance",
                testClassInstance
        ).run();

        // Then
        assertNotNull(testClassInstance.bean1); // Injection works
        assertNotNull(testClassInstance.bean2); // Injection in base class works
        assertNotNull(testClassInstance.bean1.bean2); // Injection works transitively
    }

    static class ExampleBean2 {
    }

    static class ExampleBean1 {
        @Inject
        private ExampleBean2 bean2;
    }

    static class BaseTestBean {
        @Inject
        protected ExampleBean2 bean2;
    }

    static class TestBean extends BaseTestBean {
        @Inject
        private ExampleBean1 bean1;
    }

}
