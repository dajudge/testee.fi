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

import fi.testee.exceptions.TestEEfiException;
import fi.testee.jdbc.TestData;
import fi.testee.jdbc.TestDataSources;
import fi.testee.jpa.TestPersistenceUnits;
import fi.testee.model.TestEntity;
import fi.testee.utils.JdbcUtils;
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
            final TestDataSources dataSources,
            final TestPersistenceUnits persistenceUnits
    ) throws SQLException {
        DATASOURCES.forEach(ds -> insertJdbc(dataSources.get(ds), 1, "value1"));
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
            } catch (final TestEEfiException e) {
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
            } catch (final TestEEfiException e) {
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
