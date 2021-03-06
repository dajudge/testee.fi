/*
 * Copyright (C) 2017 Alex Stockinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fi.testee.liqiubase.annotations;

import fi.testee.liqiubase.ClassLoaderResourceAccessorFactory;
import fi.testee.liqiubase.ResourceAccessorFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for using liquibase on a test data source.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Liquibase {
    /**
     * The {@link javax.sql.DataSource} to apply the changelogs on.
     *
     * @return the name of the data source.
     */
    String dataSource();

    /**
     * The contexts to use for the update.
     * <p>
     *
     * @return the contexts to use for the update.
     * @see <a href="http://www.liquibase.org/documentation/contexts.html">Liquibase documentation on contexts.</a>
     */
    String contexts() default "";

    /**
     * The {@link ResourceAccessorFactory} type to use for creating the {@link liquibase.resource.ResourceAccessor} to
     * pass to liquibase. A new instance of the factory will be instantiated, it thus needs a default or parameterless
     * constructor.
     *
     * @return the {@link ResourceAccessorFactory} type.
     */
    Class<? extends ResourceAccessorFactory> resourceAccessorFactory() default ClassLoaderResourceAccessorFactory.class;

    /**
     * Specifies the changelog file to use.
     * @return the changelog file to use.
     */
    String changeLogFile();

}
