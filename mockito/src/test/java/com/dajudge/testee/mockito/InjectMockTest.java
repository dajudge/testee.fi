package com.dajudge.testee.mockito;

import com.dajudge.testee.runtime.TestRuntime;
import com.dajudge.testee.runtime.TestInstance;
import com.dajudge.testee.runtime.TestSetup;
import com.dajudge.testee.spi.Plugin;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mock;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

public class InjectMockTest {
    private final TestSetup testSetup = new TestSetup(TestBean.class, TestRuntime.instance());
    private final TestBean testClassInstance = new TestBean();
    private final TestInstance testInstance = testSetup.newInstance("myInstance", testClassInstance);

    @Test
    public void injects_well() {
        // Given
        when(testClassInstance.mock.doIt()).thenReturn("lolcats");

        // When
        testInstance.inject(testClassInstance);

        // Then
        assertNotNull(testClassInstance.bean); // Injection works
        assertEquals("lolcats", testClassInstance.bean.delegateIt()); // Mock actually works
    }

    @After
    public void cleanup() {
        testInstance.shutdown();
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
