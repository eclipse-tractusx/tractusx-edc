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

import org.eclipse.edc.edr.store.index.SqlEndpointDataReferenceEntryIndex;
import org.eclipse.edc.edr.store.index.sql.schema.EndpointDataReferenceEntryStatements;
import org.eclipse.edc.edr.store.index.sql.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.eclipse.tractusx.edc.edr.spi.index.lock.EndpointDataReferenceLock;
import org.eclipse.tractusx.edc.edr.spi.testfixtures.index.lock.EndpointDataReferenceLockBaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

@PostgresqlIntegrationTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlEdrLockTest extends EndpointDataReferenceLockBaseTest {

    private SqlEdrLock edrLock;
    private final TypeManager typeManager = new JacksonTypeManager();
    private final PostgresEdrLockStatements statements = new PostgresEdrLockStatements();
    private final EndpointDataReferenceEntryStatements edrEntryStatements = new PostgresDialectStatements();

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) throws IOException {
        edrLock = new SqlEdrLock(extension.getDataSourceRegistry(), extension.getDatasourceName(), extension.getTransactionContext(), typeManager.getMapper(), queryExecutor, statements);
        var entryIndexStore = new SqlEndpointDataReferenceEntryIndex(extension.getDataSourceRegistry(), extension.getDatasourceName(), extension.getTransactionContext(), typeManager.getMapper(), edrEntryStatements, queryExecutor);
        var schema = TestUtils.getResourceFileContentAsString("schema.sql");
        extension.runQuery(schema);

        entryIndexStore.save(edrEntry("mock", ACQUIRE_LOCK_TP));

    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getEdrEntryTableName() + " CASCADE");
    }

    @Override
    protected EndpointDataReferenceLock getStore() {
        return edrLock;
    }
}