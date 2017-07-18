package com.dajudge.testee.jdbc;

import com.dajudge.testee.exceptions.TesteeException;
import com.dajudge.testee.utils.JdbcUtils;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static com.dajudge.testee.utils.ProxyUtils.lazy;
import static java.util.Arrays.asList;

/**
 * Implementation of a {@link DataSource}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
class TesteeDataSource implements DataSource {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(TesteeDataSource.class);

    private final String name;
    private final Supplier<Connection> connectionFactory;

    private Connection connection;
    private PrintWriter out;
    private int loginTimeout;

    TesteeDataSource(final String name, final Supplier<Connection> connectionFactory) {
        this.name = name;
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return safe(lazy(() -> {
            synchronized (TesteeDataSource.this) {
                if (connection == null) {
                    LOG.debug("Creating connection {}", name);
                    connection = connectionFactory.get();
                    try {
                        connection.setAutoCommit(false);
                    } catch (final SQLException e) {
                        throw new TesteeException("Failed to set auto-commit to false on JDBC connection", e);
                    }
                }
                return connection;
            }
        }, Connection.class));
    }

    private static Connection safe(final Connection connection) {
        return (Connection) Proxy.newProxyInstance(
                TesteeDataSource.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    try {
                        if (asList("close", "commit", "rollback").contains(method.getName())) {
                            return null;
                        }
                        return method.invoke(connection, args);
                    } catch (final InvocationTargetException e) {
                        throw e.getTargetException();
                    }
                }
        );
    }

    @Override
    public Connection getConnection(final String username, final String password) throws SQLException {
        return getConnection();
    }

    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        throw new SQLException("Don't know how to unwrap to " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return out;
    }

    @Override
    public void setLogWriter(final PrintWriter out) throws SQLException {
        this.out = out;
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        this.loginTimeout = seconds;
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return loginTimeout;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("JUL not used");
    }

    void rollback() throws SQLException {
        withConnection(c -> {
            LOG.debug("Rolling back connection {} on {}", name, this);
            c.rollback();
        });
    }

    void commit() throws SQLException {
        withConnection(c -> {
            LOG.debug("Committing connection {} on {}", name, this);
            c.commit();
        });
    }

    void close() throws SQLException {
        withConnection(c -> {
            LOG.debug("Closing connection to {} on {}", name, this);
            c.close();
        });
    }

    private void withConnection(final JdbcUtils.JdbcConsumer<Connection> runnable) throws SQLException {
        if (connection != null) {
            runnable.run(connection);
        }
    }

}
