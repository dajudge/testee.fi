package com.dajudge.testee.maven;

import com.dajudge.testee.flyway.annotation.Flyway;
import com.dajudge.testee.h2.H2PostgresConnectionFactory;
import com.dajudge.testee.jdbc.TestData;
import com.dajudge.testee.jdbc.TestDataSource;
import com.dajudge.testee.jpa.TestPersistenceUnits;
import com.dajudge.testee.junit4.TestEE;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@TestDataSource(name = "testds", factory = H2PostgresConnectionFactory.class)
@Flyway(dataSource = "testds")
@RunWith(TestEE.class)
public class MavenTest {

    @PersistenceContext(unitName = "test")
    private EntityManager entityManager;

    @TestData
    public static void setupTestData(final TestPersistenceUnits persistenceUnits) {
        persistenceUnits.get("test").persist(new TestEntity(1, "lolcats"));
    }

    @Test
    public void runs() {
        final TestEntity entity = entityManager.find(TestEntity.class, 1L);
        assertNotNull(entity);
        assertEquals("lolcats", entity.getStringValue());
    }
}
