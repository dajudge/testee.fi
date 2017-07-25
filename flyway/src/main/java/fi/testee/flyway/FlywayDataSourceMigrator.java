package fi.testee.flyway;

import fi.testee.spi.DataSourceMigrator;
import fi.testee.utils.AnnotationUtils;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.util.List;
import java.util.function.Function;

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
        final List<fi.testee.flyway.annotation.Flyway> annotations =
                AnnotationUtils.collectAnnotations(testSetupClass, fi.testee.flyway.annotation.Flyway.class);
        if (annotations == null) {
            return;
        }
        for (final fi.testee.flyway.annotation.Flyway annotation : annotations) {
            final org.flywaydb.core.Flyway flyway = new Flyway();
            final DataSource ds = dataSourceProvider.apply(annotation.dataSource());
            flyway.setDataSource(ds);
            flyway.setLocations(annotation.locations());
            flyway.migrate();
        }
    }
}
