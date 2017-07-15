package com.dajudge.testee.jdbc;

import com.dajudge.testee.exceptions.TesteeException;
import com.dajudge.testee.spi.ConnectionFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import static com.dajudge.testee.utils.ProxyUtils.lazy;

/**
 * Container class for managing a data source for a {@link com.dajudge.testee.runtime.TestSetup}. It makes sure
 * the data source is requested only once, initialized lazily and closed if required on end.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
class ManagedDataSource {
    private final String name;
    private final Connection connection;
    private Connection originalConnection;
    private final ConnectionFactory factory;

    ManagedDataSource(
            final String name,
            final ConnectionFactory factory
    ) {
        this.name = name;
        this.factory = factory;
        connection = lazy(this::createConnection, Connection.class);
    }

    private Connection createConnection() {
        originalConnection = factory.createConnection("itest_" + UUID.randomUUID().toString());
        return wrap(originalConnection);
    }

    private Connection wrap(final Connection connection) {
        return (Connection) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> {
                    try {
                        if (method.getName().equals("close")) {
                            return null;
                        }
                        return method.invoke(connection, args);
                    } catch (final InvocationTargetException e) {
                        throw e.getCause();
                    }
                }
        );
    }

    void shutdown() {
        if (originalConnection != null) {
            try {
                originalConnection.close();
            } catch (final SQLException e) {
                throw new TesteeException("Failed to close database connection", e);
            }
        }
    }

    Connection getConnection() {
        return connection;
    }

    public String getName() {
        return name;
    }
}
