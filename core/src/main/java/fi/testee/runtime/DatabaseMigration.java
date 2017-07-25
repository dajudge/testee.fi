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
package fi.testee.runtime;

import fi.testee.spi.DataSourceMigrator;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.injection.spi.ResourceInjectionServices;

import javax.sql.DataSource;
import java.util.Set;
import java.util.function.Function;

/**
 * Control of database migration.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public final class DatabaseMigration {
    private DatabaseMigration() {
    }

    static void migrateDataSources(
            final Class<?> setupClass,
            final Set<DataSourceMigrator> migrators,
            final ServiceRegistry serviceRegistry
    ) {
        final ResourceInjectionServices injectionServices = serviceRegistry.get(ResourceInjectionServices.class);
        final Function<String, DataSource> dataSourceAccessor = mappedName -> (DataSource) injectionServices
                .registerResourceInjectionPoint(null, mappedName)
                .createResource()
                .getInstance();
        migrators.forEach(it -> it.migrate(setupClass, dataSourceAccessor));
    }
}
