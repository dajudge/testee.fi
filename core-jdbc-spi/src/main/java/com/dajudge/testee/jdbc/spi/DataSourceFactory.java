package com.dajudge.testee.jdbc.spi;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Interface for creating {@link DataSource data sources} for test setups.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public interface DataSourceFactory {

    DataSource create();

    void shutdown();
}
