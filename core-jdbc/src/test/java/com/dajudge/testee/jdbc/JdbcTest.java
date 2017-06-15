package com.dajudge.testee.jdbc;

import com.dajudge.testee.jdbc.spi.DataSourceFactory;
import com.dajudge.testee.runtime.TestInstance;
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
    private static final TestSetup TEST_SETUP = new TestSetup(SubClass.class, TestRuntime.instance());

    public static class PlaygroundDataSourceFactory implements DataSourceFactory {
        static DataSource dataSource = mock(DataSource.class);

        @Override
        public DataSource create() {
            return dataSource;
        }

        @Override
        public void shutdown() {

        }
    }

    private static abstract class BaseClass {
        static boolean baseClass = false;
        static boolean subClass = false;

        @TestData
        private static void setupBaseClass() {
            assertFalse("Base class must be initialized first", subClass);
            baseClass = true;
        }
    }

    @TestDataSource(name = "jdbc/test", factory = PlaygroundDataSourceFactory.class)
    static class SubClass extends BaseClass {
        @Resource(mappedName = "jdbc/test")
        private DataSource dataSource;

        @TestData
        private static void setupSubClass() {
            subClass = true;
        }
    }

    private final SubClass testClassInstance = new SubClass();
    private TestInstance instance;

    @Before
    public void resetStatic() {
        BaseClass.baseClass = false;
        BaseClass.subClass = false;
        instance = TEST_SETUP.newInstance(UUID.randomUUID().toString(), testClassInstance);
        instance.inject(testClassInstance);
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
