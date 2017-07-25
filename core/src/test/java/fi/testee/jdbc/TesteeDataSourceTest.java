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

import fi.testee.exceptions.TestEEfiException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TesteeDataSourceTest {
    @Mock
    private Connection connection;
    @Mock
    private Supplier<Connection> factory;

    private TesteeDataSource subject;

    @Before
    public void setup() {
        when(factory.get()).thenReturn(connection);
        subject = new TesteeDataSource("lolcats", factory);
    }

    @Test(expected = SQLFeatureNotSupportedException.class)
    public void does_not_support_logging() throws SQLFeatureNotSupportedException {
        subject.getParentLogger();
    }

    @Test
    public void sets_and_get_logWriter() throws SQLException {
        final PrintWriter out = mock(PrintWriter.class);
        subject.setLogWriter(out);
        assertSame(out, subject.getLogWriter());
    }

    @Test
    public void sets_and_gets_loginTimeout() throws SQLException {
        subject.setLoginTimeout(42);
        assertEquals(42, subject.getLoginTimeout());
    }

    @Test
    public void is_not_wrapper() throws SQLException {
        assertFalse(subject.isWrapperFor(Object.class));
    }

    @Test(expected = SQLException.class)
    public void does_not_unwrap() throws SQLException {
        assertNull(subject.unwrap(Object.class));
    }

    @Test
    public void initializes_lazy() throws SQLException {
        final Connection proxy = subject.getConnection();
        assertNotNull(proxy);
        verify(factory, never()).get();
        proxy.clearWarnings();
        proxy.clearWarnings();
        verify(factory).get();
    }

    @Test
    public void sets_autoCommit() throws SQLException {
        subject.getConnection().clearWarnings();
        verify(connection).setAutoCommit(false);
    }

    @Test
    public void prevents_close() throws SQLException {
        subject.getConnection().close();
        verify(connection, never()).close();
    }

    @Test
    public void prevents_commit() throws SQLException {
        subject.getConnection().commit();
        verify(connection, never()).commit();
    }

    @Test
    public void prevents_rollback() throws SQLException {
        subject.getConnection().rollback();
        verify(connection, never()).rollback();
    }

    @Test(expected = SQLException.class)
    public void rethrows_properly() throws SQLException {
        when(connection.getAutoCommit()).thenThrow(new SQLException("lolcats"));
        subject.getConnection().getAutoCommit();
    }

    @Test(expected = TestEEfiException.class)
    public void wraps_autocommit_exception() throws SQLException {
        doThrow(new SQLException("lolcats")).when(connection).setAutoCommit(anyBoolean());
        subject.getConnection("a", "b").getAutoCommit();
    }

    @Test
    public void closes_lazy() throws SQLException {
        subject.close();
        verify(factory, never()).get();
        subject.getConnection().getAutoCommit();
        subject.close();
        verify(connection).close();
    }

    @Test
    public void commits_lazy() throws SQLException {
        subject.commit();
        verify(factory, never()).get();
        subject.getConnection().getAutoCommit();
        subject.commit();
        verify(connection).commit();
    }

    @Test
    public void rollbacks_lazy() throws SQLException {
        subject.rollback();
        verify(factory, never()).get();
        subject.getConnection().getAutoCommit();
        subject.rollback();
        verify(connection).rollback();
    }
}
