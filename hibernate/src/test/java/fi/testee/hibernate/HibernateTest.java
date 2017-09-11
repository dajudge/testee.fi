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
package fi.testee.hibernate;

import fi.testee.h2.H2PostgresConnectionFactory;
import fi.testee.jdbc.TestData;
import fi.testee.jdbc.TestDataSource;
import fi.testee.jpa.TestPersistenceUnits;
import fi.testee.junit4.TestEEfi;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@RunWith(TestEEfi.class)
@TestDataSource(name = "jdbc/test", factory = H2PostgresConnectionFactory.class)
public class HibernateTest {
    @PersistenceContext(unitName = "hibernate")
    private EntityManager em;

    @TestData
    public static void setupTestData(final TestPersistenceUnits units) {
        units.get("hibernate").persist(new TestEntity(1));
    }

    @Test
    public void runTest() {
        em.find(TestEntity.class, 1);
    }
}
