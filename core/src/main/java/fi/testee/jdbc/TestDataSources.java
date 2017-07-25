package fi.testee.jdbc;

import javax.sql.DataSource;

/**
 * Access to test {@link javax.sql.DataSource data sources} from {@link TestData} annotated methods.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public interface TestDataSources {
    /**
     * Returns a data source given its name.
     *
     * @param dataSourceName the name of the data source.
     * @return the data source.
     */
    DataSource get(String dataSourceName);
}
