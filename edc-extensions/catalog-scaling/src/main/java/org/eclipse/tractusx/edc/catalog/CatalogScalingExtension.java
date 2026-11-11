/*
 * Copyright (c) 2026 Volkswagen AG
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

package org.eclipse.tractusx.edc.catalog;

import javax.sql.DataSource;
import org.eclipse.edc.connector.controlplane.catalog.spi.DatasetResolver;
import org.eclipse.edc.connector.controlplane.catalog.spi.DistributionResolver;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Objects;

import static java.lang.String.format;

/**
 * Provides a {@link DatasetResolver} implementation which utilizes Postgres to query for matching
 * assets and contract definitions, moving the load of generating a catalog to the database. It
 * can therefore handle larger numbers of assets and contract definitions.
 */
@Extension(CatalogScalingExtension.NAME)
@Provides(DatasetResolver.class)
public class CatalogScalingExtension implements ServiceExtension {

    public static final String NAME = "Catalog Scaling Extension";
    private static final String SQL_SCRIPT_PATH = "/sql/script.sql";
    private static final int DEFAULT_CONTRACT_DEFINITION_COPY_BATCH_SIZE = 5000;

    // same @Setting annotations as used by SQL Asset Index and Contract Definition Store extension respectively
    @Setting(description = "The datasource to be used for storing assets.", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE, key = "edc.sql.store.asset.datasource")
    private String dataSourceName;
    @Setting(description = "The datasource to be used for storing contract definitions.", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE, key = "edc.sql.store.contractdefinition.datasource")
    private String contractDefinitionDataSourceName;

    @Setting(key = "edc.sql.store.contractdefinition.copy.batch.size", defaultValue = "" + DEFAULT_CONTRACT_DEFINITION_COPY_BATCH_SIZE,
            description = "The batch size to be used when copying authorized contract definitions to a temporary Postgres table.")
    private int contractDefinitionCopyBatchSize;

    // must be injected here to ensure this extension is loaded after the upstream catalog extension, so that it can override upstream's DatasetResolver
    @Inject
    private DatasetResolver toOverride;

    @Inject
    private ContractDefinitionStore contractDefinitionStore;
    @Inject
    private PolicyDefinitionStore policyDefinitionStore;
    @Inject
    private DistributionResolver distributionResolver;
    @Inject
    private PolicyEngine policyEngine;
    @Inject
    private DataSourceRegistry dataSourceRegistry;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private TypeManager typeManager;
    @Inject
    private QueryExecutor queryExecutor;

    private boolean enabled;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        if (dataSourceName == null || !dataSourceName.equals(contractDefinitionDataSourceName)) {
            context.getMonitor().warning(format("Different data sources configured for asset and contract definition: %s, %s. "
                    + "Unable to load catalog scaling improvements, as these rely on assets and contract definitions residing in the same data source.",
                    dataSourceName, contractDefinitionDataSourceName));
            return;
        }

        enabled = true;

        var contractDefinitionResolver = new ScalingContractDefinitionResolver(contractDefinitionStore, policyEngine, policyDefinitionStore);
        var queryManager = new CatalogRequestQueryManager(dataSourceRegistry, dataSourceName, transactionContext,
                typeManager.getMapper(), queryExecutor, new WrappedAssetStatements(), contractDefinitionCopyBatchSize);

        context.registerService(DatasetResolver.class, new ScalingDatasetResolver(contractDefinitionResolver,
                queryManager, policyDefinitionStore, distributionResolver,
                () -> Objects.requireNonNull(dataSourceRegistry.resolve(dataSourceName), format("DataSource %s could not be resolved", dataSourceName)),
                transactionContext));
    }

    @Override
    public void start() {
        if (!enabled) {
            return;
        }

        var dataSource = Objects.requireNonNull(dataSourceRegistry.resolve(dataSourceName),
                format("DataSource %s could not be resolved", dataSourceName));
        var sql = loadSqlScript();
        executeSqlScript(dataSource, sql);
    }

    private String loadSqlScript() {
        try (var inputStream = getClass().getResourceAsStream(SQL_SCRIPT_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException(format("SQL script resource %s was not found", SQL_SCRIPT_PATH));
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(format("Failed to read SQL script resource %s", SQL_SCRIPT_PATH), e);
        }
    }

    private void executeSqlScript(DataSource dataSource, String sql) {
        transactionContext.execute(() -> {
            try (var connection = dataSource.getConnection();
                var statement = connection.createStatement()) {
                statement.execute(sql);
            } catch (SQLException e) {
                throw new IllegalStateException(format("Failed to execute SQL script %s using datasource %s",
                                SQL_SCRIPT_PATH, dataSourceName), e);
            }
        });
    }
}
