/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
 ********************************************************************************/

package org.eclipse.tractusx.edc.lifecycle;

import org.eclipse.edc.connector.core.vault.InMemoryVault;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.InjectionContainer;
import org.eclipse.edc.sql.testfixtures.PostgresqlLocalInstance;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.DB_SCHEMA_NAME;
import static org.mockito.Mockito.mock;

public class PgParticipantRuntime extends ParticipantRuntime {

    private static final String POSTGRES_IMAGE_NAME = "postgres:14.2";
    private static final String USER = "postgres";
    private static final String PASSWORD = "password";
    private final String dbName;
    public PostgreSQLContainer<?> postgreSqlContainer;

    public PgParticipantRuntime(String moduleName, String runtimeName, String bpn, Map<String, String> properties) {
        super(moduleName, runtimeName, bpn, properties);
        this.dbName = runtimeName.toLowerCase();
        mockVault();

        postgreSqlContainer = new PostgreSQLContainer<>(POSTGRES_IMAGE_NAME)
                .withLabel("runtime", dbName)
                .withExposedPorts(5432)
                .withUsername(USER)
                .withPassword(PASSWORD)
                .withDatabaseName(dbName);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        postgreSqlContainer.start();
        var config = postgresqlConfiguration(dbName);
        config.forEach(System::setProperty);
        super.beforeAll(context);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        super.afterAll(context);
        postgreSqlContainer.stop();
        postgreSqlContainer.close();
    }

    @Override
    protected void bootExtensions(ServiceExtensionContext context, List<InjectionContainer<ServiceExtension>> serviceExtensions) {
        PostgresqlLocalInstance helper = new PostgresqlLocalInstance(postgreSqlContainer.getUsername(), postgreSqlContainer.getPassword(), baseJdbcUrl(), postgreSqlContainer.getDatabaseName());
        helper.createDatabase();
        super.bootExtensions(context, serviceExtensions);
    }

    public Map<String, String> postgresqlConfiguration(String name) {
        var jdbcUrl = jdbcUrl(name);
        return new HashMap<>() {
            {
                Stream.of("asset", "contractdefinition", "contractnegotiation", "policy", "transferprocess", "edr", "bpn", "policy-monitor")
                        .forEach(context -> {
                            var group = "edc.datasource." + context;
                            put(group + ".name", context);
                            put(group + ".url", jdbcUrl);
                            put(group + ".user", USER);
                            put(group + ".password", PASSWORD);
                        });
                // use non-default schema name to test usage of non-default schema
                put("org.eclipse.tractusx.edc.postgresql.migration.schema", DB_SCHEMA_NAME);
            }
        };
    }

    public String jdbcUrl(String name) {
        return baseJdbcUrl() + name + "?currentSchema=" + DB_SCHEMA_NAME;
    }

    public String baseJdbcUrl() {
        return format("jdbc:postgresql://%s:%s/", postgreSqlContainer.getHost(), postgreSqlContainer.getFirstMappedPort());
    }

    protected void mockVault() {
        this.registerServiceMock(Vault.class, new InMemoryVaultOverride(mock(Monitor.class)));
    }

    private static class InMemoryVaultOverride extends InMemoryVault {

        InMemoryVaultOverride(Monitor monitor) {
            super(monitor);
        }

        @Override
        public Result<Void> deleteSecret(String s) {
            super.deleteSecret(s);
            return Result.success();
        }
    }

}
