package net.catenax.edc.postgresql.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Objects;
import java.util.Properties;
import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.eclipse.dataspaceconnector.sql.ConnectionFactory;

class DriverManagerConnectionFactory implements ConnectionFactory {
  private final String jdbcUrl;
  private final Properties properties;

  public DriverManagerConnectionFactory(final String jdbcUrl, final Properties properties) {
    this.jdbcUrl = Objects.requireNonNull(jdbcUrl);
    this.properties = Objects.requireNonNull(properties);
  }

  @Override
  public Connection create() {
    try {
      return DriverManager.getConnection(jdbcUrl, properties);
    } catch (Exception exception) {
      throw new EdcPersistenceException(exception.getMessage(), exception);
    }
  }
}
