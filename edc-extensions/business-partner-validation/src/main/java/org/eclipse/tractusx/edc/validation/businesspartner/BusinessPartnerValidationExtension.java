/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.edc.validation.businesspartner;

import static org.eclipse.edc.policy.engine.spi.PolicyEngine.ALL_SCOPES;

import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.validation.businesspartner.functions.BusinessPartnerDutyFunction;
import org.eclipse.tractusx.edc.validation.businesspartner.functions.BusinessPartnerPermissionFunction;
import org.eclipse.tractusx.edc.validation.businesspartner.functions.BusinessPartnerProhibitionFunction;

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

  public BusinessPartnerValidationExtension() {}

  public BusinessPartnerValidationExtension(
      final RuleBindingRegistry ruleBindingRegistry, final PolicyEngine policyEngine) {
    this.ruleBindingRegistry = ruleBindingRegistry;
    this.policyEngine = policyEngine;
  }

  @Inject private RuleBindingRegistry ruleBindingRegistry;

  @Inject private PolicyEngine policyEngine;

  @Override
  public String name() {
    return "Business Partner Validation Extension";
  }

  @Override
  public void initialize(ServiceExtensionContext context) {

    final Monitor monitor = context.getMonitor();

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
