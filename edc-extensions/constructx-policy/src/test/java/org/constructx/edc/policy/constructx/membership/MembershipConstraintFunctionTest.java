/*
 * Copyright (c) 2024 T-Systems International GmbH
 * Copyright (c) 2025 SAP SE
 * Copyright (c) 2026 Materna SE
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

package org.constructx.edc.policy.constructx.membership;

import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.constructx.edc.policy.constructx.ConstructxPolicyConstants.CONSTRUCTX_POLICY_NS;
import static org.constructx.edc.policy.constructx.CredentialFunctions.createCredential;
import static org.constructx.edc.policy.constructx.CredentialFunctions.createMembershipCredential;
import static org.constructx.edc.policy.constructx.membership.MembershipCredentialConstraintFunction.CONSTRUCTX_MEMBERSHIP_LITERAL;
import static org.constructx.edc.policy.constructx.membership.MembershipCredentialConstraintFunction.MEMBERSHIP_LITERAL;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MembershipConstraintFunctionTest {

    private final Monitor monitor = mock();
    private final MembershipCredentialConstraintFunction<ParticipantAgentPolicyContext> function = new MembershipCredentialConstraintFunction<>(monitor);
    private final ParticipantAgentPolicyContext context = mock();
    private final ParticipantAgent participantAgent = mock();

    @BeforeEach
    void setup() {
        when(context.participantAgent()).thenReturn(participantAgent);
    }

    @Test
    void evaluate_noParticipantAgentOnContext() {
        when(context.participantAgent()).thenReturn(null);

        assertThat(function.evaluate(CONSTRUCTX_POLICY_NS + MEMBERSHIP_LITERAL, Operator.EQ, "active", null, context)).isFalse();

        verify(monitor).warning(eq("The %s%s Policy is deprecated since version 0.0.4 and will be removed in future releases. Please use %s%s Policy instead."
                .formatted(CONSTRUCTX_POLICY_NS, MEMBERSHIP_LITERAL, CONSTRUCTX_POLICY_NS, CONSTRUCTX_MEMBERSHIP_LITERAL)));
        verify(context).reportProblem("Required PolicyContext data not found: org.eclipse.edc.participant.spi.ParticipantAgent");
    }

    @Test
    void evaluate_noVcClaimOnParticipantAgent() {
        assertThat(function.evaluate(CONSTRUCTX_POLICY_NS + CONSTRUCTX_MEMBERSHIP_LITERAL, Operator.EQ, "active", null, context)).isFalse();

        verify(context).reportProblem(eq("ParticipantAgent did not contain a 'vc' claim."));
    }

    @Test
    void evaluate_vcClaimEmpty() {
        when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of()));

        assertThat(function.evaluate(CONSTRUCTX_POLICY_NS + CONSTRUCTX_MEMBERSHIP_LITERAL, Operator.EQ, "active", null, context)).isFalse();

        verify(context).reportProblem(eq("ParticipantAgent contains a 'vc' claim but it did not contain any VerifiableCredentials."));
    }

    @Test
    void evaluate_vcClaimNotList() {
        when(participantAgent.getClaims()).thenReturn(Map.of("vc", new Object()));

        assertThat(function.evaluate(CONSTRUCTX_POLICY_NS + CONSTRUCTX_MEMBERSHIP_LITERAL, Operator.EQ, "active", null, context)).isFalse();

        verify(context).reportProblem(eq("ParticipantAgent contains a 'vc' claim, but the type is incorrect. Expected java.util.List, received java.lang.Object."));
    }

    @Test
    void evaluate_rightOperandNotActive() {
        when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createMembershipCredential().build())));

        assertThat(function.evaluate(CONSTRUCTX_POLICY_NS + CONSTRUCTX_MEMBERSHIP_LITERAL, Operator.EQ, "invalid", null, context)).isFalse();

        verify(context).reportProblem(eq("Right-operand must be equal to 'active', but was 'invalid'"));
    }

    @Test
    void evaluate_whenConstructxMembershipCredentialFound() {
        when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createMembershipCredential().build())));

        assertThat(function.evaluate(CONSTRUCTX_POLICY_NS + CONSTRUCTX_MEMBERSHIP_LITERAL, Operator.EQ, "active", null, context)).isTrue();
    }

    @Test
    void evaluate_whenLegacyMembershipCredentialFound() {
        when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createMembershipCredential().build())));

        assertThat(function.evaluate(CONSTRUCTX_POLICY_NS + MEMBERSHIP_LITERAL, Operator.EQ, "active", null, context)).isTrue();

        verify(monitor).warning(eq("The %s%s Policy is deprecated since version 0.0.4 and will be removed in future releases. Please use %s%s Policy instead."
                .formatted(CONSTRUCTX_POLICY_NS, MEMBERSHIP_LITERAL, CONSTRUCTX_POLICY_NS, CONSTRUCTX_MEMBERSHIP_LITERAL)));
    }

    @Test
    void evaluate_whenMultipleCredentialsFound() {
        when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(
                createMembershipCredential().build(),
                createMembershipCredential().build(),
                createCredential("BogusCredential").build()
        )));

        assertThat(function.evaluate(CONSTRUCTX_POLICY_NS + CONSTRUCTX_MEMBERSHIP_LITERAL, Operator.EQ, "active", null, context)).isTrue();
    }

    @Test
    void evaluate_whenCredentialNotFound() {
        when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createCredential("BogusCredential").build())));

        assertThat(function.evaluate(CONSTRUCTX_POLICY_NS + CONSTRUCTX_MEMBERSHIP_LITERAL, Operator.EQ, "active", null, context)).isFalse();
    }

    @Test
    void test_canHandle() {
        assertThat(function.canHandle(List.of())).isFalse();
        assertThat(function.canHandle("AnyLiteral")).isFalse();

        assertThat(function.canHandle(CONSTRUCTX_MEMBERSHIP_LITERAL)).isFalse();
        assertThat(function.canHandle(CONSTRUCTX_POLICY_NS + CONSTRUCTX_MEMBERSHIP_LITERAL)).isTrue();

        assertThat(function.canHandle(MEMBERSHIP_LITERAL)).isFalse();
        assertThat(function.canHandle(CONSTRUCTX_POLICY_NS + MEMBERSHIP_LITERAL)).isTrue();
    }
}
