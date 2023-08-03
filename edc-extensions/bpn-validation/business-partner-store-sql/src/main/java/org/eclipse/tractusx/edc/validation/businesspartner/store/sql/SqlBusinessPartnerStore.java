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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.BusinessPartnerStore;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class SqlBusinessPartnerStore extends AbstractSqlStore implements BusinessPartnerStore {
    private static final TypeReference<List<String>> LIST_OF_STRING = new TypeReference<>() {
    };
    private final BusinessPartnerGroupStatements statements;

    public SqlBusinessPartnerStore(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext,
                                   ObjectMapper objectMapper, QueryExecutor queryExecutor, BusinessPartnerGroupStatements statements) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
    }

    @Override
    public StoreResult<List<String>> resolveForBpn(String businessPartnerNumber) {
        Objects.requireNonNull(businessPartnerNumber);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var sql = statements.findByBpnTemplate();
                var list = queryExecutor.single(connection, true, this::mapJson, sql, businessPartnerNumber);
                return list == null ?
                        StoreResult.notFound(NOT_FOUND_TEMPLATE.formatted(businessPartnerNumber)) :
                        StoreResult.success(list);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> save(String businessPartnerNumber, List<String> groups) {
        Objects.requireNonNull(businessPartnerNumber);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (exists(businessPartnerNumber, connection)) {
                    return StoreResult.alreadyExists(ALREADY_EXISTS_TEMPLATE.formatted(businessPartnerNumber));
                }
                var sql = statements.insertTemplate();
                queryExecutor.execute(connection, sql, businessPartnerNumber, toJson(groups));
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }


    @Override
    public StoreResult<Void> delete(String businessPartnerNumber) {
        Objects.requireNonNull(businessPartnerNumber);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (!exists(businessPartnerNumber, connection)) {
                    return StoreResult.notFound(NOT_FOUND_TEMPLATE.formatted(businessPartnerNumber));
                }
                var sql = statements.deleteTemplate();
                queryExecutor.execute(connection, sql, businessPartnerNumber);
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public StoreResult<Void> update(String businessPartnerNumber, List<String> groups) {
        Objects.requireNonNull(businessPartnerNumber);
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                if (!exists(businessPartnerNumber, connection)) {
                    return StoreResult.notFound(NOT_FOUND_TEMPLATE.formatted(businessPartnerNumber));
                }
                var sql = statements.updateTemplate();
                queryExecutor.execute(connection, sql, toJson(groups), businessPartnerNumber);
                return StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    private List<String> mapJson(ResultSet resultSet) throws SQLException {
        return fromJson(resultSet.getString(statements.getGroupsColumn()), LIST_OF_STRING);
    }

    private boolean exists(String businessPartnerNumber, Connection connection) {
        var countQuery = statements.countQuery();
        try (var stream = queryExecutor.query(connection, false, r -> r.getInt("COUNT"), countQuery, businessPartnerNumber)) {
            return stream.findFirst().orElse(0) > 0;
        }
    }
}
