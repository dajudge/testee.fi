package com.dajudge.testee.h2;

import com.dajudge.testee.exceptions.TesteeException;
import com.dajudge.testee.spi.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Factory for a {@link java.sql.Connection} backed by an in memory H2 database with the PostgreSQL dialect.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class H2PostgresConnectionFactory implements ConnectionFactory {
    private static final Logger LOG = LoggerFactory.getLogger(H2PostgresConnectionFactory.class);

    @Override
    public Connection createConnection(final String dbName) {
        final String url = "jdbc:h2:mem:" + dbName + ";MODE=PostgreSQL";
        LOG.debug("Creating connection to H2 database: " + url);
        try {
            return DriverManager.getConnection(url, "sa", "");
        } catch (final SQLException e) {
            throw new TesteeException("Failed to open connection to H2 database", e);
        }
    }

}
