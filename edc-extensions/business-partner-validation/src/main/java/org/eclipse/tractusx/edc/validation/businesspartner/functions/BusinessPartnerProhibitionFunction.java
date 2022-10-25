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
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package org.eclipse.tractusx.edc.validation.businesspartner.functions;

import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.engine.AtomicConstraintFunction;
import org.eclipse.dataspaceconnector.spi.policy.engine.PolicyContext;

/** AtomicConstraintFunction to validate business partner numbers for edc prohibitions. */
public class BusinessPartnerProhibitionFunction extends AbstractBusinessPartnerValidation
    implements AtomicConstraintFunction<Prohibition> {

  public BusinessPartnerProhibitionFunction(Monitor monitor) {
    super(monitor);
  }

  @Override
  public boolean evaluate(
      Operator operator, Object rightValue, Prohibition rule, PolicyContext context) {
    return evaluate(operator, rightValue, context);
  }
}
