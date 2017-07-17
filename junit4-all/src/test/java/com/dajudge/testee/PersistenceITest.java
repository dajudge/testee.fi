package com.dajudge.testee;

import com.dajudge.testee.exceptions.TesteeException;
import com.dajudge.testee.jdbc.TestData;
import com.dajudge.testee.jdbc.TestDataSources;
import com.dajudge.testee.jpa.TestPersistenceUnits;
import com.dajudge.testee.model.TestEntity;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PersistenceITest extends AbstractBaseDatabaseTest {
    @TestData
    public static void setupTestData(
            final TestDataSources dataSource,
            final TestPersistenceUnits persistenceUnits
    ) throws SQLException {
        insertJdbc(dataSource.get("jdbc/test"), 1, "value1");
        persistenceUnits.get("testUnit").persist(new TestEntity(2, "value2"));
    }

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

    @Test(expected = TesteeException.class)
    public void cannot_access_bean_transaction() {
        em.getTransaction();
    }

    @Test(expected = TesteeException.class)
    public void cannot_close_entity_manager() {
        em.close();
    }

    @Test
    public void jpa_setup_worked() {
        final TestEntity entity = em.find(TestEntity.class, 2L);
        assertNotNull(entity);
        assertEquals("value2", entity.getStringValue());
    }
}
