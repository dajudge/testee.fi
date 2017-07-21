package com.dajudge.testee.flyway.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for using flyway on a test data source.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(FlywayContainer.class)
public @interface Flyway {
    /**
     * The {@link javax.sql.DataSource} to apply the flyway migrations on.
     *
     * @return the name of the data source.
     */
    String dataSource();

    /**
     * The flyway locations to consider for the data source.
     *
     * @return the flyway locations.
     */
    String[] locations() default {"db/migration"};
}
