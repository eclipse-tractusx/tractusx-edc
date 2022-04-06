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
