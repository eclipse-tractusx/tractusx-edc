/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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

package org.eclipse.tractusx.edc.edr.index.sql.lock;


import org.eclipse.edc.edr.spi.store.EndpointDataReferenceEntryIndex;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.configuration.DataSourceName;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.tractusx.edc.edr.spi.index.lock.EndpointDataReferenceLock;

@Provides({ EndpointDataReferenceEntryIndex.class, EndpointDataReferenceLock.class })
@Extension(value = "Database-level EDR Lock extension (PostgreSQL)")
public class SqlEdrLockExtension implements ServiceExtension {


    @Deprecated(since = "0.8.1")
    public static final String DATASOURCE_SETTING_NAME = "edc.datasource.edr.name";

    @Setting(value = "The datasource to be used", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE)
    public static final String DATASOURCE_NAME = "edc.sql.store.edr.datasource";

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private TransactionContext transactionContext;


    @Inject
    private QueryExecutor queryExecutor;

    @Inject
    private TypeManager typeManager;


    @Override
    public void initialize(ServiceExtensionContext context) {
        var dataSourceName = DataSourceName.getDataSourceName(DATASOURCE_NAME, DATASOURCE_SETTING_NAME, context.getConfig(), context.getMonitor());

        var statements = new PostgresEdrLockStatements();
        var sqlStore = new SqlEdrLock(dataSourceRegistry, dataSourceName, transactionContext, typeManager.getMapper(), queryExecutor, statements);

        context.registerService(EndpointDataReferenceLock.class, sqlStore);
    }


}
