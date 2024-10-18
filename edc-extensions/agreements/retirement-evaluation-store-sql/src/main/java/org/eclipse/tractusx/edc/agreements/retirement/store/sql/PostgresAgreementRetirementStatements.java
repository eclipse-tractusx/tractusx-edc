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
import org.eclipse.edc.sql.translation.PostgresqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlOperatorTranslator;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;

public class PostgresAgreementRetirementStatements implements SqlAgreementsRetirementStatements {

    private final SqlOperatorTranslator operatorTranslator;

    public PostgresAgreementRetirementStatements() {
        this.operatorTranslator = new PostgresqlOperatorTranslator();
    }

    @Override
    public String findByIdTemplate() {
        return format("SELECT * from %s WHERE %s = ?", getTable(), getIdColumn());
    }

    @Override
    public String insertTemplate() {
        return executeStatement()
                .column(getIdColumn())
                .column(getReasonColumn())
                .column(getRetirementDateColumn())
                .insertInto(getTable());
    }

    @Override
    public String getDeleteByIdTemplate() {
        return format("DELETE FROM %s WHERE %s = ?", getTable(), getIdColumn());
    }

    @Override
    public String getCountByIdClause() {
        return format("SELECT COUNT (*) FROM %s WHERE %s = ?", getTable(), getIdColumn());
    }

    @Override
    public String getCountVariableName() {
        return "COUNT";
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        var select = format("SELECT * FROM %s", getTable());
        return new SqlQueryStatement(select, querySpec, new AgreementRetirementMapping(this), operatorTranslator);
    }

    @Override
    public String getFindContractAgreementTemplate() {
        return format("SELECT * FROM %s where %s=?;", getContractAgreementTable(), getContractAgreementIdColumn());
    }
}
