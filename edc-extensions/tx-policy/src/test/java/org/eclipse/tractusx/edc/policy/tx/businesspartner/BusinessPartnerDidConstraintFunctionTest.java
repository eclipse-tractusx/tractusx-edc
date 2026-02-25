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

package org.eclipse.tractusx.edc.policy.tx.businesspartner;

import jakarta.json.Json;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;
import org.eclipse.edc.policy.model.Operator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BusinessPartnerDidConstraintFunctionTest {

    private static final String TEST_DID = "did:web:portal-backend.int.catena-x.net:api:administration:staticdata:did:BPNL00000003CRHK";
    private static final String OTHER_DID = "did:web:other.example.com";

    private final ParticipantAgent participantAgent = mock();
    private final BusinessPartnerDidConstraintFunction<ParticipantAgentPolicyContext> function =
            new BusinessPartnerDidConstraintFunction<>();
    private final ParticipantAgentPolicyContext context = new TestParticipantAgentPolicyContext(participantAgent);

    // ─── evaluate() ──────────────────────────────────────────────────────────

    @Test
    void evaluate_isAnyOf_whenDidMatches_thenTrue() {
        var rightValue = List.of(Map.of("@value", Map.of("string", TEST_DID)));
        when(participantAgent.getIdentity()).thenReturn(TEST_DID);

        assertThat(function.evaluate(Operator.IS_ANY_OF, rightValue, null, context)).isTrue();
    }

    @Test
    void evaluate_isAnyOf_whenDidDoesNotMatch_thenFalse() {
        var rightValue = List.of(Map.of("@value", Map.of("string", TEST_DID)));
        when(participantAgent.getIdentity()).thenReturn(OTHER_DID);

        assertThat(function.evaluate(Operator.IS_ANY_OF, rightValue, null, context)).isFalse();
    }

    @Test
    void evaluate_isAnyOf_whenOneOfMultipleDidsMatches_thenTrue() {
        var rightValue = List.of(
                Map.of("@value", Map.of("string", OTHER_DID)),
                Map.of("@value", Map.of("string", TEST_DID))
        );
        when(participantAgent.getIdentity()).thenReturn(TEST_DID);

        assertThat(function.evaluate(Operator.IS_ANY_OF, rightValue, null, context)).isTrue();
    }

    @Test
    void evaluate_isNoneOf_whenDidNotInList_thenTrue() {
        var rightValue = List.of(Map.of("@value", Map.of("string", OTHER_DID)));
        when(participantAgent.getIdentity()).thenReturn(TEST_DID);

        assertThat(function.evaluate(Operator.IS_NONE_OF, rightValue, null, context)).isTrue();
    }

    @Test
    void evaluate_isNoneOf_whenDidIsInList_thenFalse() {
        var rightValue = List.of(Map.of("@value", Map.of("string", TEST_DID)));
        when(participantAgent.getIdentity()).thenReturn(TEST_DID);

        assertThat(function.evaluate(Operator.IS_NONE_OF, rightValue, null, context)).isFalse();
    }

    @Test
    void evaluate_whenNullIdentity_thenFalse() {
        var rightValue = List.of(Map.of("@value", Map.of("string", TEST_DID)));
        when(participantAgent.getIdentity()).thenReturn(null);

        assertThat(function.evaluate(Operator.IS_ANY_OF, rightValue, null, context)).isFalse();
        assertThat(context.getProblems()).anyMatch(p -> p.contains("cannot be null"));
    }

    @Test
    void evaluate_whenUnsupportedOperator_thenFalse() {
        when(participantAgent.getIdentity()).thenReturn(TEST_DID);

        assertThat(function.evaluate(Operator.EQ, TEST_DID, null, context)).isFalse();
        assertThat(context.getProblems()).anyMatch(p -> p.contains("not supported"));
    }

    // ─── validate() ──────────────────────────────────────────────────────────

    @Test
    void validate_whenIsAnyOfAndValidDids_thenSuccess() {
        var did1 = Json.createValue(TEST_DID);
        var did2 = Json.createValue(OTHER_DID);
        var rightValue = List.of(Map.of("@value", did1), Map.of("@value", did2));

        assertThat(function.validate(Operator.IS_ANY_OF, rightValue, null).succeeded()).isTrue();
    }

    @Test
    void validate_whenIsNoneOfAndValidDid_thenSuccess() {
        var did1 = Json.createValue(TEST_DID);
        assertThat(function.validate(Operator.IS_NONE_OF, List.of(Map.of("@value", did1)), null).succeeded()).isTrue();
    }

    @Test
    void validate_whenRightValueIsNotADid_thenFailure() {
        var notADid = Json.createValue("BPNL00000000001A");
        var result = function.validate(Operator.IS_ANY_OF, List.of(Map.of("@value", notADid)), null);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("Invalid right-operand");
    }

    @Test
    void validate_whenInvalidOperator_thenFailure() {
        var result = function.validate(Operator.EQ, List.of(TEST_DID), null);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("Invalid operator");
    }

    // ─── local test helper ───────────────────────────────────────────────────

    private static class TestParticipantAgentPolicyContext extends PolicyContextImpl
            implements ParticipantAgentPolicyContext {

        private final ParticipantAgent participantAgent;

        TestParticipantAgentPolicyContext(ParticipantAgent participantAgent) {
            this.participantAgent = participantAgent;
        }

        @Override
        public ParticipantAgent participantAgent() {
            return participantAgent;
        }

        @Override
        public String scope() {
            return "any";
        }
    }
}
