package net.catenax.edc.postgresql.migration;

import java.util.Objects;
import java.util.Properties;
import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.configuration.Config;
import org.eclipse.dataspaceconnector.sql.datasource.ConnectionFactoryDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.MigrateResult;

abstract class AbstractPostgresqlMigrationExtension implements ServiceExtension {
  protected abstract String getDataSourceNameConfigurationKey();

  protected abstract String getSubsystemName();

  private static final String EDC_DATASOURCE_PREFIX = "edc.datasource";
  private static final String MIGRATION_LOCATION_BASE =
      String.format(
          "classpath:%s",
          AbstractPostgresqlMigrationExtension.class.getPackageName().replaceAll("\\.", "/"));

  @Override
  public void initialize(final ServiceExtensionContext context) {
    final String subSystemName = Objects.requireNonNull(getSubsystemName());

    final String dataSourceName =
        context.getConfig().getString(getDataSourceNameConfigurationKey(), null);
    if (dataSourceName == null) {
      return;
    }

    boolean enabled =
        context
            .getConfig()
            .getBoolean(
                String.format("net.catenax.edc.postgresql.migration.%s.enabled", subSystemName),
                true);

    if (!enabled) {
      return;
    }

    Config datasourceConfiguration =
        context.getConfig(String.join(".", EDC_DATASOURCE_PREFIX, dataSourceName));

    final String jdbcUrl = Objects.requireNonNull(datasourceConfiguration.getString("url"));
    final Properties jdbcProperties = new Properties();
    jdbcProperties.putAll(datasourceConfiguration.getRelativeEntries());

    final DriverManagerConnectionFactory driverManagerConnectionFactory =
        new DriverManagerConnectionFactory(jdbcUrl, jdbcProperties);
    final ConnectionFactoryDataSource dataSource =
        new ConnectionFactoryDataSource(driverManagerConnectionFactory);

    final String schemaHistoryTableName = getSchemaHistoryTableName(subSystemName);
    final String migrationsLocation = getMigrationsLocation();

    final Flyway flyway =
        Flyway.configure()
            .baselineVersion(MigrationVersion.fromVersion("0.0.0"))
            .failOnMissingLocations(true)
            .dataSource(dataSource)
            .table(schemaHistoryTableName)
            .locations(migrationsLocation)
            .load();

    flyway.baseline();

    final MigrateResult migrateResult = flyway.migrate();

    if (!migrateResult.success) {
      throw new EdcPersistenceException(
          String.format(
              "Migrating DataSource %s for subsystem %s failed: %s",
              dataSourceName, subSystemName, String.join(", ", migrateResult.warnings)));
    }
  }

  private String getMigrationsLocation() {
    return String.join("/", MIGRATION_LOCATION_BASE, getSubsystemName());
  }

  private String getSchemaHistoryTableName(final String subSystemName) {
    return String.format("flyway_schema_history_%s", subSystemName);
  }
}
