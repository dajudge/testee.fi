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
package fi.testee.jdbc;

import fi.testee.spi.TestDataSetupAccessorFactory;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.injection.spi.ResourceInjectionServices;

import javax.sql.DataSource;

public class JdbcTestDataSetupAccessorFactory implements TestDataSetupAccessorFactory {
    @Override
    public Object createTestDataSetupAccessor(final ServiceRegistry serviceRegistry) {
        return testDataSources(serviceRegistry.get(ResourceInjectionServices.class));
    }

    private static TestDataSources testDataSources(final ResourceInjectionServices resourceInjectionServices) {
        return mappedName -> (DataSource) resourceInjectionServices
                .registerResourceInjectionPoint(null, mappedName)
                .createResource()
                .getInstance();
    }
}
