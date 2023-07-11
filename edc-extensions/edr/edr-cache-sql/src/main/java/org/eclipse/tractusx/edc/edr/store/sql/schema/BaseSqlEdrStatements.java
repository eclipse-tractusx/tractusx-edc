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
        return format("INSERT INTO %s (%s, %s, %s, %s,%s, %s, %s) VALUES (?, ?, ?, ?, ?, ?, ?)",
                getEdrTable(),
                getTransferProcessIdColumn(),
                getAssetIdColumn(),
                getAgreementIdColumn(),
                getEdrId(),
                getProviderIdColumn(),
                getCreatedAtColumn(),
                getUpdatedAtColumn()
        );
    }

    @Override
    public String getDeleteByIdTemplate() {
        return format("DELETE FROM %s WHERE %s = ?",
                getEdrTable(),
                getTransferProcessIdColumn());
    }
}
