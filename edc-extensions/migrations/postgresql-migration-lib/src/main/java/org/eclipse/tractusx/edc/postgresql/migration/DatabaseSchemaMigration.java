/*
 * Copyright (c) 2025 Think-it GmbH
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.edc.postgresql.migration;

import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.sql.DriverManagerConnectionFactory;
import org.eclipse.edc.sql.datasource.ConnectionFactoryDataSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Properties;
import javax.sql.DataSource;

/**
 * Utility class that provides common functions for database schema migrations
 */
public class DatabaseSchemaMigration {

    private final Config config;
    private DataSource dataSource;

    public DatabaseSchemaMigration(Config config) {
        this.config = config;
    }

    /**
     * Lazily instance and return DataSource to be passed to Flyway for schema migrations
     *
     * @return the dataSource.
     */
    public DataSource getDataSource() {
        if (dataSource == null) {
            var datasourceConfig = config.getConfig("edc.datasource.default");

            var url = datasourceConfig.getString("url");
            var jdbcProperties = new Properties();
            jdbcProperties.putAll(datasourceConfig.getRelativeEntries());
            var driverManagerConnectionFactory = new DriverManagerConnectionFactory();
            dataSource = new ConnectionFactoryDataSource(driverManagerConnectionFactory, url, jdbcProperties);
        }
        return dataSource;
    }

    /**
     * Checks if in the schema there's already a flyway schema history table.
     * This information can be used to decide to set "baseline" or not before migration.
     *
     * @param schemaName the schema name.
     * @return true if schema contains a flyway schema history table, false otherwise
     */
    public boolean schemaContainsFlywayTable(String schemaName) {
        try (Connection conn = getDataSource().getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            var tables = metaData.getTables(null, schemaName, null, new String[]{"TABLE"});

            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                if (tableName.startsWith("flyway_schema_history")) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            throw new EdcPersistenceException("Failed to check schema existence", e);
        }
    }
}
