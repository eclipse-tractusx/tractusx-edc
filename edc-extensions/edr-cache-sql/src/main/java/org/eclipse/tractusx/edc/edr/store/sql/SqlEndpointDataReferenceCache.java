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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.sql.ResultSetMapper;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.tractusx.edc.edr.spi.EndpointDataReferenceCache;
import org.eclipse.tractusx.edc.edr.spi.EndpointDataReferenceEntry;
import org.eclipse.tractusx.edc.edr.store.sql.schema.EdrStatements;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.edc.sql.SqlQueryExecutor.executeQuery;
import static org.eclipse.edc.sql.SqlQueryExecutor.executeQuerySingle;

public class SqlEndpointDataReferenceCache extends AbstractSqlStore implements EndpointDataReferenceCache {

    public static final String SEPARATOR = "--";
    public static final String VAULT_PREFIX = "edr" + SEPARATOR;
    private final EdrStatements statements;
    private final Clock clock;
    private final Vault vault;


    public SqlEndpointDataReferenceCache(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext, EdrStatements statements, ObjectMapper objectMapper, Vault vault, Clock clock) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper);
        this.statements = statements;
        this.clock = clock;
        this.vault = vault;
    }

    @Override
    public @Nullable EndpointDataReference resolveReference(String transferProcessId) {
        Objects.requireNonNull(transferProcessId);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var edrId = findById(connection, transferProcessId, this::mapToEdrId);
                if (edrId != null) {
                    return referenceFromEntry(edrId);
                }
                return null;
            } catch (Exception exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    private <T> T findById(Connection connection, String id, ResultSetMapper<T> resultSetMapper) {
        var sql = statements.getFindByTransferProcessIdTemplate();
        return executeQuerySingle(connection, false, resultSetMapper, sql, id);
    }

    @Override
    public @NotNull List<EndpointDataReference> referencesForAsset(String assetId) {
        return internalQuery(queryFor("assetId", assetId), this::mapToEdrId).map(this::referenceFromEntry).collect(Collectors.toList());
    }

    @NotNull
    private <T> Stream<T> internalQuery(QuerySpec spec, ResultSetMapper<T> resultSetMapper) {
        try {
            var queryStmt = statements.createQuery(spec);
            return executeQuery(getConnection(), true, resultSetMapper, queryStmt.getQueryAsString(), queryStmt.getParameters());
        } catch (SQLException exception) {
            throw new EdcPersistenceException(exception);
        }
    }

    @Override
    public Stream<EndpointDataReferenceEntry> queryForEntries(QuerySpec spec) {
        return internalQuery(spec, this::mapResultSet);
    }

    @Override
    public void save(EndpointDataReferenceEntry entry, EndpointDataReference edr) {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var sql = statements.getInsertTemplate();
                var createdAt = clock.millis();
                executeQuery(connection, sql, entry.getTransferProcessId(), entry.getAssetId(), entry.getAgreementId(), edr.getId(), createdAt, createdAt);
                vault.storeSecret(VAULT_PREFIX + edr.getId(), toJson(edr)).orElseThrow((failure) -> new EdcPersistenceException(failure.getFailureDetail()));
            } catch (Exception exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    @Override
    public StoreResult<EndpointDataReferenceEntry> deleteByTransferProcessId(String id) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var entryWrapper = findById(connection, id, this::mapToWrapper);
                if (entryWrapper != null) {
                    executeQuery(connection, statements.getDeleteByIdTemplate(), id);
                    vault.deleteSecret(VAULT_PREFIX + entryWrapper.getEdrId()).orElseThrow((failure) -> new EdcPersistenceException(failure.getFailureDetail()));
                    return StoreResult.success(entryWrapper.getEntry());
                } else {
                    return StoreResult.notFound(format("EDR with id %s not found", id));
                }
            } catch (Exception exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }


    private EndpointDataReferenceEntry mapResultSet(ResultSet resultSet) throws SQLException {
        return EndpointDataReferenceEntry.Builder.newInstance()
                .transferProcessId(resultSet.getString(statements.getTransferProcessIdColumn()))
                .assetId(resultSet.getString(statements.getAssetIdColumn()))
                .agreementId(resultSet.getString(statements.getAgreementIdColumn()))
                .build();
    }

    private String mapToEdrId(ResultSet resultSet) throws SQLException {
        return resultSet.getString(statements.getEdrId());
    }

    private EndpointDataReferenceEntryWrapper mapToWrapper(ResultSet resultSet) throws SQLException {
        return new EndpointDataReferenceEntryWrapper(mapResultSet(resultSet), mapToEdrId(resultSet));
    }

    private EndpointDataReference referenceFromEntry(String edrId) {
        var edr = vault.resolveSecret(VAULT_PREFIX + edrId);
        if (edr != null) {
            return fromJson(edr, EndpointDataReference.class);
        }
        return null;
    }

    private QuerySpec queryFor(String field, String value) {
        var filter = Criterion.Builder.newInstance()
                .operandLeft(field)
                .operator("=")
                .operandRight(value)
                .build();

        return QuerySpec.Builder.newInstance().filter(filter).build();
    }

    private static class EndpointDataReferenceEntryWrapper {
        private final EndpointDataReferenceEntry entry;
        private final String edrId;

        private EndpointDataReferenceEntryWrapper(EndpointDataReferenceEntry entry, String edrId) {
            this.entry = Objects.requireNonNull(entry);
            this.edrId = Objects.requireNonNull(edrId);
        }

        public EndpointDataReferenceEntry getEntry() {
            return entry;
        }

        public String getEdrId() {
            return edrId;
        }
    }
}
