package com.dajudge.testee;

import com.dajudge.testee.flyway.annotation.Flyway;
import com.dajudge.testee.h2.H2PostgresConnectionFactory;
import com.dajudge.testee.jdbc.TestData;
import com.dajudge.testee.jdbc.TestDataSource;
import com.dajudge.testee.jdbc.TestDataSources;
import com.dajudge.testee.jpa.TestPersistenceUnits;
import com.dajudge.testee.junit4.TestEE;
import com.dajudge.testee.model.TestEntity;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@TestDataSource(
        name = "jdbc/test",
        factory = H2PostgresConnectionFactory.class
)
@Flyway(
        dataSource = "jdbc/test"
)
@RunWith(TestEE.class)
public class PersistenceITest {
    @TestData
    public static void setupTestData(
            final TestDataSources dataSource,
            final TestPersistenceUnits persistenceUnits
    ) throws SQLException {
        try (
                final Connection c = dataSource.get("jdbc/test").getConnection();
                final PreparedStatement s = c.prepareStatement("INSERT INTO test(id, stringValue) VALUES(?,?)")
        ) {
            s.setLong(1, 1);
            s.setString(2, "value1");
            s.executeUpdate();
        }
        EntityManager em = persistenceUnits.get("testUnit");
        em.persist(new TestEntity(2, "value2"));
    }

    @PersistenceContext(unitName = "testUnit")
    private EntityManager em;
    @Resource(mappedName = "jdbc/test")
    private DataSource ds;

    @Test
    public void jdbc_setup_worked() throws SQLException {
        try (
                final Connection c = ds.getConnection();
                final PreparedStatement s = c.prepareStatement("SELECT id, stringValue FROM test WHERE id=?")
        ) {
            s.setLong(1, 1);
            try (final ResultSet rs = s.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(1, rs.getLong(1));
                assertEquals("value1", rs.getString(2));
            }
        }
    }

    @Test
    public void jpa_setup_worked() {
        final TestEntity entity = em.find(TestEntity.class, 2L);
        assertEquals("value2", entity.getStringValue());
    }
}
