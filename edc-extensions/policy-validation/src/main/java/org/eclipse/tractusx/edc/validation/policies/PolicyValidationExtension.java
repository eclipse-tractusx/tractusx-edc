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

package org.eclipse.tractusx.edc.validation.policies;

import static org.eclipse.dataspaceconnector.spi.policy.engine.PolicyEngine.ALL_SCOPES;

import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.engine.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.policy.engine.RuleBindingRegistry;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.validation.policies.attribute.AttributeDutyFunction;
import org.eclipse.tractusx.edc.validation.policies.attribute.AttributePermissionFunction;
import org.eclipse.tractusx.edc.validation.policies.attribute.AttributeProhibitionFunction;
import org.eclipse.tractusx.edc.validation.policies.businesspartner.BusinessPartnerDutyFunction;
import org.eclipse.tractusx.edc.validation.policies.businesspartner.BusinessPartnerPermissionFunction;
import org.eclipse.tractusx.edc.validation.policies.businesspartner.BusinessPartnerProhibitionFunction;
import org.eclipse.tractusx.edc.validation.policies.role.RoleDutyFunction;
import org.eclipse.tractusx.edc.validation.policies.role.RolePermissionFunction;
import org.eclipse.tractusx.edc.validation.policies.role.RoleProhibitionFunction;

public class PolicyValidationExtension implements ServiceExtension {

  /**
   * The key for business partner numbers, role and attribute constraints.
   * Must be used as left operand when declaring constraints.
   *
   * <p>Example: BPN
   *
   * <pre>
   * {
   *     "constraint": {
   *         "leftOperand": "BusinessPartnerNumber",
   *         "operator": "EQ",
   *         "rightOperand": "BPNLCDQ90000X42KU"
   *     }
   * }
   *
   * <p>Example: Role
   *
   * <pre>
   * {
   *     "constraint": {
   *         "leftOperand": "Role",
   *         "operator": "EQ",
   *         "rightOperand": "Dismantler"
   *     }
   * }
   * </pre>
   *
   * <p>Example: Attribute
   *
   * <pre>
   * {
   *     "constraint": {
   *         "leftOperand": "Attribute",
   *         "operator": "EQ",
   *         "rightOperand": "ISO-Certificated"
   *     }
   * }
   * </pre>
   */
  public static final String BUSINESS_PARTNER_CONSTRAINT_KEY = "BusinessPartnerNumber";

  public static final String ROLE_CONSTRAINT_KEY = "Role";
  public static final String ATTRIBUTE_CONSTRAINT_KEY = "Attribute";

  public PolicyValidationExtension() {}

  public PolicyValidationExtension(
      final RuleBindingRegistry ruleBindingRegistry, final PolicyEngine policyEngine) {
    this.ruleBindingRegistry = ruleBindingRegistry;
    this.policyEngine = policyEngine;
  }

  @Inject private RuleBindingRegistry ruleBindingRegistry;

  @Inject private PolicyEngine policyEngine;

  @Override
  public String name() {
    return "Policy Validation Extension";
  }

  @Override
  public void initialize(ServiceExtensionContext context) {

    final Monitor monitor = context.getMonitor();

    ruleBindingRegistry.bind("USE", ALL_SCOPES);

    // BPN Validation
    final BusinessPartnerDutyFunction dutyFunction = new BusinessPartnerDutyFunction(monitor);
    final BusinessPartnerPermissionFunction permissionFunction =
        new BusinessPartnerPermissionFunction(monitor);
    final BusinessPartnerProhibitionFunction prohibitionFunction =
        new BusinessPartnerProhibitionFunction(monitor);
    ruleBindingRegistry.bind(BUSINESS_PARTNER_CONSTRAINT_KEY, ALL_SCOPES);
    policyEngine.registerFunction(
        ALL_SCOPES, Duty.class, BUSINESS_PARTNER_CONSTRAINT_KEY, dutyFunction);
    policyEngine.registerFunction(
        ALL_SCOPES, Permission.class, BUSINESS_PARTNER_CONSTRAINT_KEY, permissionFunction);
    policyEngine.registerFunction(
        ALL_SCOPES, Prohibition.class, BUSINESS_PARTNER_CONSTRAINT_KEY, prohibitionFunction);

    // Role Validation
    final RoleDutyFunction roleDutyFunction = new RoleDutyFunction(monitor);
    final RolePermissionFunction rolePermissionFunction = new RolePermissionFunction(monitor);
    final RoleProhibitionFunction roleProhibitionFunction = new RoleProhibitionFunction(monitor);
    ruleBindingRegistry.bind(ROLE_CONSTRAINT_KEY, ALL_SCOPES);
    policyEngine.registerFunction(ALL_SCOPES, Duty.class, ROLE_CONSTRAINT_KEY, roleDutyFunction);
    policyEngine.registerFunction(
        ALL_SCOPES, Permission.class, ROLE_CONSTRAINT_KEY, rolePermissionFunction);
    policyEngine.registerFunction(
        ALL_SCOPES, Prohibition.class, ROLE_CONSTRAINT_KEY, roleProhibitionFunction);

    // Attribute Validation
    final AttributeDutyFunction attributeDutyFunction = new AttributeDutyFunction(monitor);
    final AttributePermissionFunction attributePermissionFunction =
        new AttributePermissionFunction(monitor);
    final AttributeProhibitionFunction attributeProhibitionFunction =
        new AttributeProhibitionFunction(monitor);
    ruleBindingRegistry.bind(ATTRIBUTE_CONSTRAINT_KEY, ALL_SCOPES);
    policyEngine.registerFunction(
        ALL_SCOPES, Duty.class, ATTRIBUTE_CONSTRAINT_KEY, attributeDutyFunction);
    policyEngine.registerFunction(
        ALL_SCOPES, Permission.class, ATTRIBUTE_CONSTRAINT_KEY, attributePermissionFunction);
    policyEngine.registerFunction(
        ALL_SCOPES, Prohibition.class, ATTRIBUTE_CONSTRAINT_KEY, attributeProhibitionFunction);
  }
}
