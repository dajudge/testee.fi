package com.dajudge.testee.jdbc;

import com.dajudge.testee.spi.DataSourceFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declaration of a container managed data source.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(TestDataSourceContainer.class)
public @interface TestDataSource {
    /**
     * The JNDI name of the data source.
     *
     * @return JNDI name of the data source.
     */
    String name();

    /**
     * The {@link DataSourceFactory} to use.
     *
     * @return @link DataSourceFactory} to use.
     */
    Class<? extends DataSourceFactory> factory();
}
