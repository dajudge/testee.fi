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
package fi.testee.jdbc;

import fi.testee.spi.ConnectionFactory;

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
     * The {@link ConnectionFactory} to use.
     *
     * @return {@link ConnectionFactory} to use.
     */
    Class<? extends ConnectionFactory> factory();
}
