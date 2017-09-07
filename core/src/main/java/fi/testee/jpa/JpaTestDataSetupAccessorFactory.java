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

import fi.testee.services.JpaInjectionServicesImpl;
import fi.testee.spi.TestDataSetupAccessorFactory;
import org.jboss.weld.bootstrap.api.ServiceRegistry;

import javax.persistence.EntityManager;

public class JpaTestDataSetupAccessorFactory implements TestDataSetupAccessorFactory {
    @Override
    public Object createTestDataSetupAccessor(final ServiceRegistry serviceRegistry) {
        // TODO it's somewhat hacky to require the actual implementation instead of the interface here
        return testPersistenceUnits(serviceRegistry.get(JpaInjectionServicesImpl.class));
    }

    private static TestPersistenceUnits testPersistenceUnits(final JpaInjectionServicesImpl jpaInjectionServices) {
        return unitName -> (EntityManager) jpaInjectionServices
                .registerPersistenceContextInjectionPoint(unitName)
                .createResource()
                .getInstance();
    }
}
