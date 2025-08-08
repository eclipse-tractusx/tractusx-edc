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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.tractusx.edc.agreements.bpns.spi.store.AgreementsBpnsStore;
import org.eclipse.tractusx.edc.agreements.bpns.spi.types.AgreementsBpnsEntry;

import java.sql.SQLException;
import java.util.Objects;

public class SqlAgreementsBpnsStore extends AbstractSqlStore implements AgreementsBpnsStore {

    private final SqlAgreementsBpnsStatements statements;

    public SqlAgreementsBpnsStore(DataSourceRegistry dataSourceRegistry, String dataSourceName,
                                  TransactionContext transactionContext, ObjectMapper objectMapper,
                                  QueryExecutor queryExecutor, SqlAgreementsBpnsStatements statements) {
        super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper, queryExecutor);
        this.statements = statements;
    }

    @Override
    public StoreResult<Void> save(AgreementsBpnsEntry agreementsBpnsEntry) {
        Objects.requireNonNull(agreementsBpnsEntry.getAgreementId());
        Objects.requireNonNull(agreementsBpnsEntry.getProviderBpn());
        Objects.requireNonNull(agreementsBpnsEntry.getConsumerBpn());

        return transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                int inserted = queryExecutor.execute(
                        connection,
                        statements.insertWithOnConflict(),
                        agreementsBpnsEntry.getAgreementId(),
                        agreementsBpnsEntry.getProviderBpn(),
                        agreementsBpnsEntry.getConsumerBpn());

                return inserted == 0 ?
                        StoreResult.alreadyExists(ALREADY_EXISTS_TEMPLATE.formatted(agreementsBpnsEntry.getAgreementId())) :
                        StoreResult.success();
            } catch (SQLException e) {
                throw new EdcPersistenceException(e);
            }
        });
    }
}
