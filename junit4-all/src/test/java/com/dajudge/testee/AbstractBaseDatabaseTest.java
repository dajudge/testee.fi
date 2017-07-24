package com.dajudge.testee;

import com.dajudge.testee.flyway.annotation.Flyway;
import com.dajudge.testee.h2.H2PostgresConnectionFactory;
import com.dajudge.testee.jdbc.TestDataSource;
import com.dajudge.testee.junit4.TestEE;
import com.dajudge.testee.liqiubase.annotations.Liquibase;
import com.dajudge.testee.psql.PostgresConfiguration;
import com.dajudge.testee.psql.PostgresConnectionFactory;
import com.dajudge.testee.utils.JdbcUtils;
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
        hostname = "${System.getenv()['TESTEE_PSQL_HOSTNAME'] ?: 'localhost'}",
        port = "${System.getenv('TESTEE_PSQL_PORT') ?: '5432'}",
        username = "${System.getenv('TESTEE_PSQL_USER') ?: 'postgres'}",
        password = "${System.getenv('TESTEE_PSQL_PASSWORD') ?: 'postgres'}"
)
@RunWith(TestEE.class)
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
