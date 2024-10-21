/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.edc.validation.businesspartner;

import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.validation.businesspartner.functions.BusinessPartnerNumberPermissionFunction;

import static org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext.CATALOG_SCOPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext.NEGOTIATION_SCOPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext.TRANSFER_SCOPE;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;
import static org.eclipse.tractusx.edc.validation.businesspartner.BusinessPartnerNumberValidationExtension.NAME;

/**
 * Business partner number evaluation function.
 */
@Extension(NAME)
public class BusinessPartnerNumberValidationExtension implements ServiceExtension {

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
    protected static final String NAME = "Business Partner Validation Extension";
    @Inject
    private RuleBindingRegistry ruleBindingRegistry;
    @Inject
    private PolicyEngine policyEngine;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var function = new BusinessPartnerNumberPermissionFunction();

        bindToScope(TransferProcessPolicyContext.class, function.transferProcess(), TRANSFER_SCOPE);
        bindToScope(ContractNegotiationPolicyContext.class, function.contractNegotiation(), NEGOTIATION_SCOPE);
        bindToScope(CatalogPolicyContext.class, function.catalog(), CATALOG_SCOPE);
    }

    private <C extends PolicyContext> void bindToScope(Class<C> contextType, AtomicConstraintRuleFunction<Permission, C> function, String scope) {
        ruleBindingRegistry.bind("USE", scope);
        ruleBindingRegistry.bind(ODRL_SCHEMA + "use", scope);
        ruleBindingRegistry.bind(BUSINESS_PARTNER_CONSTRAINT_KEY, scope);
        ruleBindingRegistry.bind(TX_BUSINESS_PARTNER_CONSTRAINT_KEY, scope);

        policyEngine.registerFunction(contextType, Permission.class, BUSINESS_PARTNER_CONSTRAINT_KEY, function);
        policyEngine.registerFunction(contextType, Permission.class, TX_BUSINESS_PARTNER_CONSTRAINT_KEY, function);
    }

}
