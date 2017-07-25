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
package fi.testee.liqiubase;

import fi.testee.exceptions.TestEEfiException;
import fi.testee.liqiubase.annotations.Liquibase;
import fi.testee.spi.DataSourceMigrator;
import fi.testee.utils.AnnotationUtils;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ResourceAccessor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * {@link DataSourceMigrator} based on {@link liquibase.Liquibase}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class LiquibaseDataSourceMigrator implements DataSourceMigrator {
    @Override
    public void migrate(final Class<?> testSetupClass, final Function<String, DataSource> dataSourceProvider) {
        AnnotationUtils.collectAnnotations(testSetupClass, Liquibase.class).stream().forEach(it -> liquibase(it, dataSourceProvider));
    }

    private void liquibase(
            final Liquibase annotation,
            final Function<String, DataSource> dataSourceProvider
    ) {
        try (final Connection c = dataSourceProvider.apply(annotation.dataSource()).getConnection()) {
            final String contexts = isBlank(annotation.contexts()) ? null : annotation.contexts();
            final ResourceAccessor resourceAccessor = annotation.resourceAccessorFactory().newInstance().create();
            applyChangelog(c, contexts, resourceAccessor);
        } catch (final LiquibaseException | SQLException e) {
            throw new TestEEfiException("Failed to apply Liquibase changelog", e);
        } catch (final IllegalAccessException | InstantiationException e) {
            throw new TestEEfiException("Failed to instantiate ResourceAccessorFactory", e);
        }
    }

    private void applyChangelog(
            final Connection c,
            final String contexts,
            final ResourceAccessor resourceAccessor
    ) throws LiquibaseException {
        liquibase.Liquibase liquibase = null;
        final Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(c));
        liquibase = new liquibase.Liquibase("liquibase/h2.xml", resourceAccessor, database);
        liquibase.update(contexts);
    }
}
