/*
 * Copyright (c) 2025 Think-it
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

import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.postgresql.migration.DatabaseMigrationConfiguration;
import org.eclipse.tractusx.edc.postgresql.migration.DatabaseSchemaMigration;
import org.flywaydb.core.Flyway;

import static org.eclipse.tractusx.edc.postgresql.migration.controlplane.ControlPlanePostgresqlMigration.NAME;
import static org.flywaydb.core.api.MigrationVersion.LATEST;

@Extension(NAME)
public class ControlPlanePostgresqlMigration implements ServiceExtension {

    public static final String NAME = "Control-plane Postgresql Schema Migration";

    @Configuration
    private DatabaseMigrationConfiguration configuration;

    @Inject
    private Monitor monitor;
    private DatabaseSchemaMigration databaseSchemaMigration;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        databaseSchemaMigration = new DatabaseSchemaMigration(context.getConfig());
    }

    @Override
    public void prepare() {
        if (!configuration.enabled()) {
            monitor.info("Migration for control plane disabled");
            return;
        }

        var dataSource = databaseSchemaMigration.getDataSource();

        var flyway = Flyway.configure()
                .baselineVersion("1.0.0")
                .baselineOnMigrate(true)
                .failOnMissingLocations(true)
                .dataSource(dataSource)
                .table("flyway_schema_history_control_plane")
                .locations("classpath:migrations/control-plane")
                .defaultSchema(configuration.schema())
                .target(LATEST)
                .load();

        if (databaseSchemaMigration.schemaContainsFlywayTable(configuration.schema())) {
            flyway.baseline();
        }

        var migrateResult = flyway.migrate();

        if (!migrateResult.success) {
            throw new EdcPersistenceException(
                    "Migrating control-plane failed: %s".formatted(String.join(", ", migrateResult.warnings))
            );
        }
    }

}
