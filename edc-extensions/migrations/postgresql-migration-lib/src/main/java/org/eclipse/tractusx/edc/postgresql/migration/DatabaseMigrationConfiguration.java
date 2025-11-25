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

package org.eclipse.tractusx.edc.postgresql.migration;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;

@Settings
public record DatabaseMigrationConfiguration(

        @Setting(
                key = "tx.edc.postgresql.migration.enabled",
                description = "Enable/disables data-plane schema migration",
                defaultValue = DEFAULT_MIGRATION_ENABLED_TEMPLATE)
        boolean enabled,

        @Setting(
                key = "tx.edc.postgresql.migration.schema",
                description = "Schema used for the migration",
                defaultValue = DEFAULT_MIGRATION_SCHEMA
        )
        String schema
) {
    private static final String DEFAULT_MIGRATION_ENABLED_TEMPLATE = "true";
    private static final String DEFAULT_MIGRATION_SCHEMA = "public";
}
