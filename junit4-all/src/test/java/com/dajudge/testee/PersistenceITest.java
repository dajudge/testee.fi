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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PersistenceITest extends AbstractBaseDatabaseTest {
    @TestData
    public static void setupTestData(
            final TestDataSources dataSource,
            final TestPersistenceUnits persistenceUnits
    ) throws SQLException {
        DATASOURCES.forEach(ds -> insertJdbc(dataSource.get(ds), 1, "value1"));
        UNITS.forEach(unit -> persistenceUnits.get(unit).persist(new TestEntity(2, "value2")));
    }

    @Test
    public void jdbc_setup_worked() {
        dataSources().forEach(ds -> {
            try (final Connection c = ds.getConnection()) {
                final List<Map<String, Object>> result = JdbcUtils.query(
                        c,
                        "SELECT id, stringValue FROM test WHERE id=?",
                        JdbcUtils::mapRowMapper,
                        1
                );
                assertEquals(1, result.size());
                final Map<String, Object> row = result.get(0);
                assertTrue("ID not contained in " + row, row.containsKey("ID"));
                assertTrue("STRINGVALUE not contained in " + row, row.containsKey("STRINGVALUE"));
                assertEquals(1L, (long) row.get("ID"));
                assertEquals("value1", row.get("STRINGVALUE"));
            } catch (final SQLException e) {
                throw new AssertionError("Failed to execute JDBC", e);
            }
        });
    }

    @Test
    public void cannot_access_bean_transaction() {
        units().forEach(unit -> {
            try {
                unit.getTransaction();
                fail("Must not allow to access JPA transaction");
            } catch (final TesteeException e) {
                // this is the expected behavior
            }
        });
    }

    @Test
    public void cannot_close_entity_manager() {
        units().forEach(unit -> {
            try {
                unit.close();
                fail("Must not allow to close EntityManager");
            } catch (final TesteeException e) {
                // this is the expected behavior
            }
        });
    }

    @Test
    public void jpa_setup_worked() {
        units().forEach(unit -> {
            final TestEntity entity = unit.find(TestEntity.class, 2L);
            assertNotNull(entity);
            assertEquals("value2", entity.getStringValue());
        });
    }
}
