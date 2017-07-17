package com.dajudge.testee;

import com.dajudge.testee.exceptions.TesteeException;
import com.dajudge.testee.jdbc.TestData;
import com.dajudge.testee.jdbc.TestDataSources;
import com.dajudge.testee.jpa.TestPersistenceUnits;
import com.dajudge.testee.model.TestEntity;
import com.dajudge.testee.utils.JdbcUtils;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        try (final Connection c = ds.getConnection()) {
            final List<Map<String, Object>> result = JdbcUtils.query(
                    c,
                    "SELECT id, stringValue FROM test WHERE id=?",
                    JdbcUtils::mapRowMapper,
                    1
            );
            assertEquals(1, result.size());
            final Map<String, Object> row = result.get(0);
            assertEquals(1L, (long)row.get("ID"));
            assertEquals("value1", row.get("STRINGVALUE"));
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
