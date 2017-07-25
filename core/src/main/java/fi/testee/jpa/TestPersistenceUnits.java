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
package fi.testee.jpa;

import fi.testee.jdbc.TestData;

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
