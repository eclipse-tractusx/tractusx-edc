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
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.lease.testfixtures.LeaseUtil;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.eclipse.tractusx.edc.edr.spi.EndpointDataReferenceCacheTestBase;
import org.eclipse.tractusx.edc.edr.spi.store.EndpointDataReferenceCache;
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
import java.time.Duration;

import static java.util.UUID.randomUUID;
import static org.eclipse.tractusx.edc.edr.spi.TestFunctions.edr;
import static org.eclipse.tractusx.edc.edr.spi.TestFunctions.edrEntry;
import static org.eclipse.tractusx.edc.edr.store.sql.SqlEndpointDataReferenceCache.SEPARATOR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@PostgresqlDbIntegrationTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
public class SqlEndpointDataReferenceCacheTest extends EndpointDataReferenceCacheTestBase {

    EdrStatements statements = new PostgresEdrStatements();
    SqlEndpointDataReferenceCache cache;

    Clock clock = Clock.systemUTC();

    Vault vault = mock(Vault.class);

    TypeManager typeManager = new TypeManager();

    LeaseUtil leaseUtil;

    @BeforeEach
    void setUp(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) throws IOException {

        when(vault.deleteSecret(any())).thenReturn(Result.success());
        when(vault.storeSecret(any(), any())).thenReturn(Result.success());
        when(vault.resolveSecret(any())).then(a -> edrJson(a.getArgument(0)));

        cache = new SqlEndpointDataReferenceCache(extension.getDataSourceRegistry(), extension.getDatasourceName(), extension.getTransactionContext(), statements, typeManager.getMapper(), vault, "", clock, queryExecutor, CONNECTOR_NAME);
        var schema = Files.readString(Paths.get("./docs/schema.sql"));
        extension.runQuery(schema);
        leaseUtil = new LeaseUtil(extension.getTransactionContext(), extension::getConnection, statements, clock);

    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) throws SQLException {
        extension.runQuery("DROP TABLE " + statements.getEdrTable() + " CASCADE");
    }

    @Test
    void verify_unoffensive_secretKey() {
        var tpId = "tp1";
        var assetId = "asset1";
        var edrId = "edr1";

        var edr = edr(edrId);
        var entry = edrEntry(assetId, randomUUID().toString(), tpId);

        getStore().save(entry, edr);

        verify(vault).storeSecret(argThat(s -> s.startsWith("edr--")), anyString());
    }

    @Override
    protected EndpointDataReferenceCache getStore() {
        return cache;
    }

    @Override
    protected void lockEntity(String negotiationId, String owner, Duration duration) {
        leaseUtil.leaseEntity(negotiationId, owner, duration);
    }

    @Override
    protected boolean isLockedBy(String negotiationId, String owner) {
        return leaseUtil.isLeased(negotiationId, owner);
    }


    private String edrJson(String id) {
        return typeManager.writeValueAsString(edr(id.split(SEPARATOR)[1]));
    }

}
