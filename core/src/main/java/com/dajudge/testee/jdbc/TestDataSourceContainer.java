package com.dajudge.testee.jdbc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for {@link TestDataSource}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestDataSourceContainer {
    TestDataSource[] value();
}
