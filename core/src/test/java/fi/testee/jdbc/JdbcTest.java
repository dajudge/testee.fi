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
package fi.testee.jdbc;

import fi.testee.runtime.TestRuntime;
import fi.testee.runtime.TestSetup;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JdbcTest {
    private TestSetup testSetup;

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
        PlaygroundConnectionFactory.shutdown = false;
        testSetup = new TestSetup(SubClass.class, TestRuntime.instance());
        testSetup.prepareTestInstance(UUID.randomUUID().toString(), testClassInstance).shutdown();
    }

    @Test
    public void injects_well() {
        assertNotNull("DataSource was not injected", testClassInstance.dataSource);
    }

    @Test
    public void sets_up_test_data() {
        assertTrue("TestData setup not performed", BaseClass.baseClass && BaseClass.subClass);
    }

    @Test
    public void shuts_down_properly() {
        testSetup.shutdown();
        assertTrue("DataSource factory was not shut down properly", PlaygroundConnectionFactory.shutdown);
    }
}
