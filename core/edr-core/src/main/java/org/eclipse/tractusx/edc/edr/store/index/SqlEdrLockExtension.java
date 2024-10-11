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

package org.eclipse.tractusx.edc.edr.store.index;


import org.eclipse.edc.edr.spi.store.EndpointDataReferenceEntryIndex;
import org.eclipse.edc.edr.store.index.sql.schema.postgres.PostgresDialectStatements;
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

@Provides({ EndpointDataReferenceEntryIndex.class, EndpointDataReferenceLock.class })
@Extension(value = "Database-level EDR Lock extension (PostgreSQL)")
public class SqlEdrLockExtension implements ServiceExtension {


    public static final String DEFAULT_DATASOURCE_NAME = "edr";

    @Deprecated(since = "0.8.1")
    public static final String DATASOURCE_SETTING_NAME = "edc.datasource.edr.name";

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
        var dataSourceName = context.getConfig().getString(DATASOURCE_SETTING_NAME, DEFAULT_DATASOURCE_NAME);

        var statements = new PostgresDialectStatements();
        var sqlStore = new SqlEdrLock(dataSourceRegistry, dataSourceName, transactionContext, typeManager.getMapper(), queryExecutor, statements);

        context.registerService(EndpointDataReferenceLock.class, sqlStore);
    }


}
