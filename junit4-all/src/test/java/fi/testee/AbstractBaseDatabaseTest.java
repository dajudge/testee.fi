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
package fi.testee;

import fi.testee.flyway.annotation.Flyway;
import fi.testee.h2.H2PostgresConnectionFactory;
import fi.testee.jdbc.TestDataSource;
import fi.testee.junit4.TestEEfi;
import fi.testee.liqiubase.annotations.Liquibase;
import fi.testee.psql.PostgresConfiguration;
import fi.testee.psql.PostgresConnectionFactory;
import fi.testee.utils.JdbcUtils;
import org.junit.runner.RunWith;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Collection;

import static java.util.Arrays.asList;

@TestDataSource(name = AbstractBaseDatabaseTest.H2_DATASOURCE, factory = H2PostgresConnectionFactory.class)
@TestDataSource(name = AbstractBaseDatabaseTest.PSQL_DATASOURCE, factory = PostgresConnectionFactory.class)
@Liquibase(dataSource = AbstractBaseDatabaseTest.H2_DATASOURCE)
@Flyway(dataSource = AbstractBaseDatabaseTest.PSQL_DATASOURCE)
@PostgresConfiguration(
        hostname = "${System.getenv('TESTEEFI_PSQL_HOSTNAME') ?: 'localhost'}",
        port = "${System.getenv('TESTEEFI_PSQL_PORT') ?: '5432'}",
        username = "${System.getenv('TESTEEFI_PSQL_USER') ?: 'postgres'}",
        password = "${System.getenv('TESTEEFI_PSQL_PASSWORD') ?: 'postgres'}"
)
@RunWith(TestEEfi.class)
public abstract class AbstractBaseDatabaseTest {
    static final String H2_DATASOURCE = "jdbc/h2";
    static final String PSQL_DATASOURCE = "jdbc/psql";
    static final Collection<String> DATASOURCES = asList(
            H2_DATASOURCE,
            PSQL_DATASOURCE
    );
    static final Collection<String> UNITS = asList("h2", "psql");

    @PersistenceContext(unitName = "h2")
    private EntityManager h2Unit;
    @PersistenceContext(unitName = "psql")
    private EntityManager psqlUnit;
    @Resource(mappedName = H2_DATASOURCE)
    private DataSource h2;
    @Resource(mappedName = PSQL_DATASOURCE)
    private DataSource psql;

    protected Collection<DataSource> dataSources() {
        return asList(h2, psql);
    }

    protected Collection<EntityManager> units() {
        return asList(h2Unit, psqlUnit);
    }

    protected static void insertJdbc(DataSource ds, int id, String stringValue) {
        JdbcUtils.execute(() -> {
            try (final Connection c = ds.getConnection()) {
                JdbcUtils.update(c, "INSERT INTO test(id, stringValue) VALUES(?,?)", id, stringValue);
            }
            return null;
        }, e -> "Failed to INSERT");

    }
}
