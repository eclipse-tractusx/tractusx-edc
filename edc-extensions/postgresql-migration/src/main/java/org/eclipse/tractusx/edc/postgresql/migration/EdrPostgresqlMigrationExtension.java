/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

public class EdrPostgresqlMigrationExtension extends AbstractPostgresqlMigrationExtension {
    private static final String NAME_SUBSYSTEM = "edr";

    private static final String DATASOURCE_SETTING_NAME = "edc.datasource.edr.name";

    protected String getDataSourceNameConfigurationKey() {
        return DATASOURCE_SETTING_NAME;
    }

    protected String getSubsystemName() {
        return NAME_SUBSYSTEM;
    }
}
