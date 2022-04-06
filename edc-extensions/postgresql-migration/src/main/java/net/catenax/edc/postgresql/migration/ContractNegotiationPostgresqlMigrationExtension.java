package net.catenax.edc.postgresql.migration;

import org.eclipse.dataspaceconnector.spi.EdcSetting;

public class ContractNegotiationPostgresqlMigrationExtension
    extends AbstractPostgresqlMigrationExtension {
  private static final String NAME_SUBSYSTEM = "contractnegotiation";

  @EdcSetting
  private static final String DATASOURCE_SETTING_NAME = "edc.datasource.contractnegotiation.name";

  protected String getDataSourceNameConfigurationKey() {
    return DATASOURCE_SETTING_NAME;
  }

  protected String getSubsystemName() {
    return NAME_SUBSYSTEM;
  }
}
