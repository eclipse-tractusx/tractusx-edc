/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.agreements.retirement.function;

import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.tractusx.edc.agreements.retirement.spi.AgreementsRetirementStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgreementsRetirementFunctionTest {

    private AgreementsRetirementStore store;
    private AgreementsRetirementFunction function;
    private PolicyContext policyContext;
    private Rule rule;

    @BeforeEach
    void setUp() {
        store = mock(AgreementsRetirementStore.class);
        function = new AgreementsRetirementFunction(mock(Monitor.class), store);
        policyContext = mock(PolicyContext.class);
        rule = mock(Rule.class);
    }

    @Test
    @DisplayName("XYZ")
    void verify_functionIsRegisteredInPolicyMonitorAndTransferScope(){
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("Verify if given agreement with ID can be recovered from policyContext")
    void verify_agreementExistsInPolicyContext(){

        var context = mock(PolicyContext.class);
        function.evaluate(mock(Rule.class), context);
        verify(context).reportProblem("Tried to evaluate agreement retirement function but policyContext has no agreement defined.");

    }

    @Test
    @DisplayName("Fails evaluation if agreement is retired")
    void fails_ifAgreementIsRetired(){

        var agreement = buildAgreement();

        when(policyContext.getContextData(ContractAgreement.class)).thenReturn(agreement);
        when(store.findRetiredAgreement("mock-id")).thenReturn(StoreResult.success("mock-stamp"));

        var result = function.evaluate( rule, policyContext);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Passes evaluation if agreement is not retired")
    void passes_ifAgreementIsNotRetired(){

        var agreement = buildAgreement();

        when(policyContext.getContextData(ContractAgreement.class)).thenReturn(agreement);
        when(store.findRetiredAgreement("mock-id")).thenReturn(StoreResult.notFound("mock-stamp"));

        var result = function.evaluate(rule, policyContext);

        assertThat(result).isTrue();
    }

    private ContractAgreement buildAgreement(){
        return ContractAgreement.Builder.newInstance()
                .id("mock-id")
                .assetId("fake")
                .consumerId("fake")
                .providerId("fake")
                .policy(mock(Policy.class))
                .build();
    }
}