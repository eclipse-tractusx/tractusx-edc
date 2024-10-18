/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.agreements.retirement.store.sql;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

/**
 * Statement templates and SQL table+column names required for the {@link SqlAgreementsRetirementStore}
 */
public interface SqlAgreementsRetirementStatements extends SqlStatements {

    default String getIdColumn() {
        return "contract_agreement_id";
    }

    default String getReasonColumn() {
        return "reason";
    }

    default String getRetirementDateColumn() {
        return "agreement_retirement_date";
    }

    default String getTable() {
        return "edc_agreement_retirement";
    }

    default String getContractAgreementIdColumn() {
        return "agr_id";
    }

    default String getProviderAgentColumn() {
        return "provider_agent_id";
    }

    default String getConsumerAgentColumn() {
        return "consumer_agent_id";
    }

    default String getAssetIdColumn() {
        return "asset_id";
    }

    default String getPolicyColumn() {
        return "policy";
    }

    default String getContractAgreementTable() {
        return "edc_contract_agreement";
    }

    String findByIdTemplate();

    String insertTemplate();

    String getDeleteByIdTemplate();

    String getCountByIdClause();

    String getCountVariableName();

    String getFindContractAgreementTemplate();

    SqlQueryStatement createQuery(QuerySpec querySpec);
}
