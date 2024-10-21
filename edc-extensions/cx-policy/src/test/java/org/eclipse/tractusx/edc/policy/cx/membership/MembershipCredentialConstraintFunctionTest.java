/*
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
 */

package org.eclipse.tractusx.edc.policy.cx.membership;

import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;
import static org.eclipse.tractusx.edc.policy.cx.CredentialFunctions.createMembershipCredential;
import static org.eclipse.tractusx.edc.policy.cx.CredentialFunctions.createPcfCredential;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MembershipCredentialConstraintFunctionTest {

    private final ParticipantAgent participantAgent = mock();
    private final MembershipCredentialConstraintFunction<PolicyContext> function = new MembershipCredentialConstraintFunction<>() {
        @Override
        protected ParticipantAgent getParticipantAgent(PolicyContext context) {
            return participantAgent;
        }
    };
    private final PolicyContext context = mock();

    @Test
    void evaluate_leftOperandInvalid() {
        when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createMembershipCredential().build())));
        assertThat(function.evaluate(CX_POLICY_NS + "foobar", Operator.EQ, "active", null, context)).isFalse();
        verify(context).reportProblem(startsWith("Invalid left-operand: must be 'Membership', but was"));
    }

    @Test
    void evaluate_noVcClaimOnParticipantAgent() {
        assertThat(function.evaluate(CX_POLICY_NS + "Membership", Operator.EQ, "active", null, context)).isFalse();
        verify(context).reportProblem(eq("ParticipantAgent did not contain a 'vc' claim."));
    }

    @Test
    void evaluate_vcClaimEmpty() {
        when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of()));
        assertThat(function.evaluate(CX_POLICY_NS + "Membership", Operator.EQ, "active", null, context)).isFalse();
        verify(context).reportProblem(eq("ParticipantAgent contains a 'vc' claim but it did not contain any VerifiableCredentials."));
    }

    @Test
    void evaluate_vcClaimNotList() {
        when(participantAgent.getClaims()).thenReturn(Map.of("vc", new Object()));
        assertThat(function.evaluate(CX_POLICY_NS + "Membership", Operator.EQ, "active", null, context)).isFalse();
        verify(context).reportProblem(eq("ParticipantAgent contains a 'vc' claim, but the type is incorrect. Expected java.util.List, received java.lang.Object."));
    }

    @Test
    void evaluate_rightOperandNotActive() {
        when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createMembershipCredential().build())));
        assertThat(function.evaluate(CX_POLICY_NS + "Membership", Operator.EQ, "invalid", null, context)).isFalse();
        verify(context).reportProblem(eq("Right-operand must be equal to 'active', but was 'invalid'"));
    }

    @Test
    void evaluate_whenSingleCredentialFound() {
        when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createMembershipCredential().build())));
        assertThat(function.evaluate(CX_POLICY_NS + "Membership", Operator.EQ, "active", null, context)).isTrue();
    }

    @Test
    void evaluate_whenMultipleCredentialsFound() {
        when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createMembershipCredential().build(),
                createMembershipCredential().build(),
                createPcfCredential().build())));
        assertThat(function.evaluate(CX_POLICY_NS + "Membership", Operator.EQ, "active", null, context)).isTrue();
    }

    @Test
    void evaluate_whenCredentialNotFound() {
        when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createPcfCredential().build())));
        assertThat(function.evaluate(CX_POLICY_NS + "Membership", Operator.EQ, "active", null, context)).isFalse();
    }
}
