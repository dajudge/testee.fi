package com.dajudge.testee.jdbc;

import com.dajudge.testee.runtime.TestRuntime;
import com.dajudge.testee.runtime.TestSetup;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class JdbcTest {

    @TestDataSource(name = "jdbc/test", factory = PlaygroundConnectionFactory.class)
    private static abstract class BaseClass {
        static boolean baseClass = false;
        static boolean subClass = false;

        @Resource(mappedName = "jdbc/test")
        protected DataSource dataSource;

        @TestData
        private static void setupBaseClass() {
            assertFalse("Base class must be initialized first", subClass);
            baseClass = true;
        }
    }

    public static class SubClass extends BaseClass {
        @TestData
        private static void setupSubClass() {
            subClass = true;
        }
    }

    private final SubClass testClassInstance = new SubClass();

    @Before
    public void resetStatic() {
        BaseClass.baseClass = false;
        BaseClass.subClass = false;
        final TestSetup testSetup = new TestSetup(SubClass.class, TestRuntime.instance());
        testSetup.prepareTestInstance(UUID.randomUUID().toString(), testClassInstance).run();
    }

    @Test
    public void injects_well() {
        assertNotNull("DataSource was not injected", testClassInstance.dataSource);
    }

    @Test
    public void sets_up_test_data() {
        assertTrue("TestData setup not performed", BaseClass.baseClass && BaseClass.subClass);
    }
}
