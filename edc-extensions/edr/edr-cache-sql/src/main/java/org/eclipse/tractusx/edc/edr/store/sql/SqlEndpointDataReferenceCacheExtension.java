/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.edr.store.sql;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.tractusx.edc.edr.spi.store.EndpointDataReferenceCache;
import org.eclipse.tractusx.edc.edr.store.sql.schema.EdrStatements;
import org.eclipse.tractusx.edc.edr.store.sql.schema.postgres.PostgresEdrStatements;

import java.time.Clock;

@Extension(value = SqlEndpointDataReferenceCacheExtension.NAME)
public class SqlEndpointDataReferenceCacheExtension implements ServiceExtension {

    public static final String NAME = "SQL EDR cache store";

    @Setting(required = true, value = "Datasource name for EDR Cache SQL store", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE)
    public static final String DATASOURCE_SETTING_NAME = "edc.datasource.edr.name";

    private static final String DEFAULT_EDR_VAULT_PATH = "";
    @Setting(value = "Directory/Path where to store EDRs in the vault for vaults that supports hierarchical structuring.", defaultValue = DEFAULT_EDR_VAULT_PATH)
    public static final String EDC_EDR_VAULT_PATH = "edc.edr.vault.path";

    @Inject
    private DataSourceRegistry dataSourceRegistry;
    @Inject
    private TransactionContext transactionContext;
    @Inject(required = false)
    private EdrStatements statements;
    @Inject
    private TypeManager typeManager;
    @Inject
    private Clock clock;
    @Inject
    private Vault vault;

    @Inject
    private QueryExecutor queryExecutor;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public EndpointDataReferenceCache edrCache(ServiceExtensionContext context) {
        var dataSourceName = context.getConfig().getString(DATASOURCE_SETTING_NAME, DataSourceRegistry.DEFAULT_DATASOURCE);
        var vaultDirectory = context.getConfig().getString(EDC_EDR_VAULT_PATH, DEFAULT_EDR_VAULT_PATH);
        return new SqlEndpointDataReferenceCache(dataSourceRegistry, dataSourceName, transactionContext, getStatementImpl(),
                typeManager.getMapper(), vault, vaultDirectory, clock, queryExecutor, context.getConnectorId());
    }

    private EdrStatements getStatementImpl() {
        return statements == null ? new PostgresEdrStatements() : statements;
    }
}
