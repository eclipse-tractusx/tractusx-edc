/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial Implementation
 *
 */

package org.eclipse.tractusx.edc.tests.data;

import lombok.Value;

/**
 * The PayMe constraint should be used when no constraint validation/enforcement in the EDC is
 * intended.
 */
@Value
public class PayMeConstraint implements Constraint {
  double amount;
}
