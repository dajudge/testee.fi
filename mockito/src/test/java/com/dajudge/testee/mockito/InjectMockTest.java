package com.dajudge.testee.mockito;

import com.dajudge.testee.runtime.TestRuntime;
import com.dajudge.testee.runtime.TestSetup;
import org.junit.Test;
import org.mockito.Mock;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

public class InjectMockTest {

    @Test
    public void injects_well() {
        // Given
        final TestSetup testSetup = new TestSetup(TestBean.class, TestRuntime.instance());
        final TestBean testClassInstance = new TestBean();

        // When
        final Runnable cleanup = testSetup.prepareTestInstance("myInstance", testClassInstance);
        when(testClassInstance.mock.doIt()).thenReturn("lolcats");

        try {
            // Then
            assertNotNull(testClassInstance.bean); // Injection works
            assertEquals("lolcats", testClassInstance.bean.delegateIt()); // Mock actually works
        } finally {
            cleanup.run();
        }
    }

    static class ExampleBean2 {
        public String doIt() {
            return "I am IronMan";
        }
    }

    static class ExampleBean1 {
        @Inject
        private ExampleBean2 bean2;

        public String delegateIt() {
            return bean2.doIt();
        }
    }

    static class TestBean {
        @Inject
        private ExampleBean1 bean;
        @Mock
        private ExampleBean2 mock;
    }

}
