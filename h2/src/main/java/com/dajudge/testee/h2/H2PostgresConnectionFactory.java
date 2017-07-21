package com.dajudge.testee.h2;

import com.dajudge.testee.spi.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashSet;
import java.util.Set;

import static com.dajudge.testee.utils.JdbcUtils.execute;

/**
 * Factory for a {@link java.sql.Connection} backed by an in memory H2 database with the PostgreSQL dialect.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
@Singleton
public class H2PostgresConnectionFactory implements ConnectionFactory {
    private static final Logger LOG = LoggerFactory.getLogger(H2PostgresConnectionFactory.class);

    private Set<String> dbNames = new HashSet<>();

    @Override
    public Connection createConnection(final String dbName) {
        dbNames.add(dbName);
        LOG.debug("Creating connection to H2 database: {}", dbName);
        return connect(dbName, -1);
    }

    private Connection connect(String dbName, int closeDelay) {
        final String url = url(dbName, closeDelay);
        return execute(
                () -> DriverManager.getConnection(url, "sa", ""),
                e -> "Failed to open connection to H2 database"
        );
    }

    private String url(String dbName, int closeDelay) {
        return "jdbc:h2:mem:" + dbName + ";MODE=PostgreSQL;DB_CLOSE_DELAY=" + closeDelay;
    }

    @Override
    public void release() {
        dbNames.forEach(dbName -> execute(
                () -> {
                    LOG.debug("Cleaning up H2 database: {}", dbName);
                    connect(dbName, 0).close();
                    return null;
                },
                e -> "Failed to close H2 database"
        ));
    }
}
