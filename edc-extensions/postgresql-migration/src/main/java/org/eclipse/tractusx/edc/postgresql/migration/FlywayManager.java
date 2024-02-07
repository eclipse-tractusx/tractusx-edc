/********************************************************************************
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
 ********************************************************************************/

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
