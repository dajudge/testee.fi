package com.dajudge.testee.jpa;

import com.dajudge.testee.jdbc.TestData;

import javax.persistence.EntityManager;

/**
 * Access to test {@link javax.persistence.EntityManager persistence units} from {@link TestData} annotated methods.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public interface TestPersistenceUnits {
    /**
     * Returns a {@link EntityManager} given the name of a persistence unit.
     *
     * @param unitName the name of the persistence unit.
     * @return the entity manager.
     */
    EntityManager get(final String unitName);
}
