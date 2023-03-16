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

public interface ObjectStoreStatements {
  default String getObjectStoreTable() {
    return "edc_cpadapter_object_store";
  }

  default String getIdColumn() {
    return "id";
  }

  default String getCreatedAtColumn() {
    return "created_at";
  }

  default String getTypeColumn() {
    return "type";
  }

  default String getObjectColumn() {
    return "object";
  }

  String getSaveObjectTemplate();

  String getFindByIdAndTypeTemplate();

  String getFindByTypeTemplate();

  String getDeleteTemplate();
}
