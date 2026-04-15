/********************************************************************************
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.policy.cx.businesspartner;

import jakarta.json.Json;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.tractusx.edc.policy.cx.TestParticipantAgentPolicyContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_CREDENTIAL_NS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BusinessPartnerNumberConstraintFunctionTest {

    private static final String TEST_BPN = "BPNL00000000001A";

    private final ParticipantAgent participantAgent = mock();
    private final BusinessPartnerNumberConstraintFunction<ParticipantAgentPolicyContext> function = new BusinessPartnerNumberConstraintFunction<>();
    private final ParticipantAgentPolicyContext context = new TestParticipantAgentPolicyContext(participantAgent);

    @Test
    void evaluate() {
        var bpn1 = Map.of("string", TEST_BPN);
        var rightValue = List.of(Map.of("@value", bpn1));
        when(participantAgent.getClaims()).thenReturn(Map.of("vc", bpnVcList(TEST_BPN)));
        assertThat(function.evaluate(Operator.IS_ANY_OF, rightValue, null, context)).isTrue();
    }

    @Test
    void evaluate_whenIsNoneOf_andBpnNotInList() {
        var bpn1 = Map.of("string", "BPNL00000000002B");
        var rightValue = List.of(Map.of("@value", bpn1));
        when(participantAgent.getClaims()).thenReturn(Map.of("vc", bpnVcList(TEST_BPN)));
        assertThat(function.evaluate(Operator.IS_NONE_OF, rightValue, null, context)).isTrue();
    }

    @Test
    void evaluate_whenIsNoneOf_andBpnInList_returnsFalse() {
        var bpn1 = Map.of("string", TEST_BPN);
        var rightValue = List.of(Map.of("@value", bpn1));
        when(participantAgent.getClaims()).thenReturn(Map.of("vc", bpnVcList(TEST_BPN)));
        assertThat(function.evaluate(Operator.IS_NONE_OF, rightValue, null, context)).isFalse();
    }

    @Test
    void evaluate_whenNoVcClaim_returnsFalse() {
        when(participantAgent.getClaims()).thenReturn(Map.of());
        var rightValue = List.of(Map.of("@value", Map.of("string", TEST_BPN)));
        assertThat(function.evaluate(Operator.IS_ANY_OF, rightValue, null, context)).isFalse();
        assertThat(context.getProblems()).isNotEmpty();
    }

    @Test
    void evaluate_whenNoBpnCredential_returnsFalse() {
        when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of()));
        var rightValue = List.of(Map.of("@value", Map.of("string", TEST_BPN)));
        assertThat(function.evaluate(Operator.IS_ANY_OF, rightValue, null, context)).isFalse();
        assertThat(context.getProblems()).isNotEmpty();
    }

    @Test
    void validate_whenIsAnyOfAndValidRightValueArePassed_thenSuccess() {
        var bpn1 = Json.createValue("BPNL00000000001A");
        var bpn2 = Json.createValue("BPNL00000000002B");
        var rightValue = List.of(Map.of("@value", bpn1), Map.of("@value", bpn2));
        var result = function.validate(Operator.IS_ANY_OF, rightValue, null);
        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validate_whenIsAnyOfAndRightValuePassedAsString_thenSuccess() {
        var rightValue = "BPNL00000000001A, BPNL00000000002B";
        var result = function.validate(Operator.IS_ANY_OF, rightValue, null);
        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validate_whenIsNoneOfAndValidRightValueArePassed_thenSuccess() {
        var bpn1 = Json.createValue("BPNL00000000001A");
        var bpn2 = Json.createValue("BPNL00000000002B");
        var rightValue = List.of(Map.of("@value", bpn1), Map.of("@value", bpn2));
        var result = function.validate(Operator.IS_NONE_OF, rightValue, null);
        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validate_whenValidOperatorAndOneRightValueIsPassed_thenSuccess() {
        var bpn1 = Json.createValue("BPNL00000000001A");
        var rightValue = List.of(Map.of("@value", bpn1));
        var result = function.validate(Operator.IS_ANY_OF, rightValue, null);
        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validate_whenDuplicateItemsArePassed_thenFails() {
        var rightValue = List.of("BPNL00000000001A", "BPNL00000000001A");
        var result = function.validate(Operator.IS_NONE_OF, rightValue, null);
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("Invalid right-operand: ");
    }

    @Test
    void validate_whenInvalidOperator_thenFailure() {
        var rightValue = List.of("BPNL00000000001A");
        var result = function.validate(Operator.EQ, rightValue, null);
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("Invalid operator");
    }

    @Test
    void validate_whenInvalidRightValueType_thenFailure() {
        var result = function.validate(Operator.IS_ANY_OF, "BPNL000000001A", null);
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("list must contain only unique");
    }

    @Test
    void validate_whenInvalidBpnlFormat_thenFailure() {
        var rightValue = List.of("invalid-bpnl");
        var result = function.validate(Operator.IS_ANY_OF, rightValue, null);
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("Invalid right-operand: ");
    }

    private List<VerifiableCredential> bpnVcList(String bpn) {
        return List.of(VerifiableCredential.Builder.newInstance()
                .types(List.of(CX_CREDENTIAL_NS + "VerifiableCredential", CX_CREDENTIAL_NS + "BpnCredential"))
                .id("test-vc-id")
                .issuer(new org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer("did:web:issuer", Map.of()))
                .issuanceDate(java.time.Instant.now())
                .credentialSubject(org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject.Builder.newInstance()
                        .id("subject-id")
                        .claim(CX_CREDENTIAL_NS + "bpn", bpn)
                        .build())
                .build());
    }
}
