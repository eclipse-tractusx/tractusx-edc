/*
 * Copyright (c) 2025 Think-it GmbH
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

package org.eclipse.tractusx.edc.postgresql.migration.controlplane;

import org.eclipse.edc.boot.system.DefaultServiceExtensionContext;
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.tractusx.edc.postgresql.migration.AbstractPostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.AgreementBpnsPostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.AgreementRetirementPostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.AssetPostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.BusinessGroupPostgresMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.ContractDefinitionPostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.ContractNegotiationPostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.DataPlaneInstancePostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.EdrIndexPostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.FederatedCatalogCacheMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.JtiValidationPostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.PolicyMonitorPostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.PolicyPostgresqlMigrationExtension;
import org.eclipse.tractusx.edc.postgresql.migration.TransferProcessPostgresqlMigrationExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.tests.testcontainer.PostgresContainerManager.getPostgresTestContainerName;
import static org.flywaydb.core.api.CoreMigrationType.BASELINE;
import static org.flywaydb.core.api.CoreMigrationType.SQL;
import static org.mockito.Mockito.mock;

@Testcontainers
@ExtendWith(DependencyInjectionExtension.class)
public class ControlPlanePostgresqlMigrationTest {

    @Container
    private final PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>(getPostgresTestContainerName());

    @Test
    void shouldRunOnEmptyDatabase(ObjectFactory objectFactory) {
        var context = contextWithSettings(Map.of(
                "edc.datasource.default.url", postgresql.getJdbcUrl(),
                "edc.datasource.default.user", postgresql.getUsername(),
                "edc.datasource.default.password", postgresql.getPassword()
        ));

        var newMigrations = objectFactory.constructInstance(ControlPlanePostgresqlMigration.class);
        newMigrations.initialize(context);
        newMigrations.prepare();

        try (var connection = createDataSource().getConnection()) {
            var callableStatement = connection.prepareCall("select * from flyway_schema_history_control_plane;");
            callableStatement.execute();
            var resultSet = callableStatement.getResultSet();
            resultSet.next();
            assertThat(resultSet.getString("version")).isEqualTo("1.0.0");
            assertThat(resultSet.getString("type")).isEqualTo(SQL.toString());
            assertThat(testMigrationHasBeenApplied(connection)).isEqualTo(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldUseMergedMigrationAsBaseline_whenSchemaAlreadyBuilt(ObjectFactory objectFactory) {
        var context = contextWithSettings(Map.of(
                "edc.datasource.default.url", postgresql.getJdbcUrl(),
                "edc.datasource.default.user", postgresql.getUsername(),
                "edc.datasource.default.password", postgresql.getPassword()
        ));
        var currentMigrations = List.of(
                objectFactory.constructInstance(AssetPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(AssetPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(ContractDefinitionPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(ContractNegotiationPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(DataPlaneInstancePostgresqlMigrationExtension.class),
                objectFactory.constructInstance(EdrIndexPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(FederatedCatalogCacheMigrationExtension.class),
                objectFactory.constructInstance(FederatedCatalogCacheMigrationExtension.class),
                objectFactory.constructInstance(JtiValidationPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(PolicyMonitorPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(PolicyPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(TransferProcessPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(AgreementBpnsPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(AgreementRetirementPostgresqlMigrationExtension.class),
                objectFactory.constructInstance(BusinessGroupPostgresMigrationExtension.class)
        );
        currentMigrations.forEach(e -> e.initialize(context));
        currentMigrations.forEach(AbstractPostgresqlMigrationExtension::prepare);

        var newMigrations = objectFactory.constructInstance(ControlPlanePostgresqlMigration.class);
        newMigrations.initialize(context);
        newMigrations.prepare();

        try (var connection = createDataSource().getConnection()) {
            var callableStatement = connection.prepareCall("select * from flyway_schema_history_control_plane;");
            callableStatement.execute();
            var resultSet = callableStatement.getResultSet();
            resultSet.next();
            assertThat(resultSet.getString("version")).isEqualTo("1.0.0");
            assertThat(resultSet.getString("type")).isEqualTo(BASELINE.toString());
            assertThat(testMigrationHasBeenApplied(connection)).isEqualTo(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private DataSource createDataSource() {
        var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(postgresql.getJdbcUrl());
        dataSource.setUser(postgresql.getUsername());
        dataSource.setPassword(postgresql.getPassword());
        return dataSource;
    }

    private @NotNull DefaultServiceExtensionContext contextWithSettings(Map<@NotNull String, @NotNull String> settings) {
        var config = ConfigFactory.fromMap(settings);
        return new DefaultServiceExtensionContext(mock(), config);
    }

    private boolean testMigrationHasBeenApplied(Connection connection) throws SQLException {
        return connection.prepareCall("select dummy_column from edc_policydefinitions;").execute();
    }
}
