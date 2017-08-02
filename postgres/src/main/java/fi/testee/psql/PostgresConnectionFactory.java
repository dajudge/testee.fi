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
package fi.testee.psql;

import fi.testee.exceptions.TestEEfiException;
import fi.testee.spi.ConnectionFactory;
import fi.testee.utils.AnnotationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

import static fi.testee.utils.ExpressionUtils.evalExpression;
import static fi.testee.utils.JdbcUtils.execute;
import static fi.testee.utils.JdbcUtils.query;
import static fi.testee.utils.JdbcUtils.update;
import static java.util.UUID.randomUUID;

/**
 * A {@link ConnectionFactory} for PostgreSQL.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class PostgresConnectionFactory implements ConnectionFactory {
    private static final Logger LOG = LoggerFactory.getLogger(PostgresConnectionFactory.class);
    private final ConcurrentHashMap<String, String> databases = new ConcurrentHashMap<>();

    @Resource(mappedName = "testeefi/setup/class")
    private Class<?> testSetupClass;
    private PostgresConfiguration config;

    @PostConstruct
    private void readConfigFromTestClass() {
        config = AnnotationUtils.firstByTypeHierarchy(testSetupClass, PostgresConfiguration.class);
        if (config == null) {
            throw new TestEEfiException(
                    "No @PostgresConfiguration found on "
                            + testSetupClass
                            + " which uses PostgresConnectionFactory"
            );
        }
    }

    @Override
    public Connection createConnection(final String dbName) {
        databases.computeIfAbsent(dbName, s -> newDatabase());
        return execute(
                () -> connect(databases.get(dbName)),
                e -> "Failed to open connection to PostgreSQL database"
        );
    }

    private Connection connect(
            final String dbName
    ) throws SQLException {
        return DriverManager.getConnection(
                urlFor(dbName),
                username(),
                password()
        );
    }

    private String username() {
        return evalExpression(config.username());
    }

    private String hostname() {
        return evalExpression(config.hostname());
    }

    private String password() {
        return evalExpression(config.password());
    }

    private String port() {
        return evalExpression(config.port());
    }

    private String urlFor(String dbName) {
        return "jdbc:postgresql://" + hostname() + ":" + port() + "/" + dbName;
    }

    private String newDatabase() {
        final String dbName = "testeefi_" + randomUUID().toString().replace("-", "");
        createDB(dbName);
        return dbName;
    }

    private void createDB(final String dbName) {
        execute(() -> {
            try (final Connection c = connect("postgres")) {
                update(c, "CREATE DATABASE " + dbName);
                update(c, "GRANT ALL PRIVILEGES ON DATABASE " + dbName + " TO " + username());
                return null;
            }
        }, e -> "Failed to create PostgreSQL database " + dbName);
    }

    private void dropDB(final String dbName) {
        execute(() -> {
            try (final Connection c = connect("postgres")) {
                update(c, "UPDATE pg_database SET datallowconn = 'false' WHERE datname = '" + dbName + "'");
                query(
                        c,
                        "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '" + dbName + "'",
                        row -> null
                );
                update(c, "DROP DATABASE " + dbName);
            }
            return null;
        }, e -> "Failed to drop PostgreSQL database " + dbName);
    }

    @PreDestroy
    public void release() {
        databases.values().forEach(dbName -> {
            try {
                dropDB(dbName);
            } catch (final RuntimeException e) {
                // Don't stop dropping DBs if one of them fails. Notify the user, though.
                LOG.error("Failed to cleanup PostgreSQL database {}", dbName, e);
            }
        });
    }
}
