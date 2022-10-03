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

package net.catenax.edc.validation.businesspartner.functions;

import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.engine.AtomicConstraintFunction;
import org.eclipse.dataspaceconnector.spi.policy.engine.PolicyContext;

/** AtomicConstraintFunction to validate business partner numbers for edc duties. */
public class BusinessPartnerDutyFunction extends AbstractBusinessPartnerValidation
    implements AtomicConstraintFunction<Duty> {

  public BusinessPartnerDutyFunction(Monitor monitor) {
    super(monitor);
  }

  @Override
  public boolean evaluate(Operator operator, Object rightValue, Duty rule, PolicyContext context) {
    return evaluate(operator, rightValue, context);
  }
}
