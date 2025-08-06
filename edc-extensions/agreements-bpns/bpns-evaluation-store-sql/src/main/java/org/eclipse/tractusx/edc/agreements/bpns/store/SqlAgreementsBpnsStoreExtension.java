/********************************************************************************
 * Copyright (c) 2025 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.agreements.bpns.store;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.tractusx.edc.agreements.bpns.store.sql.PostgresAgreementsBpnsStatements;
import org.eclipse.tractusx.edc.agreements.bpns.store.sql.SqlAgreementsBpnsStatements;
import org.eclipse.tractusx.edc.agreements.bpns.store.sql.SqlAgreementsBpnsStore;

@Extension(value = SqlAgreementsBpnsStoreExtension.NAME)
public class SqlAgreementsBpnsStoreExtension implements ServiceExtension {

    protected static final String NAME = "SQL Agreement BPNs Store.";

    @Setting(description = "Datasource name for the SQL AgreementsBpns store", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE)
    private static final String DATASOURCE_SETTING_NAME = "edc.sql.store.contractdefinition.datasource";

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private TransactionContext transactionContext;

    @Inject
    private TypeManager typeManager;

    @Inject
    private QueryExecutor queryExecutor;

    @Inject(required = false)
    private SqlAgreementsBpnsStatements statements;

    @Provider
    public SqlAgreementsBpnsStore sqlStore(ServiceExtensionContext context) {
        var dataSourceName = context.getConfig().getString(DATASOURCE_SETTING_NAME, DataSourceRegistry.DEFAULT_DATASOURCE);
        return new SqlAgreementsBpnsStore(dataSourceRegistry, dataSourceName, transactionContext,
                typeManager.getMapper(), queryExecutor, getStatements());
    }

    @Override
    public String name() {
        return NAME;
    }

    private SqlAgreementsBpnsStatements getStatements() {
        return statements == null ? new PostgresAgreementsBpnsStatements() : statements;
    }
}
