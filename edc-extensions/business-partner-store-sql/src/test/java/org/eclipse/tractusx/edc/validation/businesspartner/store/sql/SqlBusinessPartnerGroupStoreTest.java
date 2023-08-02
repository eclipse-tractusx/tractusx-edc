/*
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.validation.businesspartner.store.sql;

import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.BusinessPartnerGroupStore;
import org.eclipse.tractusx.edc.validation.businesspartner.store.BusinessPartnerGroupStoreTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlBusinessPartnerGroupStoreTest extends BusinessPartnerGroupStoreTestBase {
    private final TypeManager typeManager = new TypeManager();
    private final BusinessPartnerGroupStatements statements = new PostgresBusinessPartnerGroupStatements();
    private SqlBusinessPartnerGroupStore store;

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) throws IOException {
        store = new SqlBusinessPartnerGroupStore(extension.getDataSourceRegistry(), extension.getDatasourceName(), extension.getTransactionContext(), typeManager.getMapper(), queryExecutor, statements);
        var schema = Files.readString(Paths.get("./docs/schema.sql"));
        extension.runQuery(schema);
    }


    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getTable() + " CASCADE");
    }

    protected BusinessPartnerGroupStore getStore() {
        return store;
    }
}