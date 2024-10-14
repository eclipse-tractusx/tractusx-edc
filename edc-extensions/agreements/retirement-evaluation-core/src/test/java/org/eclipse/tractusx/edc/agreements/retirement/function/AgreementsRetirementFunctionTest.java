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
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.tractusx.edc.agreements.retirement.spi.store.AgreementsRetirementStore;
import org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgreementsRetirementFunctionTest {

    private AgreementsRetirementStore store;
    private AgreementsRetirementFunction function;
    private PolicyContext policyContext;
    private Permission rule;

    @BeforeEach
    void setUp() {
        store = mock(AgreementsRetirementStore.class);
        function = new AgreementsRetirementFunction(store);
        policyContext = mock(PolicyContext.class);
        rule = mock(Permission.class);
    }

    @Test
    @DisplayName("Evaluation passes if no agreement is found in policyContext")
    void verify_agreementExistsInPolicyContext() {

        when(policyContext.getContextData(ContractAgreement.class))
                .thenReturn(null);
        assertThat(function.evaluate(rule, policyContext)).isTrue();

    }

    @Test
    @DisplayName("Fails evaluation if agreement is retired")
    void fails_ifAgreementIsRetired() {

        var agreementId = "test-agreement-id";
        var agreement = buildAgreement(agreementId);

        var entry = AgreementsRetirementEntry.Builder.newInstance()
                .withAgreementId(agreementId)
                .withReason("mock-reason")
                .build();

        when(policyContext.getContextData(ContractAgreement.class)).thenReturn(agreement);
        when(store.findRetiredAgreements(createFilterQueryByAgreementId(agreement.getId())))
                .thenReturn(StoreResult.success(List.of(entry)));

        var result = function.evaluate(rule, policyContext);

        assertThat(result).isTrue();
        verify(policyContext, times(1)).reportProblem(any());
    }

    @Test
    @DisplayName("Passes evaluation if agreement is not retired")
    void passes_ifAgreementIsNotRetired() {

        when(store.findRetiredAgreements(any(QuerySpec.class)))
                .thenReturn(StoreResult.success(List.of()));

        var result = function.evaluate(rule, policyContext);

        assertThat(result).isTrue();
        verify(policyContext, never()).reportProblem(any());
    }

    private ContractAgreement buildAgreement(String agreementId) {
        return ContractAgreement.Builder.newInstance()
                .id(agreementId)
                .assetId("fake")
                .consumerId("fake")
                .providerId("fake")
                .policy(mock(Policy.class))
                .build();
    }

    private QuerySpec createFilterQueryByAgreementId(String agreementId) {
        return QuerySpec.Builder.newInstance()
                .filter(
                        Criterion.Builder.newInstance()
                                .operandLeft("agreementId")
                                .operator("=")
                                .operandRight(agreementId)
                                .build()
                ).build();
    }
}