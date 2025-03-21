/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.edc.tests.runtimes;

import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.util.io.Ports;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.DB_SCHEMA_NAME;

/**
 * JUnit extension that permits to spin up a PostgresSQL container
 */
public class PostgresExtension implements BeforeAllCallback, AfterAllCallback {

    private static final String POSTGRES_IMAGE_NAME = "postgres:16.4";
    private static final String USER = "postgres";
    private static final String PASSWORD = "password";
    private final PostgreSQLContainer<?> postgreSqlContainer;
    private final String[] databases;
    private final int exposedPort;

    public PostgresExtension(String... databases) {
        this.databases = databases;
        exposedPort = Ports.getFreePort();
        this.postgreSqlContainer = new PostgreSQLContainer<>(POSTGRES_IMAGE_NAME)
                .withUsername(USER)
                .withPassword(PASSWORD);
        postgreSqlContainer.setPortBindings(List.of("%d:5432".formatted(exposedPort)));
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        postgreSqlContainer.start();
        postgreSqlContainer.waitingFor(Wait.forHealthcheck());
        this.createDatabases();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        postgreSqlContainer.stop();
        postgreSqlContainer.close();
    }

    public Config getConfig(String databaseName) {
        var settings = getConfiguration(databaseName);

        return ConfigFactory.fromMap(settings);
    }

    public Map<String, String> getConfiguration(String databaseName) {
        var jdbcUrl = baseJdbcUrl() + databaseName.toLowerCase() + "?currentSchema=" + DB_SCHEMA_NAME;
        var group = "edc.datasource.default";

        return Map.of(
                group + ".url", jdbcUrl,
                group + ".user", USER,
                group + ".password", PASSWORD,
                "org.eclipse.tractusx.edc.postgresql.migration.schema", DB_SCHEMA_NAME
        );
    }

    private void createDatabases() {
        try (var connection = DriverManager.getConnection(baseJdbcUrl() + "postgres", postgreSqlContainer.getUsername(), postgreSqlContainer.getPassword())) {
            var command = stream(databases).map("create database %s;"::formatted).collect(joining("; "));
            connection.createStatement().execute(command);
        } catch (SQLException ignored) {
            // ignored
        }
    }

    private String baseJdbcUrl() {
        return format("jdbc:postgresql://%s:%s/", postgreSqlContainer.getHost(), exposedPort);
    }
}
