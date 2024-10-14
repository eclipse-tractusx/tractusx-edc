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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.tractusx.edc.edr.spi.index.lock.EndpointDataReferenceLock;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_EXPIRES_IN;

public class SqlEdrLock extends AbstractSqlStore implements EndpointDataReferenceLock {
    private final EdrLockStatements statements;

    public SqlEdrLock(DataSourceRegistry dataSourceRegistry, String dataSourceName, TransactionContext transactionContext,
                      ObjectMapper objectMapper, QueryExecutor queryExecutor, EdrLockStatements statements) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
    }

    @Override
    public StoreResult<Boolean> acquireLock(String edrId, DataAddress edr) {
        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var sql = statements.getSelectForUpdateTemplate();
                // this blocks until Postgres can acquire the row-level lock
                var edrEntry = queryExecutor.single(connection, false, this::mapEdr, sql, edrId);

                // check again, to abort
                if (!isExpired(edr, edrEntry)) {
                    return StoreResult.success(false);
                }
                return StoreResult.success(true);
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }

    @Override
    public boolean isExpired(DataAddress edr, EndpointDataReferenceEntry metadata) {
        var expiresInString = edr.getStringProperty(EDR_PROPERTY_EXPIRES_IN);
        if (expiresInString == null) {
            return false;
        }

        var expiresIn = Long.parseLong(expiresInString);
        // createdAt is in millis, expires-in is in seconds
        var expiresAt = metadata.getCreatedAt() / 1000L + expiresIn;
        var expiresAtInstant = Instant.ofEpochSecond(expiresAt);

        return expiresAtInstant.isBefore(Instant.now());
    }

    private EndpointDataReferenceEntry mapEdr(ResultSet resultSet) throws SQLException {
        return EndpointDataReferenceEntry.Builder.newInstance()
                .createdAt(resultSet.getLong(statements.getCreatedAtColumn()))
                .assetId(resultSet.getString(statements.getAssetIdColumn()))
                .transferProcessId(resultSet.getString(statements.getTransferProcessIdColumn()))
                .agreementId(resultSet.getString(statements.getAgreementIdColumn()))
                .providerId(resultSet.getString(statements.getProviderIdColumn()))
                .contractNegotiationId(resultSet.getString(statements.getContractNegotiationIdColumn()))
                .build();
    }

}
