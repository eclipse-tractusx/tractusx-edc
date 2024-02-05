/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.postgresql.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.MigrateResult;

import javax.sql.DataSource;


public class FlywayManager {

    private static final String MIGRATION_LOCATION_BASE =
            String.format("classpath:%s", FlywayManager.class.getPackageName().replace(".", "/"));

    public static MigrateResult migrate(DataSource dataSource, String subSystemName, String defaultSchema, MigrationVersion target) {
        var flyway =
                Flyway.configure()
                        .baselineVersion(MigrationVersion.fromVersion("0.0.0"))
                        .failOnMissingLocations(true)
                        .dataSource(dataSource)
                        .table("flyway_schema_history_%s".formatted(subSystemName))
                        .locations("%s/%s".formatted(MIGRATION_LOCATION_BASE, subSystemName))
                        .defaultSchema(defaultSchema)
                        .target(target)
                        .load();

        flyway.baseline();

        return flyway.migrate();
    }
}
