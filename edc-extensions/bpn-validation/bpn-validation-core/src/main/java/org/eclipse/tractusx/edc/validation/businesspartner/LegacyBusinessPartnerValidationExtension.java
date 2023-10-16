/*
 *
 *   Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft
 *
 *   See the NOTICE file(s) distributed with this work for additional
 *   information regarding copyright ownership.
 *
 *   This program and the accompanying materials are made available under the
 *   terms of the Apache License, Version 2.0 which is available at
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *   License for the specific language governing permissions and limitations
 *   under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.tractusx.edc.validation.businesspartner;

import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.validation.businesspartner.functions.legacy.BusinessPartnerDutyFunction;
import org.eclipse.tractusx.edc.validation.businesspartner.functions.legacy.BusinessPartnerPermissionFunction;
import org.eclipse.tractusx.edc.validation.businesspartner.functions.legacy.BusinessPartnerProhibitionFunction;

import static org.eclipse.edc.connector.contract.spi.offer.ContractDefinitionResolver.CATALOGING_SCOPE;
import static org.eclipse.edc.connector.contract.spi.validation.ContractValidationService.NEGOTIATION_SCOPE;
import static org.eclipse.edc.connector.contract.spi.validation.ContractValidationService.TRANSFER_SCOPE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;

/**
 * Business partner number evaluation function.
 *
 * @deprecated Please use {@code BusinessPartnerEvaluationExtension} instead.
 */
@Deprecated(forRemoval = true, since = "0.5.0")
public class LegacyBusinessPartnerValidationExtension implements ServiceExtension {

    /**
     * The key for business partner numbers constraints. Must be used as left operand when declaring constraints.
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

    public static final String TX_BUSINESS_PARTNER_CONSTRAINT_KEY = TX_NAMESPACE + BUSINESS_PARTNER_CONSTRAINT_KEY;


    public static final String DEFAULT_LOG_AGREEMENT_EVALUATION = "true";


    @Setting(value = "Enable logging when evaluating the business partner constraints in the agreement validation", type = "boolean", defaultValue = DEFAULT_LOG_AGREEMENT_EVALUATION)
    public static final String BUSINESS_PARTNER_VALIDATION_LOG_AGREEMENT_VALIDATION = "tractusx.businesspartnervalidation.log.agreement.validation";
    @Inject
    private RuleBindingRegistry ruleBindingRegistry;
    @Inject
    private PolicyEngine policyEngine;

    public LegacyBusinessPartnerValidationExtension() {
    }

    public LegacyBusinessPartnerValidationExtension(
            final RuleBindingRegistry ruleBindingRegistry, final PolicyEngine policyEngine) {
        this.ruleBindingRegistry = ruleBindingRegistry;
        this.policyEngine = policyEngine;
    }

    @Override
    public String name() {
        return "Business Partner Validation Extension";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        var monitor = context.getMonitor();

        var logAgreementEvaluation = logAgreementEvaluationSetting(context);

        var dutyFunction = new BusinessPartnerDutyFunction(monitor, logAgreementEvaluation);
        var permissionFunction = new BusinessPartnerPermissionFunction(monitor, logAgreementEvaluation);
        var prohibitionFunction = new BusinessPartnerProhibitionFunction(monitor, logAgreementEvaluation);

        bindToScope(dutyFunction, permissionFunction, prohibitionFunction, TRANSFER_SCOPE);
        bindToScope(dutyFunction, permissionFunction, prohibitionFunction, NEGOTIATION_SCOPE);
        bindToScope(dutyFunction, permissionFunction, prohibitionFunction, CATALOGING_SCOPE);

        monitor.warning("This extension was deprecated and is scheduled for removal in version 0.6.0 of Tractus-X EDC");
    }

    private void bindToScope(BusinessPartnerDutyFunction dutyFunction, BusinessPartnerPermissionFunction permissionFunction, BusinessPartnerProhibitionFunction prohibitionFunction, String scope) {
        ruleBindingRegistry.bind("USE", scope);
        ruleBindingRegistry.bind(BUSINESS_PARTNER_CONSTRAINT_KEY, scope);
        ruleBindingRegistry.bind(TX_BUSINESS_PARTNER_CONSTRAINT_KEY, scope);


        policyEngine.registerFunction(scope, Duty.class, BUSINESS_PARTNER_CONSTRAINT_KEY, dutyFunction);
        policyEngine.registerFunction(scope, Permission.class, BUSINESS_PARTNER_CONSTRAINT_KEY, permissionFunction);
        policyEngine.registerFunction(scope, Prohibition.class, BUSINESS_PARTNER_CONSTRAINT_KEY, prohibitionFunction);

        policyEngine.registerFunction(scope, Permission.class, TX_BUSINESS_PARTNER_CONSTRAINT_KEY, permissionFunction);
    }

    private boolean logAgreementEvaluationSetting(ServiceExtensionContext context) {
        return Boolean.parseBoolean(context.getSetting(BUSINESS_PARTNER_VALIDATION_LOG_AGREEMENT_VALIDATION, DEFAULT_LOG_AGREEMENT_EVALUATION));
    }
}
