/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.edr.store.sql.schema;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import static java.lang.String.format;

public class BaseSqlEdrStatements implements EdrStatements {

    @Override
    public String getFindByTransferProcessIdTemplate() {
        return format("SELECT * FROM %s WHERE %s = ?", getEdrTable(), getTransferProcessIdColumn());
    }

    @Override
    public SqlQueryStatement createQuery(QuerySpec querySpec) {
        var select = format("SELECT * FROM %s", getEdrTable());
        return new SqlQueryStatement(select, querySpec, new EdrMapping(this));
    }

    @Override
    public String getInsertTemplate() {
        return format("INSERT INTO %s (%s, %s, %s, %s,%s, %s, %s, %s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                getEdrTable(),
                getTransferProcessIdColumn(),
                getAssetIdColumn(),
                getAgreementIdColumn(),
                getEdrId(),
                getProviderIdColumn(),
                getExpirationTimestampColumn(),
                getStateColumn(),
                getStateCountColumn(),
                getStateTimestampColumn(),
                getErrorDetailColumn(),
                getCreatedAtColumn(),
                getUpdatedAtColumn()
        );
    }

    @Override
    public String getUpdateTemplate() {
        return format("UPDATE %s SET %s=?, %s=?, %s=?, %s=?, %s=? WHERE %s = ?;",
                getEdrTable(), getStateColumn(), getStateCountColumn(), getStateTimestampColumn(),
                getErrorDetailColumn(), getUpdatedAtColumn(), getTransferProcessIdColumn());
    }

    @Override
    public String getDeleteByIdTemplate() {
        return format("DELETE FROM %s WHERE %s = ?",
                getEdrTable(),
                getTransferProcessIdColumn());
    }

    @Override
    public String getDeleteLeaseTemplate() {
        return format("DELETE FROM %s WHERE %s=?", getLeaseTableName(), getLeaseIdColumn());
    }

    @Override
    public String getInsertLeaseTemplate() {
        return format("INSERT INTO %s (%s, %s, %s, %s) VALUES (?, ?, ?, ?);",
                getLeaseTableName(), getLeaseIdColumn(), getLeasedByColumn(), getLeasedAtColumn(), getLeaseDurationColumn());
    }

    @Override
    public String getUpdateLeaseTemplate() {
        return format("UPDATE %s SET %s=? WHERE %s = ?;", getEdrTable(), getLeaseIdColumn(), getTransferProcessIdColumn());
    }

    @Override
    public String getFindLeaseByEntityTemplate() {
        return format("SELECT * FROM %s  WHERE %s = (SELECT lease_id FROM %s WHERE %s=? )",
                getLeaseTableName(), getLeaseIdColumn(), getEdrTable(), getTransferProcessIdColumn());
    }
}
