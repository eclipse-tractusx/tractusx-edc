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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlBusinessPartnerGroupStoreTest {
    private final TypeManager typeManager = new TypeManager();
    private final BusinessPartnerGroupStatements statements = new PostgresBusinessPartnerGroupStatements();
    private SqlBusinessPartnerGroupStore store;

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) throws IOException {
        store = new SqlBusinessPartnerGroupStore(extension.getDataSourceRegistry(), extension.getDatasourceName(), extension.getTransactionContext(), typeManager.getMapper(), queryExecutor, statements);
        var schema = Files.readString(Paths.get("./docs/schema.sql"));
        extension.runQuery(schema);
    }

    @Test
    void resolveForBpn() {
        store.save("test-bpn", List.of("group1", "group2", "group3"));
        assertThat(store.resolveForBpn("test-bpn").getContent()).containsExactly("group1", "group2", "group3");
    }

    @Test
    void resolveForBpn_notExists() {
        assertThat(store.resolveForBpn("test-bpn").succeeded()).isFalse();
    }

    @Test
    void resolveForBpn_existsNoGroups() {
        store.save("test-bpn", List.of());
        assertThat(store.resolveForBpn("test-bpn").getContent()).isNotNull().isEmpty();
    }

    @Test
    void save() {
        store.save("test-bpn", List.of("group1", "group2", "group3"));
        assertThat(store.resolveForBpn("test-bpn").getContent()).containsExactly("group1", "group2", "group3");
    }

    @Test
    void save_exists() {
        store.save("test-bpn", List.of("group1", "group2", "group3"));
        assertThat(store.save("test-bpn", List.of("group4")).succeeded()).isFalse();
    }

    @Test
    void delete() {
        var businessPartnerNumber = "test-bpn";
        store.save(businessPartnerNumber, List.of("group1", "group2", "group3"));
        var delete = store.delete(businessPartnerNumber);
        assertThat(delete.succeeded()).withFailMessage(delete::getFailureDetail).isTrue();
    }

    @Test
    void delete_notExist() {
        var businessPartnerNumber = "test-bpn";
        store.delete(businessPartnerNumber);
        assertThat(store.resolveForBpn(businessPartnerNumber).succeeded()).isFalse();
    }

    @Test
    void update() {
        var businessPartnerNumber = "test-bpn";
        store.save(businessPartnerNumber, List.of("group1", "group2", "group3"));
        assertThat(store.update(businessPartnerNumber, List.of("group4", "group5")).succeeded()).isTrue();
    }

    @Test
    void update_notExists() {
        assertThat(store.update("test-bpn", List.of("foo", "bar")).succeeded()).isFalse();
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getTable() + " CASCADE");
    }
}