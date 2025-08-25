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
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.eclipse.tractusx.edc.validation.businesspartner.functions.BusinessPartnerGroupLegacyFunction;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.store.BusinessPartnerStore;

import static org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext.CATALOG_SCOPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext.NEGOTIATION_SCOPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext.TRANSFER_SCOPE;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;
import static org.eclipse.tractusx.edc.validation.businesspartner.functions.BusinessPartnerGroupLegacyFunction.BUSINESS_PARTNER_CONSTRAINT_KEY;

/**
 * Registers a {@link org.eclipse.tractusx.edc.validation.businesspartner.functions.BusinessPartnerGroupLegacyFunction} for the following scopes:
 * <ul>
 *     <li>{@code catalog}</li>
 *     <li>{@code contract.negotiation}</li>
 *     <li>{@code transfer.process}</li>
 * </ul>
 * The rule to which the function is bound is {@link BusinessPartnerGroupLegacyFunction#BUSINESS_PARTNER_CONSTRAINT_KEY}. That means, that policies that are bound to these scopes look
 * like this:
 * <pre>
 * {
 *     "constraint": {
 *         "leftOperand": "https://w3id.org/tractusx/v0.0.1/ns/BusinessPartnerGroup",
 *         "operator": "isAnyOf",
 *         "rightOperand": ["gold_customer","platin_partner"]
 *     }
 * }
 * </pre>
 * <p>
 * Note that the {@link BusinessPartnerGroupLegacyFunction} is an {@link org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction}, thus it is registered with the {@link PolicyEngine} for the {@link Permission} class.
 */
@Extension(value = "Registers a function to evaluate whether a BPN number is covered by a certain policy or not", categories = { "policy", "contract" })
public class BusinessPartnerValidationExtension implements ServiceExtension {

    private static final String USE = "USE";

    @Inject
    private RuleBindingRegistry ruleBindingRegistry;
    @Inject
    private PolicyEngine policyEngine;
    @Inject
    private BusinessPartnerStore store;
    @Inject
    private BdrsClient bdrsClient;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor().withPrefix("BusinessPartnerGroupFunction");
        bindToLegacyScope(TRANSFER_SCOPE, TransferProcessPolicyContext.class, new BusinessPartnerGroupLegacyFunction<>(store, bdrsClient, monitor));
        bindToLegacyScope(NEGOTIATION_SCOPE, ContractNegotiationPolicyContext.class, new BusinessPartnerGroupLegacyFunction<>(store, bdrsClient, monitor));
        bindToLegacyScope(CATALOG_SCOPE, CatalogPolicyContext.class, new BusinessPartnerGroupLegacyFunction<>(store, bdrsClient, monitor));
    }

    private <C extends PolicyContext> void bindToLegacyScope(String scope, Class<C> contextType, AtomicConstraintRuleFunction<Permission, C> function) {
        ruleBindingRegistry.bind(USE, scope);
        ruleBindingRegistry.bind(ODRL_SCHEMA + "use", scope);
        ruleBindingRegistry.bind(BUSINESS_PARTNER_CONSTRAINT_KEY, scope);
        ruleBindingRegistry.bind(CX_POLICY_NS, scope);

        policyEngine.registerFunction(contextType, Permission.class, BUSINESS_PARTNER_CONSTRAINT_KEY, function);
    }

}
