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

import org.eclipse.edc.sql.lease.LeaseStatements;

/** Defines all statements that are needed for the ContractDefinition store */
public interface QueueStatements extends LeaseStatements {
  default String getQueueTable() {
    return "edc_cpadapter_queue";
  }

  default String getIdColumn() {
    return "id";
  }

  default String getCreatedAtColumn() {
    return "created_at";
  }

  default String getChannelColumn() {
    return "channel";
  }

  default String getMessageColumn() {
    return "message";
  }

  default String getInvokeAfterColumn() {
    return "invoke_after";
  }

  String getAllMessagesTemplate();

  String getMessagesToSendTemplate();

  String getSaveMessageTemplate();

  String getDeleteTemplate();

  String getFindByIdTemplate();

  String getUpdateTemplate();
}
