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

package org.eclipse.tractusx.edc.postgresql.migration.connector;

import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.postgresql.migration.DatabaseMigrationConfiguration;
import org.flywaydb.core.Flyway;

import java.util.Map;
import java.util.UUID;

import static org.eclipse.tractusx.edc.postgresql.migration.connector.ConnectorPostgresqlMigration.NAME;
import static org.flywaydb.core.api.MigrationVersion.LATEST;

@Extension(NAME)
public class ConnectorPostgresqlMigration implements ServiceExtension {

    public static final String NAME = "Connector Postgresql Schema Migration";

    @Configuration
    private DatabaseMigrationConfiguration configuration;

    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        if (configuration.enabled() && configuration.participantContextId() == null) {
            throw new EdcException("The participant context id has not been set, it is a mandatory setting now. You can " +
                    "use this UUID generated randomly for you: %s, or you can generate one by yourself. Please note that"
                            .formatted(UUID.randomUUID().toString()) +
                    " once set, it must never change. Depending on how you are configuring the Connector, set it on the " +
                    "`edc.participant.context.id` setting/system property or `EDC_PARTICIPANT_CONTEXT_ID` environment " +
                    "variable, then restart the Connector");
        }
    }

    @Override
    public void prepare() {
        if (!configuration.enabled()) {
            monitor.info("Migration for connector disabled");
            return;
        }

        var dataSource = configuration.getDataSource();

        var flyway = Flyway.configure()
                .baselineVersion("1.0.0")
                .baselineOnMigrate(true)
                .failOnMissingLocations(true)
                .dataSource(dataSource)
                .table("flyway_schema_history")
                .locations("classpath:migrations/connector")
                .defaultSchema(configuration.schema())
                .placeholders(Map.of("ParticipantContextId", configuration.participantContextId()))
                .target(LATEST)
                .load();

        var migrateResult = flyway.migrate();

        if (!migrateResult.success) {
            throw new EdcPersistenceException(
                    "Migrating connector failed: %s".formatted(String.join(", ", migrateResult.warnings))
            );
        }
    }

}
