package net.catenax.edc.postgresql.migration;

import org.eclipse.dataspaceconnector.sql.asset.index.ConfigurationKeys;

public class AssetPostgresqlMigrationExtension extends AbstractPostgresqlMigrationExtension {
  private static final String NAME_SUBSYSTEM = "asset";

  protected String getDataSourceNameConfigurationKey() {
    return ConfigurationKeys.DATASOURCE_SETTING_NAME;
  }

  protected String getSubsystemName() {
    return NAME_SUBSYSTEM;
  }
}
