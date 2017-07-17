package com.dajudge.testee;

import com.dajudge.testee.runtime.TestRuntime;
import com.dajudge.testee.runtime.TestSetup;
import org.junit.After;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.junit.Assert.assertNotNull;

public class PersistenceUnitTest {
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
        assertNotNull(testClassInstance.entityManager); // Injection works
    }

    static class TestBean {
        @PersistenceContext(unitName = "testUnit")
        private EntityManager entityManager;
    }
}
