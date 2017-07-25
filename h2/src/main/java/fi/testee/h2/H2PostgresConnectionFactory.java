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
package fi.testee.h2;

import fi.testee.spi.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashSet;
import java.util.Set;

import static fi.testee.utils.JdbcUtils.execute;

/**
 * Factory for a {@link java.sql.Connection} backed by an in memory H2 database with the PostgreSQL dialect.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
@Singleton
public class H2PostgresConnectionFactory implements ConnectionFactory {
    private static final Logger LOG = LoggerFactory.getLogger(H2PostgresConnectionFactory.class);

    private Set<String> dbNames = new HashSet<>();

    @Override
    public Connection createConnection(final String dbName) {
        dbNames.add(dbName);
        LOG.debug("Creating connection to H2 database: {}", dbName);
        return connect(dbName, -1);
    }

    private Connection connect(String dbName, int closeDelay) {
        final String url = url(dbName, closeDelay);
        return execute(
                () -> DriverManager.getConnection(url, "sa", ""),
                e -> "Failed to open connection to H2 database"
        );
    }

    private String url(String dbName, int closeDelay) {
        return "jdbc:h2:mem:" + dbName + ";MODE=PostgreSQL;DB_CLOSE_DELAY=" + closeDelay;
    }

    @Override
    public void release() {
        dbNames.forEach(dbName -> execute(
                () -> {
                    LOG.debug("Cleaning up H2 database: {}", dbName);
                    connect(dbName, 0).close();
                    return null;
                },
                e -> "Failed to close H2 database"
        ));
    }
}
