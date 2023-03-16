/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 * ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

package org.eclipse.tractusx.edc.cp.adapter.store.schema;

import static java.lang.String.format;

import org.eclipse.edc.sql.dialect.BaseSqlDialect;

public class BaseSqlDialectObjectStoreStatements implements ObjectStoreStatements {
  @Override
  public String getSaveObjectTemplate() {
    return format(
        "INSERT INTO %s (%s, %s, %s, %s) VALUES(?, ?, ?, ?%s)",
        getObjectStoreTable(),
        getIdColumn(),
        getCreatedAtColumn(),
        getTypeColumn(),
        getObjectColumn(),
        getFormatJsonOperator());
  }

  @Override
  public String getFindByIdAndTypeTemplate() {
    return format(
        "SELECT * FROM %s WHERE %s = ? AND %s = ?",
        getObjectStoreTable(), getIdColumn(), getTypeColumn());
  }

  @Override
  public String getFindByTypeTemplate() {
    return format("SELECT * FROM %s WHERE %s = ?", getObjectStoreTable(), getTypeColumn());
  }

  @Override
  public String getDeleteTemplate() {
    return format(
        "DELETE FROM %s WHERE %s = ? AND %s = ?;",
        getObjectStoreTable(), getIdColumn(), getTypeColumn());
  }

  protected String getFormatJsonOperator() {
    return BaseSqlDialect.getJsonCastOperator();
  }
}
