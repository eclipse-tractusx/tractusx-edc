/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.policy.cx.summary;

import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Permission;

import java.util.Map;

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
    static final Map<String, String> CREDENTIAL_MAPPINGS = Map.of(
            "Membership", "MembershipCredential",
            "Dismantler", "DismantlerCredential",
            "FrameworkAgreement.pcf", "PcfCredential",
            "FrameworkAgreement.sustainability", "SustainabilityCredential",
            "FrameworkAgreement.quality", "QualityCredential",
            "FrameworkAgreement.traceability", "TraceabilityCredential",
            "FrameworkAgreement.behavioraltwin", "BehaviorTwinCredential",
            "BPN", "BpnCredential"
    );

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
    }

}
