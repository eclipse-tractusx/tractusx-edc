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

public class BaseSqlDialectQueueStatements implements QueueStatements {

  @Override
  public String getSaveMessageTemplate() {
    return format(
        "INSERT INTO %s (%s, %s, %s, %s, %s) VALUES(?, ?, ?, ?%s, ?)",
        getQueueTable(),
        getCreatedAtColumn(),
        getIdColumn(),
        getChannelColumn(),
        getMessageColumn(),
        getInvokeAfterColumn(),
        getFormatJsonOperator());
  }

  @Override
  public String getAllMessagesTemplate() {
    return format("SELECT * FROM %s ", getQueueTable());
  }

  @Override
  public String getMessagesToSendTemplate() {
    return format(
        "SELECT * FROM %s WHERE %s <= ? AND %s IS NULL LIMIT ?",
        getQueueTable(), getInvokeAfterColumn(), getLeaseIdColumn());
  }
  ;

  @Override
  public String getDeleteTemplate() {
    return format("DELETE FROM %s WHERE %s = ?", getQueueTable(), getIdColumn());
  }

  @Override
  public String getFindByIdTemplate() {
    return format("SELECT * FROM %s WHERE %s = ?", getQueueTable(), getIdColumn());
  }

  @Override
  public String getUpdateTemplate() {
    return format(
        "UPDATE %s SET %s=?, %s=?%s, %s=? WHERE %s=?",
        getQueueTable(),
        getChannelColumn(),
        getMessageColumn(),
        getFormatJsonOperator(),
        getInvokeAfterColumn(),
        getIdColumn());
  }

  @Override
  public String getDeleteLeaseTemplate() {
    return format("DELETE FROM %s WHERE %s = ?;", getLeaseTableName(), getLeaseIdColumn());
  }

  @Override
  public String getInsertLeaseTemplate() {
    return format(
        "INSERT INTO %s (%s, %s, %s, %s)" + "VALUES (?,?,?,?);",
        getLeaseTableName(),
        getLeaseIdColumn(),
        getLeasedByColumn(),
        getLeasedAtColumn(),
        getLeaseDurationColumn());
  }

  @Override
  public String getUpdateLeaseTemplate() {
    return format(
        "UPDATE %s SET %s=? WHERE %s = ?;", getQueueTable(), getLeaseIdColumn(), getIdColumn());
  }

  @Override
  public String getFindLeaseByEntityTemplate() {
    return format(
        "SELECT * FROM %s  WHERE %s = (SELECT lease_id FROM %s WHERE %s=? )",
        getLeaseTableName(), getLeaseIdColumn(), getQueueTable(), getIdColumn());
  }

  protected String getFormatJsonOperator() {
    return BaseSqlDialect.getJsonCastOperator();
  }
}
