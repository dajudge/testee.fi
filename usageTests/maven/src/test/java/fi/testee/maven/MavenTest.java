package fi.testee.maven;

import fi.testee.flyway.annotation.Flyway;
import fi.testee.h2.H2PostgresConnectionFactory;
import fi.testee.jdbc.TestData;
import fi.testee.jdbc.TestDataSource;
import fi.testee.jpa.TestPersistenceUnits;
import fi.testee.junit4.TestEEfi;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@TestDataSource(name = "testds", factory = H2PostgresConnectionFactory.class)
@Flyway(dataSource = "testds")
@RunWith(TestEEfi.class)
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
