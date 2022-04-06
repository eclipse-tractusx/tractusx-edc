package net.catenax.edc.postgresql.migration;

import org.eclipse.dataspaceconnector.spi.EdcSetting;

public class PolicyPostgresqlMigrationExtension extends AbstractPostgresqlMigrationExtension {
  private static final String NAME_SUBSYSTEM = "policy";

  @EdcSetting private static final String DATASOURCE_SETTING_NAME = "edc.datasource.policy.name";

  protected String getDataSourceNameConfigurationKey() {
    return DATASOURCE_SETTING_NAME;
  }

  protected String getSubsystemName() {
    return NAME_SUBSYSTEM;
  }
}
