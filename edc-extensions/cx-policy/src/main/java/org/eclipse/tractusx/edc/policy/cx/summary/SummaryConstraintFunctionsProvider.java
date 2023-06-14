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
            "Membership", "cx-active-member",
            "Dismantler", "cx-dismantler",
            "FrameworkAgreement.pcf", "cx-pcf",
            "FrameworkAgreement.sustainability", "cx-sustainability",
            "FrameworkAgreement.quality", "cx-quality",
            "FrameworkAgreement.traceability", "cx-traceability",
            "FrameworkAgreement.behavioraltwin", "cx-behavior-twin",
            "BPN", "cx-bpn"
    );

    /**
     * Configures and registers required summary functions with the policy engine.
     */
    public static void registerFunctions(PolicyEngine engine) {
        var tokenPolicyFunction = new SummaryTokenPolicyFunction();
        engine.registerPreValidator(CATALOG_REQUEST_SCOPE, tokenPolicyFunction);
        engine.registerPreValidator(NEGOTIATION_REQUEST_SCOPE, tokenPolicyFunction);
        engine.registerPreValidator(TRANSFER_PROCESS_REQUEST_SCOPE, tokenPolicyFunction);

        CREDENTIAL_MAPPINGS.forEach((credentialName, summaryType) -> {

            engine.registerFunction(CATALOG_SCOPE,
                    Permission.class,
                    credentialName,
                    new SummaryConstraintFunction(summaryType));

            engine.registerFunction(NEGOTIATION_SCOPE,
                    Permission.class,
                    credentialName,
                    new SummaryConstraintFunction(summaryType));

            engine.registerFunction(TRANSFER_PROCESS_SCOPE,
                    Permission.class,
                    credentialName,
                    new SummaryConstraintFunction(summaryType));
        });

    }

}
