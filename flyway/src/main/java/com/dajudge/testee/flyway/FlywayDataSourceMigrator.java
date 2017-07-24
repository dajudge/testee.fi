package com.dajudge.testee.flyway;

import com.dajudge.testee.spi.DataSourceMigrator;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.util.List;
import java.util.function.Function;

import static com.dajudge.testee.utils.AnnotationUtils.collectAnnotations;

/**
 * Implementation of a {@link DataSourceMigrator}
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class FlywayDataSourceMigrator implements DataSourceMigrator {

    @Override
    public void migrate(
            final Class<?> testSetupClass,
            final Function<String, DataSource> dataSourceProvider
    ) {
        final List<com.dajudge.testee.flyway.annotation.Flyway> annotations =
                collectAnnotations(testSetupClass, com.dajudge.testee.flyway.annotation.Flyway.class);
        if (annotations == null) {
            return;
        }
        for (final com.dajudge.testee.flyway.annotation.Flyway annotation : annotations) {
            final org.flywaydb.core.Flyway flyway = new Flyway();
            final DataSource ds = dataSourceProvider.apply(annotation.dataSource());
            flyway.setDataSource(ds);
            flyway.setLocations(annotation.locations());
            flyway.migrate();
        }
    }
}
