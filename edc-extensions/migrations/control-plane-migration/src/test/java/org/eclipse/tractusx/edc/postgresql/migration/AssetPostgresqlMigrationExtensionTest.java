/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
 ********************************************************************************/

package org.eclipse.tractusx.edc.postgresql.migration;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.store.sql.assetindex.SqlAssetIndex;
import org.eclipse.edc.connector.controlplane.store.sql.assetindex.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.tests.testcontainer.PostgresContainerManager.getPostgresTestContainerName;

@PostgresqlIntegrationTest
class AssetPostgresqlMigrationExtensionTest {
    private SqlAssetIndex store;

    @RegisterExtension
    static PostgresqlStoreSetupExtension extension =
            new PostgresqlStoreSetupExtension(getPostgresTestContainerName());

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        store = new SqlAssetIndex(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), new JacksonTypeManager().getMapper(), new PostgresDialectStatements(),
                queryExecutor);
    }

    // bugfix https://github.com/eclipse-tractusx/tractusx-edc/issues/1003
    @Test
    void version006shouldTransformPropertiesListToMap(PostgresqlStoreSetupExtension extension) {
        var dataSource = extension.getDataSourceRegistry().resolve(extension.getDatasourceName());
        FlywayManager.migrate(dataSource, "asset", "public", MigrationVersion.fromVersion("0.0.5"));

        insertAsset(extension, "1");
        insertAsset(extension, "2");

        FlywayManager.migrate(dataSource, "asset", "public", MigrationVersion.fromVersion("0.0.6"));

        var result = store.findById("1");

        assertThat(result).isNotNull();
        assertThat(result.getProperties()).containsExactlyInAnyOrderEntriesOf(
                Map.of(Asset.PROPERTY_ID, "1", "key", "value1", "anotherKey", "anotherValue1"));
        assertThat(result.getPrivateProperties()).containsExactlyInAnyOrderEntriesOf(
                Map.of("privateKey", "privateValue1", "anotherPrivateKey", "anotherPrivateValue1"));
    }

    private void insertAsset(PostgresqlStoreSetupExtension extension, String id) {
        var propertiesArray = "[ %s, %s ]".formatted(propertyJsonMap("key", "value" + id), propertyJsonMap("anotherKey", "anotherValue" + id));
        var privatePropertiesArray = "[ %s, %s ]".formatted(propertyJsonMap("privateKey", "privateValue" + id), propertyJsonMap("anotherPrivateKey", "anotherPrivateValue" + id));

        extension.runQuery(("insert into edc_asset (asset_id, properties, private_properties, data_address) " +
                "values ('%s', '%s'::json, '%s'::json, '{\"type\":\"type\"}'::json)")
                .formatted(id, propertiesArray, privatePropertiesArray));
    }

    private String propertyJsonMap(String key, String value) {
        return "{\"property_name\" : \"%s\", \"property_value\" : \"%s\", \"property_type\" : \"java.lang.String\"}".formatted(key, value);
    }

}
