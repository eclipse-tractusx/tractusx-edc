/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.edr.store.sql;

import org.eclipse.edc.junit.annotations.PostgresqlDbIntegrationTest;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.tractusx.edc.edr.store.sql.schema.EdrStatements;
import org.eclipse.tractusx.edc.edr.store.sql.schema.postgres.PostgresEdrStatements;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.Clock;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.tractusx.edc.edr.spi.EndpointDataReferenceCacheTestBase.CONNECTOR_NAME;
import static org.eclipse.tractusx.edc.edr.spi.TestFunctions.edr;
import static org.eclipse.tractusx.edc.edr.spi.TestFunctions.edrEntry;
import static org.eclipse.tractusx.edc.edr.store.sql.SqlEndpointDataReferenceCache.VAULT_PREFIX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@PostgresqlDbIntegrationTest
@ExtendWith(PostgresqlTransactionalStoreSetupExtension.class)
public class SqlEndpointDataReferenceCacheTransactionalTest {

    EdrStatements statements = new PostgresEdrStatements();
    SqlEndpointDataReferenceCache cache;

    Clock clock = Clock.systemUTC();

    Vault vault = mock(Vault.class);

    TypeManager typeManager = new TypeManager();

    @BeforeEach
    void setUp(PostgresqlTransactionalStoreSetupExtension extension, QueryExecutor queryExecutor) throws IOException {

        when(vault.deleteSecret(any())).thenReturn(Result.success());
        when(vault.storeSecret(any(), any())).thenReturn(Result.success());

        cache = new SqlEndpointDataReferenceCache(extension.getDataSourceRegistry(), extension.getDatasourceName(), extension.getTransactionContext(), statements, typeManager.getMapper(), vault, clock, queryExecutor, CONNECTOR_NAME);
        var schema = Files.readString(Paths.get("./docs/schema.sql"));
        extension.runQuery(schema);

    }

    @Test
    void save_shouldFail_whenVaultError() {

        var tpId = "tp1";
        var assetId = "asset1";
        var edrId = "edr1";

        var edr = edr(edrId);
        var entry = edrEntry(assetId, randomUUID().toString(), tpId);

        when(vault.storeSecret(any(), any())).thenReturn(Result.failure("fail"));
        when(vault.resolveSecret(edr.getId())).thenReturn(typeManager.writeValueAsString(edr));

        assertThatThrownBy(() -> cache.save(entry, edr)).isInstanceOf(EdcPersistenceException.class);

        assertThat(cache.resolveReference(tpId))
                .isNull();

    }

    @Test
    void save() {

        var tpId = "tp1";
        var assetId = "asset1";
        var edrId = "edr1";

        var edr = edr(edrId);
        var entry = edrEntry(assetId, randomUUID().toString(), tpId);

        when(vault.storeSecret(any(), any())).thenReturn(Result.success());
        when(vault.resolveSecret(VAULT_PREFIX + edr.getId())).thenReturn(typeManager.writeValueAsString(edr));

        cache.save(entry, edr);

        assertThat(cache.resolveReference(tpId))
                .isNotNull()
                .extracting(EndpointDataReference::getId)
                .isEqualTo(edrId);

        var edrs = cache.referencesForAsset(assetId, null);
        assertThat(edrs.size()).isEqualTo(1);
        assertThat(edrs.get((0)).getId()).isEqualTo(edrId);

        verify(vault).storeSecret(eq(VAULT_PREFIX + edr.getId()), any());
        verify(vault, times(2)).resolveSecret(eq(VAULT_PREFIX + edr.getId()));

    }

    @Test
    void deleteByTransferProcessId_shouldDelete_WhenFound() {

        var entry = edrEntry("assetId", "agreementId", "tpId");
        var edr = edr("edrId");
        cache.save(entry, edr);

        assertThat(cache.deleteByTransferProcessId(entry.getTransferProcessId()))
                .extracting(StoreResult::getContent)
                .isEqualTo(entry);

        assertThat(cache.resolveReference(entry.getTransferProcessId())).isNull();
        assertThat(cache.referencesForAsset(entry.getAssetId(), null)).hasSize(0);
        assertThat(cache.queryForEntries(QuerySpec.max())).hasSize(0);

        verify(vault).storeSecret(eq(VAULT_PREFIX + edr.getId()), any());
        verify(vault).deleteSecret(eq(VAULT_PREFIX + edr.getId()));
    }

    @AfterEach
    void tearDown(PostgresqlTransactionalStoreSetupExtension extension) throws SQLException {
        extension.runQuery("DROP TABLE " + statements.getEdrTable() + " CASCADE");
    }

}
