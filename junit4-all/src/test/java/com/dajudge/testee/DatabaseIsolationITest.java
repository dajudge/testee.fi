package com.dajudge.testee;

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
