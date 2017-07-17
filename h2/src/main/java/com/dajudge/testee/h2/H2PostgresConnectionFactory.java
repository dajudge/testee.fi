package com.dajudge.testee.h2;

import com.dajudge.testee.spi.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;

import static com.dajudge.testee.utils.JdbcUtils.execute;

/**
 * Factory for a {@link java.sql.Connection} backed by an in memory H2 database with the PostgreSQL dialect.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class H2PostgresConnectionFactory implements ConnectionFactory {
    private static final Logger LOG = LoggerFactory.getLogger(H2PostgresConnectionFactory.class);

    @Override
    public Connection createConnection(final String dbName) {
        return connect(dbName, -1);
    }

    private Connection connect(String dbName, int closeDelay) {
        final String url = "jdbc:h2:mem:" + dbName + ";MODE=PostgreSQL;DB_CLOSE_DELAY=" + closeDelay;
        LOG.debug("Creating connection to H2 database: " + url);
        return execute(
                () -> DriverManager.getConnection(url, "sa", ""),
                e -> "Failed to open connection to H2 database"
        );
    }

    @Override
    public void release(final String dbName) {
        // FIXME this never gets called
        execute(
                () -> {
                    connect(dbName, 0).close();
                    return null;
                },
                e -> "Failed to close H2 database"
        );
    }
}
