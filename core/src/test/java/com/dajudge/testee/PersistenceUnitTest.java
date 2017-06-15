package com.dajudge.testee;

import com.dajudge.testee.runtime.TestRuntime;
import com.dajudge.testee.runtime.TestInstance;
import com.dajudge.testee.runtime.TestSetup;
import org.junit.After;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.junit.Assert.assertNotNull;

public class PersistenceUnitTest {
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
        assertNotNull(testClassInstance.entityManager); // Injection works
    }

    @After
    public void cleanup() {
        testInstance.shutdown();
    }

    static class TestBean {
        @PersistenceContext(unitName = "testUnit")
        private EntityManager entityManager;
    }
}
