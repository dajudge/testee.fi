package com.dajudge.testee;

import com.dajudge.testee.flyway.annotation.Flyway;
import com.dajudge.testee.h2.H2PostgresConnectionFactory;
import com.dajudge.testee.jdbc.TestDataSource;
import com.dajudge.testee.junit4.TestEE;
import com.dajudge.testee.utils.JdbcUtils;
import org.junit.runner.RunWith;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@TestDataSource(
        name = "jdbc/test",
        factory = H2PostgresConnectionFactory.class
)
@Flyway(
        dataSource = "jdbc/test"
)
@RunWith(TestEE.class)
public abstract class AbstractBaseDatabaseTest {

    @PersistenceContext(unitName = "testUnit")
    protected EntityManager em;
    @Resource(mappedName = "jdbc/test")
    protected DataSource ds;

    protected static void insertJdbc(DataSource ds, int id, String stringValue) throws SQLException {
        try (final Connection c = ds.getConnection()) {
            JdbcUtils.update(c, "INSERT INTO test(id, stringValue) VALUES(?,?)", id, stringValue);
        }
    }
}
