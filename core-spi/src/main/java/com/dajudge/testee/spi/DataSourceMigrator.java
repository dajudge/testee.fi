package com.dajudge.testee.spi;

import javax.sql.DataSource;
import java.util.function.Function;

/**
 * Interface for libraries used to migrate databases.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public interface DataSourceMigrator {
    void migrate(Class<?> testSetupClass, Function<String, DataSource> dataSourceProvider);
}
