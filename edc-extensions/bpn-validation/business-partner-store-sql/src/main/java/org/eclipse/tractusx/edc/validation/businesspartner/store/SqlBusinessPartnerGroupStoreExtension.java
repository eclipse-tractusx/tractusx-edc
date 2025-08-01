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

package org.eclipse.tractusx.edc.validation.businesspartner.store;

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
import org.eclipse.tractusx.edc.validation.businesspartner.spi.store.BusinessPartnerStore;
import org.eclipse.tractusx.edc.validation.businesspartner.store.sql.BusinessPartnerGroupStatements;
import org.eclipse.tractusx.edc.validation.businesspartner.store.sql.PostgresBusinessPartnerGroupStatements;
import org.eclipse.tractusx.edc.validation.businesspartner.store.sql.SqlBusinessPartnerStore;

@Extension("Registers an SQL implementation for the BusinessPartnerGroupStore")
public class SqlBusinessPartnerGroupStoreExtension implements ServiceExtension {

    private static final String DEFAULT_DATASOURCE_NAME = "bpn";
    @Deprecated(since = "0.8.0")
    @Setting(value = "Datasource name for the SQL BusinessPartnerGroup store", defaultValue = DEFAULT_DATASOURCE_NAME)
    private static final String DATASOURCE_SETTING_NAME = "edc.datasource.bpn.name";
    @Setting(value = "The datasource to be used", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE)
    public static final String DATASOURCE_NAME = "edc.sql.store.bpn.datasource";

    private static final String NAME = "SQL Business Partner Store";
    @Inject
    private DataSourceRegistry dataSourceRegistry;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private TypeManager typeManager;
    @Inject
    private QueryExecutor queryExecutor;
    @Inject(required = false)
    private BusinessPartnerGroupStatements statements;

    @Provider
    public BusinessPartnerStore sqlStore(ServiceExtensionContext context) {
        var dataSourceName = context.getConfig().getString(DATASOURCE_NAME, DataSourceRegistry.DEFAULT_DATASOURCE);
        return new SqlBusinessPartnerStore(dataSourceRegistry, dataSourceName, transactionContext, typeManager.getMapper(), queryExecutor, getStatements());
    }

    @Override
    public String name() {
        return NAME;
    }

    private BusinessPartnerGroupStatements getStatements() {
        return statements == null ? new PostgresBusinessPartnerGroupStatements() : statements;
    }
}
