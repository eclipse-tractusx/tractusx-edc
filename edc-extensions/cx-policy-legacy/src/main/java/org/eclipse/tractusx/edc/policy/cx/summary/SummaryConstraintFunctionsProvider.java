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

package org.eclipse.tractusx.edc.policy.cx.summary;

import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Permission;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.CATALOG_REQUEST_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.CATALOG_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.NEGOTIATION_REQUEST_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.NEGOTIATION_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.TRANSFER_PROCESS_REQUEST_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.TRANSFER_PROCESS_SCOPE;

/**
 * Registers {@link SummaryConstraintFunction} and {@link SummaryTokenPolicyFunction} instances with the runtime policy engine.
 */
public class SummaryConstraintFunctionsProvider {

    /**
     * Mappings from policy constraint left operand values to the corresponding item value in the summary VP.
     */
    static final Map<String, String> CREDENTIAL_MAPPINGS;


    static {
        var initialMappings = Map.of(
                "Membership", "MembershipCredential",
                "Dismantler", "DismantlerCredential",
                "FrameworkAgreement.pcf", "PcfCredential",
                "FrameworkAgreement.sustainability", "SustainabilityCredential",
                "FrameworkAgreement.quality", "QualityCredential",
                "FrameworkAgreement.traceability", "TraceabilityCredential",
                "FrameworkAgreement.behavioraltwin", "BehaviorTwinCredential",
                "BPN", "BpnCredential"
        );
        var mappings = new HashMap<>(initialMappings);
        initialMappings.forEach((credentialName, summaryType) -> {
            mappings.put(TX_NAMESPACE + credentialName, summaryType);
        });
        CREDENTIAL_MAPPINGS = unmodifiableMap(mappings);
    }

    /**
     * Configures and registers required summary functions with the policy engine.
     */
    public static void registerFunctions(PolicyEngine engine) {
        var tokenPolicyFunction = new SummaryTokenPolicyFunction();
        engine.registerPreValidator(CATALOG_REQUEST_SCOPE, tokenPolicyFunction);
        engine.registerPreValidator(NEGOTIATION_REQUEST_SCOPE, tokenPolicyFunction);
        engine.registerPreValidator(TRANSFER_PROCESS_REQUEST_SCOPE, tokenPolicyFunction);

        CREDENTIAL_MAPPINGS.forEach((constraintName, summaryType) -> {

            engine.registerFunction(CATALOG_SCOPE,
                    Permission.class,
                    constraintName,
                    new SummaryConstraintFunction(summaryType));

            engine.registerFunction(NEGOTIATION_SCOPE,
                    Permission.class,
                    constraintName,
                    new SummaryConstraintFunction(summaryType));

            engine.registerFunction(TRANSFER_PROCESS_SCOPE,
                    Permission.class,
                    constraintName,
                    new SummaryConstraintFunction(summaryType));
        });

    }

    public static void registerBindings(RuleBindingRegistry registry) {
        CREDENTIAL_MAPPINGS.forEach((constraintName, summaryType) -> {
            registry.bind(constraintName, CATALOG_REQUEST_SCOPE);
            registry.bind(constraintName, NEGOTIATION_REQUEST_SCOPE);
            registry.bind(constraintName, TRANSFER_PROCESS_REQUEST_SCOPE);
            registry.bind(constraintName, CATALOG_SCOPE);
            registry.bind(constraintName, NEGOTIATION_SCOPE);
            registry.bind(constraintName, TRANSFER_PROCESS_SCOPE);
        });

        registry.bind(ODRL_SCHEMA + "use", CATALOG_SCOPE);
        registry.bind(ODRL_SCHEMA + "use", NEGOTIATION_SCOPE);
        registry.bind(ODRL_SCHEMA + "use", TRANSFER_PROCESS_SCOPE);

    }

}
