/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.edc.postgresql.migration;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.sql.DriverManagerConnectionFactory;
import org.eclipse.edc.sql.datasource.ConnectionFactoryDataSource;

import java.util.Objects;
import java.util.Properties;

import static org.flywaydb.core.api.MigrationVersion.LATEST;

abstract class AbstractPostgresqlMigrationExtension implements ServiceExtension {

    private static final String EDC_DATASOURCE_PREFIX = "edc.datasource";

    private static final String DEFAULT_MIGRATION_ENABLED_TEMPLATE = "true";
    @Setting(value = "Enable/disables subsystem schema migration", defaultValue = DEFAULT_MIGRATION_ENABLED_TEMPLATE, type = "boolean")
    private static final String MIGRATION_ENABLED_TEMPLATE = "org.eclipse.tractusx.edc.postgresql.migration.%s.enabled";

    private static final String DEFAULT_MIGRATION_SCHEMA = "public";
    @Setting(value = "Schema used for the migration", defaultValue = DEFAULT_MIGRATION_SCHEMA)
    private static final String MIGRATION_SCHEMA = "org.eclipse.tractusx.edc.postgresql.migration.schema";

    @Override
    public String name() {
        return "Postgresql schema migration for subsystem " + getSubsystemName();
    }

    @Override
    public void initialize(final ServiceExtensionContext context) {
        var config = context.getConfig();

        var subSystemName = Objects.requireNonNull(getSubsystemName());
        var enabled = config.getBoolean(MIGRATION_ENABLED_TEMPLATE.formatted(subSystemName), Boolean.valueOf(DEFAULT_MIGRATION_ENABLED_TEMPLATE));

        if (!enabled) {
            return;
        }

        var configGroup = "%s.%s".formatted(EDC_DATASOURCE_PREFIX, subSystemName);
        var datasourceConfig = config.getConfig(configGroup);

        var dataSourceName = datasourceConfig.getString("name", null);
        if (dataSourceName == null) {
            context.getMonitor().warning("No 'name' setting in group %s found, no schema migrations will run for subsystem %s"
                    .formatted(configGroup, subSystemName));
            return;
        }

        var jdbcUrl = datasourceConfig.getString("url");
        var jdbcProperties = new Properties();
        jdbcProperties.putAll(datasourceConfig.getRelativeEntries());

        var driverManagerConnectionFactory = new DriverManagerConnectionFactory();
        var dataSource = new ConnectionFactoryDataSource(driverManagerConnectionFactory, jdbcUrl, jdbcProperties);

        var defaultSchema = config.getString(MIGRATION_SCHEMA, DEFAULT_MIGRATION_SCHEMA);
        var migrateResult = FlywayManager.migrate(dataSource, subSystemName, defaultSchema, LATEST);

        if (!migrateResult.success) {
            throw new EdcPersistenceException(
                    String.format(
                            "Migrating DataSource %s for subsystem %s failed: %s",
                            dataSourceName, subSystemName, String.join(", ", migrateResult.warnings)));
        }
    }

    protected abstract String getSubsystemName();

}
