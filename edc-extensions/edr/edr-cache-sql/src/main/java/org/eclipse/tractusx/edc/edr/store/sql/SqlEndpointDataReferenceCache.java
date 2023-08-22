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
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.ResultSetMapper;
import org.eclipse.edc.sql.lease.SqlLeaseContextBuilder;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.tractusx.edc.edr.spi.store.EndpointDataReferenceCache;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.tractusx.edc.edr.store.sql.schema.EdrStatements;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.ASSET_ID;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.PROVIDER_ID;

public class SqlEndpointDataReferenceCache extends AbstractSqlStore implements EndpointDataReferenceCache {

    public static final String SEPARATOR = "--";
    public static final String VAULT_PREFIX = "edr" + SEPARATOR;
    private final EdrStatements statements;
    private final Clock clock;
    private final Vault vault;

    private final SqlLeaseContextBuilder leaseContext;

    private final String leaseHolder;


    public SqlEndpointDataReferenceCache(DataSourceRegistry dataSourceRegistry, String dataSourceName,
                                         TransactionContext transactionContext, EdrStatements statements,
                                         ObjectMapper objectMapper, Vault vault, Clock clock,
                                         QueryExecutor queryExecutor, String connectorId) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
        this.clock = clock;
        this.vault = vault;
        this.leaseHolder = connectorId;
        leaseContext = SqlLeaseContextBuilder.with(transactionContext, connectorId, statements, clock, queryExecutor);
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

    @Override
    public @Nullable StoreResult<EndpointDataReferenceEntry> findByIdAndLease(String transferProcessId) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var entity = findById(connection, transferProcessId, this::mapResultSet);
                if (entity == null) {
                    return StoreResult.notFound(format("EndpointDataReference %s not found", transferProcessId));
                }
                leaseContext.withConnection(connection).acquireLease(entity.getId());
                return StoreResult.success(entity);
            } catch (Exception exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    @Override
    public StoreResult<EndpointDataReferenceEntry> findByCorrelationIdAndLease(String correlationId) {
        return findByIdAndLease(correlationId);
    }

    @Override
    public void save(EndpointDataReferenceEntry entity) {
        throw new UnsupportedOperationException("Please use save(EndpointDataReferenceEntry, EndpointDataReference) instead!");
    }

    @Override
    public @NotNull List<EndpointDataReference> referencesForAsset(String assetId, String providerId) {
        var querySpec = QuerySpec.Builder.newInstance();
        querySpec.filter(filterFor(ASSET_ID, assetId));

        if (providerId != null) {
            querySpec.filter(filterFor(PROVIDER_ID, providerId));
        }

        return internalQuery(querySpec.build(), this::mapToWrapper)
                .filter(wrapper -> filterActive(wrapper.getEntry()))
                .map(EndpointDataReferenceEntryWrapper::getEdrId)
                .map(this::referenceFromEntry).collect(Collectors.toList());
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
                queryExecutor.execute(connection, sql,
                        entry.getTransferProcessId(),
                        entry.getAssetId(),
                        entry.getAgreementId(),
                        edr.getId(),
                        entry.getProviderId(),
                        entry.getExpirationTimestamp(),
                        entry.getState(),
                        entry.getStateCount(),
                        entry.getStateTimestamp(),
                        entry.getErrorDetail(),
                        entry.getCreatedAt(),
                        entry.getUpdatedAt());
                vault.storeSecret(VAULT_PREFIX + edr.getId(), toJson(edr)).orElseThrow((failure) -> new EdcPersistenceException(failure.getFailureDetail()));
            } catch (Exception exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    @Override
    public void update(EndpointDataReferenceEntry entry) {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                leaseContext.withConnection(connection).breakLease(entry.getTransferProcessId());
                var sql = statements.getUpdateTemplate();
                queryExecutor.execute(connection, sql,
                        entry.getState(),
                        entry.getStateCount(),
                        entry.getStateTimestamp(),
                        entry.getErrorDetail(),
                        entry.getUpdatedAt(),
                        entry.getTransferProcessId());
            } catch (SQLException exception) {
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
                    leaseContext.withConnection(connection).acquireLease(id);
                    queryExecutor.execute(connection, statements.getDeleteByIdTemplate(), id);
                    leaseContext.withConnection(connection).breakLease(id);
                    vault.deleteSecret(VAULT_PREFIX + entryWrapper.getEdrId()).orElseThrow((failure) -> new EdcPersistenceException(failure.getFailureDetail()));
                    return StoreResult.success(entryWrapper.getEntry());
                } else {
                    return StoreResult.notFound(format("EDR with id %s not found", id));
                }
            } catch (SQLException exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }

    @Override
    public @NotNull List<EndpointDataReferenceEntry> nextNotLeased(int max, Criterion... criteria) {
        return transactionContext.execute(() -> {
            var filter = Arrays.stream(criteria).collect(toList());
            var querySpec = QuerySpec.Builder.newInstance().filter(filter).limit(max).build();
            var statement = statements.createQuery(querySpec);
            statement.addWhereClause(statements.getNotLeasedFilter());
            statement.addParameter(clock.millis());

            try (
                    var connection = getConnection();
                    var stream = queryExecutor.query(getConnection(), true, this::mapResultSet, statement.getQueryAsString(), statement.getParameters())
            ) {
                var negotiations = stream.collect(toList());
                negotiations.forEach(cn -> leaseContext.withConnection(connection).acquireLease(cn.getId()));
                return negotiations;
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private <T> T findById(Connection connection, String id, ResultSetMapper<T> resultSetMapper) {
        var sql = statements.getFindByTransferProcessIdTemplate();
        return queryExecutor.single(connection, false, resultSetMapper, sql, id);
    }

    @NotNull
    private <T> Stream<T> internalQuery(QuerySpec spec, ResultSetMapper<T> resultSetMapper) {
        return transactionContext.execute(() -> {
            try {
                var queryStmt = statements.createQuery(spec);
                return queryExecutor.query(getConnection(), true, resultSetMapper, queryStmt.getQueryAsString(), queryStmt.getParameters());
            } catch (SQLException exception) {
                throw new EdcPersistenceException(exception);
            }
        });

    }

    private EndpointDataReferenceEntry mapResultSet(ResultSet resultSet) throws SQLException {
        Long expirationTimestamp = resultSet.getLong(statements.getExpirationTimestampColumn());
        if (resultSet.wasNull()) {
            expirationTimestamp = null;
        }
        return EndpointDataReferenceEntry.Builder.newInstance()
                .transferProcessId(resultSet.getString(statements.getTransferProcessIdColumn()))
                .assetId(resultSet.getString(statements.getAssetIdColumn()))
                .agreementId(resultSet.getString(statements.getAgreementIdColumn()))
                .providerId(resultSet.getString(statements.getProviderIdColumn()))
                .state(resultSet.getInt(statements.getStateColumn()))
                .stateTimestamp(resultSet.getLong(statements.getStateTimestampColumn()))
                .stateCount(resultSet.getInt(statements.getStateCountColumn()))
                .createdAt(resultSet.getLong(statements.getCreatedAtColumn()))
                .updatedAt(resultSet.getLong(statements.getUpdatedAtColumn()))
                .errorDetail(resultSet.getString(statements.getErrorDetailColumn()))
                .expirationTimestamp(expirationTimestamp)
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

    private Criterion filterFor(String field, Object value) {
        return Criterion.Builder.newInstance()
                .operandLeft(field)
                .operator("=")
                .operandRight(value)
                .build();
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
