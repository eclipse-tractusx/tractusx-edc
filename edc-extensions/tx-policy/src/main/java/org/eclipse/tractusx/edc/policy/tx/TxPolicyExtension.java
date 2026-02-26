/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * Copyright (c) 2026 Catena-X Automotive Network e.V.
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
 */

package org.eclipse.tractusx.edc.policy.tx;

import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.policy.tx.businesspartner.BusinessPartnerDidConstraintFunction;

import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_POLICY_V2_NS;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.CATALOG_REQUEST_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.CATALOG_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.NEGOTIATION_REQUEST_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.NEGOTIATION_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.TRANSFER_PROCESS_REQUEST_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.TRANSFER_PROCESS_SCOPE;
import static org.eclipse.tractusx.edc.policy.tx.businesspartner.BusinessPartnerDidConstraintFunction.BUSINESS_PARTNER_DID;

/**
 * EDC extension that registers Tractus-X v2.0.0 policy constraint functions.
 * <p>
 * Currently registers:
 * <ul>
 *   <li>{@code https://w3id.org/tractusx/policy/2.0.0/BusinessPartnerDID} — evaluates
 *       the DID of a participant agent as an access-policy constraint.</li>
 * </ul>
 */
@Extension(TxPolicyExtension.NAME)
public class TxPolicyExtension implements ServiceExtension {

    public static final String NAME = "TX Policy";

    @Inject
    private PolicyEngine policyEngine;

    @Inject
    private RuleBindingRegistry bindingRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var businessPartnerDidKey = TX_POLICY_V2_NS + BUSINESS_PARTNER_DID;

        // Register constraint function for catalog (access) scope.
        // No BDRS lookup needed: in DCP/IATP flows the agent identity is already a DID.
        policyEngine.registerFunction(CatalogPolicyContext.class, Permission.class,
                businessPartnerDidKey, new BusinessPartnerDidConstraintFunction<>());

        // Bind the left-operand key to all relevant scopes so the policy engine evaluates it
        bindingRegistry.bind(businessPartnerDidKey, CATALOG_SCOPE);
        bindingRegistry.bind(businessPartnerDidKey, CATALOG_REQUEST_SCOPE);
        bindingRegistry.bind(businessPartnerDidKey, NEGOTIATION_SCOPE);
        bindingRegistry.bind(businessPartnerDidKey, NEGOTIATION_REQUEST_SCOPE);
        bindingRegistry.bind(businessPartnerDidKey, TRANSFER_PROCESS_SCOPE);
        bindingRegistry.bind(businessPartnerDidKey, TRANSFER_PROCESS_REQUEST_SCOPE);
    }
}
