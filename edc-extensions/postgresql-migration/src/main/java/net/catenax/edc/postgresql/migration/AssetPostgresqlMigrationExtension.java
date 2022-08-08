/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial implementation
 *
 */

package net.catenax.edc.postgresql.migration;

import org.eclipse.dataspaceconnector.sql.assetindex.ConfigurationKeys;

public class AssetPostgresqlMigrationExtension extends AbstractPostgresqlMigrationExtension {
  private static final String NAME_SUBSYSTEM = "asset";

  protected String getDataSourceNameConfigurationKey() {
    return ConfigurationKeys.DATASOURCE_SETTING_NAME;
  }

  protected String getSubsystemName() {
    return NAME_SUBSYSTEM;
  }
}
