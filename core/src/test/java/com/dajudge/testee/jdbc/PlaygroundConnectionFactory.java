package com.dajudge.testee.jdbc;

import com.dajudge.testee.spi.ConnectionFactory;

import java.sql.Connection;

import static org.mockito.Mockito.mock;

/**
 * Created by dajudge on 17.07.2017.
 */
public class PlaygroundConnectionFactory implements ConnectionFactory {
    static Connection c = mock(Connection.class);
    static boolean shutdown;

    @Override
    public Connection createConnection(final String name) {
        return c;
    }

    @Override
    public void release(final String dbName) {
    }
}
