/*
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.postgresql.migration;

import org.eclipse.edc.connector.store.sql.assetindex.ConfigurationKeys;

public class BusinessGroupPostgresMigrationExtension extends AbstractPostgresqlMigrationExtension {
    private static final String NAME = "businessgroup";


    @Override
    protected String getDataSourceNameConfigurationKey() {
        return ConfigurationKeys.DATASOURCE_SETTING_NAME;
    }

    @Override
    protected String getSubsystemName() {
        return NAME;
    }
}
