/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import static java.lang.String.format;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.DB_SCHEMA_NAME;

/**
 * Instantiates the Postgres docker container and configures the runtime accordingly
 */
public class PgRuntimeExtension extends ParticipantRuntimeExtension {
    private static final String POSTGRES_IMAGE_NAME = "postgres:16.4";
    private static final String USER = "postgres";
    private static final String PASSWORD = "password";
    private final PostgreSQLContainer<?> postgreSqlContainer;
    private final String dbName;

    public PgRuntimeExtension(String moduleName, String runtimeName, String bpn, Map<String, String> properties) {
        super(moduleName, runtimeName, bpn, properties, null);
        this.dbName = runtimeName.toLowerCase();
        postgreSqlContainer = new PostgreSQLContainer<>(POSTGRES_IMAGE_NAME)
                .withLabel("runtime", dbName)
                .withExposedPorts(5432)
                .withUsername(USER)
                .withPassword(PASSWORD)
                .withDatabaseName(dbName);
    }

    @Override
    public void beforeAll(ExtensionContext context) {

        postgreSqlContainer.start();
        postgreSqlContainer.waitingFor(Wait.forHealthcheck());
        var config = postgresqlConfiguration(dbName);
        config.forEach(System::setProperty);
        createDatabase();
        super.beforeAll(context);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        super.afterAll(context);
        postgreSqlContainer.stop();
        postgreSqlContainer.close();
    }

    private Map<String, String> postgresqlConfiguration(String name) {
        var jdbcUrl = baseJdbcUrl() + name + "?currentSchema=" + DB_SCHEMA_NAME;
        var group = "edc.datasource.default";

        return Map.of(
                group + ".url", jdbcUrl,
                group + ".user", USER,
                group + ".password", PASSWORD,
                "org.eclipse.tractusx.edc.postgresql.migration.schema", DB_SCHEMA_NAME
        );
    }

    private void createDatabase() {
        try (var connection = DriverManager.getConnection(baseJdbcUrl() + "postgres", postgreSqlContainer.getUsername(), postgreSqlContainer.getPassword())) {
            connection.createStatement().execute(String.format("create database %s;", postgreSqlContainer.getDatabaseName()));
        } catch (SQLException ignored) {
            // ignored
        }
    }

    private String baseJdbcUrl() {
        return format("jdbc:postgresql://%s:%s/", postgreSqlContainer.getHost(), postgreSqlContainer.getFirstMappedPort());
    }
}
