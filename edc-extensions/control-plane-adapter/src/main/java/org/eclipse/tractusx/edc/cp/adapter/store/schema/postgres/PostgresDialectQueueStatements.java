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

package org.eclipse.tractusx.edc.cp.adapter.store.schema.postgres;

import org.eclipse.edc.sql.dialect.PostgresDialect;
import org.eclipse.tractusx.edc.cp.adapter.store.schema.BaseSqlDialectQueueStatements;

public class PostgresDialectQueueStatements extends BaseSqlDialectQueueStatements {

  /**
   * Overridable operator to convert strings to JSON. For postgres, this is the "::json" operator
   */
  @Override
  protected String getFormatJsonOperator() {
    return PostgresDialect.getJsonCastOperator();
  }
}
