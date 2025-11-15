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

package org.eclipse.tractusx.edc.agreements.bpns.store.sql;

import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.eclipse.tractusx.edc.agreements.bpns.spi.store.AgreementsBpnsStore;
import org.eclipse.tractusx.edc.agreements.bpns.store.AgreementsBpnsStoreTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.eclipse.tractusx.edc.tests.testcontainer.PostgresContainerManager.getPostgresTestContainerName;

@PostgresqlIntegrationTest
class SqlAgreementsBpnsStoreTest extends AgreementsBpnsStoreTestBase {
    private final TypeManager typeManager = new JacksonTypeManager();
    private final SqlAgreementsBpnsStatements statements = new PostgresAgreementsBpnsStatements();
    private SqlAgreementsBpnsStore store;

    @RegisterExtension
    static PostgresqlStoreSetupExtension extension =
            new PostgresqlStoreSetupExtension(getPostgresTestContainerName());

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) throws IOException {
        store = new SqlAgreementsBpnsStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager.getMapper(), queryExecutor, statements);

        var schema = Files.readString(Paths.get("./docs/schema.sql"));
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getTable() + " CASCADE");
    }

    @Override
    protected AgreementsBpnsStore getStore() {
        return store;
    }
}