package fi.testee.spi;

import java.sql.Connection;

/**
 * Interface for creating {@link Connection data sources} for test setups.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public interface ConnectionFactory {

    /**
     * Creates a new {@link Connection} for a given database name.
     *
     * @param dbName the database name.
     * @return a {@link Connection} to the database.
     */
    Connection createConnection(String dbName);

    void release();
}
