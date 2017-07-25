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

import org.junit.Test;

import java.sql.SQLException;

public class DatabaseIsolationITest extends AbstractBaseDatabaseTest {
    @Test
    public void tests_run_isolated_1() throws SQLException {
        dataSources().forEach(ds -> insertJdbc(ds, 3, "value3"));
    }

    @Test
    public void tests_run_isolated_2() throws SQLException {
        // One of these two tests will fail when tests don't run isolated
        tests_run_isolated_1();
    }
}
