package org.eclipse.tractusx.edc.postgresql.migration;

public class CpAdapterPostgresqlMigrationExtension extends AbstractPostgresqlMigrationExtension {
  private static final String NAME_SUBSYSTEM = "cpadapter";
  private static final String DATASOURCE_SETTING_NAME = "edc.datasource.cpadapter.name";

  @Override
  protected String getDataSourceNameConfigurationKey() {
    return DATASOURCE_SETTING_NAME;
  }

  @Override
  protected String getSubsystemName() {
    return NAME_SUBSYSTEM;
  }
}
