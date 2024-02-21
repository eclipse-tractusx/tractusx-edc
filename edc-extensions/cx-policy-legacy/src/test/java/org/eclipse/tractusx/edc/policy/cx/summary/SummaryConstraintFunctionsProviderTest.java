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
import org.junit.jupiter.api.Test;

import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.CATALOG_REQUEST_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.CATALOG_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.NEGOTIATION_REQUEST_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.NEGOTIATION_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.TRANSFER_PROCESS_REQUEST_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.TRANSFER_PROCESS_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.summary.SummaryConstraintFunctionsProvider.registerBindings;
import static org.eclipse.tractusx.edc.policy.cx.summary.SummaryConstraintFunctionsProvider.registerFunctions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SummaryConstraintFunctionsProviderTest {

    @Test
    void verify_function_registrations() {
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

    @Test
    void verify_binding_registrations() {
        var bindingRegistry = mock(RuleBindingRegistry.class);

        registerBindings(bindingRegistry);

        assertRuleTypeRegistered("Membership", bindingRegistry);
        assertRuleTypeRegistered("Dismantler", bindingRegistry);
        assertRuleTypeRegistered("FrameworkAgreement.pcf", bindingRegistry);
        assertRuleTypeRegistered("FrameworkAgreement.sustainability", bindingRegistry);
        assertRuleTypeRegistered("FrameworkAgreement.quality", bindingRegistry);
        assertRuleTypeRegistered("FrameworkAgreement.traceability", bindingRegistry);
        assertRuleTypeRegistered("FrameworkAgreement.behavioraltwin", bindingRegistry);
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

    private void assertRuleTypeRegistered(String ruleType, RuleBindingRegistry bindingRegistry) {
        verify(bindingRegistry, times(1)).bind(ruleType, CATALOG_REQUEST_SCOPE);
        verify(bindingRegistry, times(1)).bind(ruleType, CATALOG_SCOPE);
        verify(bindingRegistry, times(1)).bind(ruleType, NEGOTIATION_REQUEST_SCOPE);
        verify(bindingRegistry, times(1)).bind(ruleType, NEGOTIATION_SCOPE);
        verify(bindingRegistry, times(1)).bind(ruleType, TRANSFER_PROCESS_REQUEST_SCOPE);
        verify(bindingRegistry, times(1)).bind(ruleType, TRANSFER_PROCESS_SCOPE);
    }

}
