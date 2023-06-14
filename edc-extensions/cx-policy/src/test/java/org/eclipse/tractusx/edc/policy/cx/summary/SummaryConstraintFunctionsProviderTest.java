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
import org.junit.jupiter.api.Test;

import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.CATALOG_REQUEST_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.CATALOG_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.NEGOTIATION_REQUEST_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.NEGOTIATION_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.TRANSFER_PROCESS_REQUEST_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.TRANSFER_PROCESS_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.summary.SummaryConstraintFunctionsProvider.registerFunctions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SummaryConstraintFunctionsProviderTest {

    @Test
    void verify_registrations() {
        var policyEngine = mock(PolicyEngine.class);

        registerFunctions(policyEngine);

        assertTokenFunctionsRegistered(CATALOG_REQUEST_SCOPE, policyEngine);
        assertTokenFunctionsRegistered(NEGOTIATION_REQUEST_SCOPE, policyEngine);
        assertTokenFunctionsRegistered(TRANSFER_PROCESS_REQUEST_SCOPE, policyEngine);

        SummaryConstraintFunctionsProvider.CREDENTIAL_MAPPINGS.forEach((credentialName, summaryType) -> {
            assertSummaryFunctionsRegistered(CATALOG_SCOPE, policyEngine, credentialName);
            assertSummaryFunctionsRegistered(NEGOTIATION_SCOPE, policyEngine, credentialName);
            assertSummaryFunctionsRegistered(TRANSFER_PROCESS_SCOPE, policyEngine, credentialName);
        });

    }

    private void assertTokenFunctionsRegistered(String scope, PolicyEngine policyEngine) {
        verify(policyEngine, times(1)).registerPreValidator(eq(scope), any());
    }

    private void assertSummaryFunctionsRegistered(String scope, PolicyEngine policyEngine, String credentialName) {
        verify(policyEngine, times(1)).registerFunction(
                eq(scope),
                eq(Permission.class),
                eq(credentialName),
                any(SummaryConstraintFunction.class));
    }

}
