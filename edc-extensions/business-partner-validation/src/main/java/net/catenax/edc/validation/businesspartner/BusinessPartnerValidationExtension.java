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

package net.catenax.edc.validation.businesspartner;

import static org.eclipse.dataspaceconnector.spi.policy.engine.PolicyEngine.ALL_SCOPES;

import net.catenax.edc.validation.businesspartner.functions.BusinessPartnerDutyFunction;
import net.catenax.edc.validation.businesspartner.functions.BusinessPartnerPermissionFunction;
import net.catenax.edc.validation.businesspartner.functions.BusinessPartnerProhibitionFunction;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.engine.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.policy.engine.RuleBindingRegistry;
import org.eclipse.dataspaceconnector.spi.system.Requires;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@Requires({RuleBindingRegistry.class, PolicyEngine.class})
public class BusinessPartnerValidationExtension implements ServiceExtension {

  /**
   * The key for business partner numbers constraints. Must be used as left operand when declaring
   * constraints.
   *
   * <p>Example:
   *
   * <pre>
   * {
   *     "constraint": {
   *         "leftOperand": "BusinessPartnerNumber",
   *         "operator": "EQ",
   *         "rightOperand": "BPNLCDQ90000X42KU"
   *     }
   * }
   * </pre>
   */
  public static final String BUSINESS_PARTNER_CONSTRAINT_KEY = "BusinessPartnerNumber";

  @Override
  public String name() {
    return "Business Partner Validation Extension";
  }

  @Override
  public void initialize(ServiceExtensionContext context) {

    final Monitor monitor = context.getMonitor();
    final PolicyEngine policyEngine = context.getService(PolicyEngine.class);
    final RuleBindingRegistry ruleBindingRegistry = context.getService(RuleBindingRegistry.class);

    final BusinessPartnerDutyFunction dutyFunction = new BusinessPartnerDutyFunction(monitor);
    final BusinessPartnerPermissionFunction permissionFunction =
        new BusinessPartnerPermissionFunction(monitor);
    final BusinessPartnerProhibitionFunction prohibitionFunction =
        new BusinessPartnerProhibitionFunction(monitor);

    ruleBindingRegistry.bind("USE", ALL_SCOPES);
    ruleBindingRegistry.bind(BUSINESS_PARTNER_CONSTRAINT_KEY, ALL_SCOPES);

    policyEngine.registerFunction(
        ALL_SCOPES, Duty.class, BUSINESS_PARTNER_CONSTRAINT_KEY, dutyFunction);
    policyEngine.registerFunction(
        ALL_SCOPES, Permission.class, BUSINESS_PARTNER_CONSTRAINT_KEY, permissionFunction);
    policyEngine.registerFunction(
        ALL_SCOPES, Prohibition.class, BUSINESS_PARTNER_CONSTRAINT_KEY, prohibitionFunction);
  }
}
