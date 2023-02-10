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

import org.eclipse.dataspaceconnector.spi.EdcSetting;

public class TransferProcessPostgresqlMigrationExtension
    extends AbstractPostgresqlMigrationExtension {
  private static final String NAME_SUBSYSTEM = "transferprocess";

  @EdcSetting
  private static final String DATASOURCE_SETTING_NAME = "edc.datasource.transferprocess.name";

  protected String getDataSourceNameConfigurationKey() {
    return DATASOURCE_SETTING_NAME;
  }

  protected String getSubsystemName() {
    return NAME_SUBSYSTEM;
  }
}
