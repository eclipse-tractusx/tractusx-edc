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

package org.eclipse.tractusx.edc.agreements.retirement;


import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.tractusx.edc.agreements.retirement.spi.service.AgreementsRetirementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgreementRetirementValidatorTest {

    private AgreementRetirementValidator validator;
    private final AgreementsRetirementService service = mock();
    private final Policy policy = mock();
    private final PolicyContext context = mock();

    @BeforeEach
    public void setup() {
        validator = new AgreementRetirementValidator(service);
    }

    @Test
    @DisplayName("Verify validator returns true if no agreement is found in policyContext")
    public void verify_agreementExistsInPolicyContext() {

        when(context.getContextData(ContractAgreement.class))
                .thenReturn(null);
        assertThat(validator.apply(policy, context)).isTrue();

    }

    @Test
    public void verify_returnFalseWhenRetired() {
        var agreementId = "test-agreement";
        var agreement = buildAgreement(agreementId);

        when(context.getContextData(ContractAgreement.class))
                .thenReturn(agreement);
        when(service.isRetired(agreementId))
                .thenReturn(true);

        var result = validator.apply(policy, context);

        assertThat(result).isFalse();
        verify(context, times(1)).reportProblem(anyString());
    }

    @Test
    public void verify_returnFalseWhenNotRetired() {
        var agreementId = "test-agreement";
        var agreement = buildAgreement(agreementId);

        when(context.getContextData(ContractAgreement.class))
                .thenReturn(agreement);
        when(service.isRetired(agreementId))
                .thenReturn(false);

        var result = validator.apply(policy, context);

        assertThat(result).isTrue();
        verify(context, never()).reportProblem(anyString());
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

}