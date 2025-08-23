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
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.tractusx.edc.policy.cx.TestParticipantAgentPolicyContext;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BusinessPartnerNumberConstraintFunctionTest {

    private final ParticipantAgent participantAgent = mock();
    private final BdrsClient bdrsClient = mock();
    private final BusinessPartnerNumberConstraintFunction<ParticipantAgentPolicyContext> function = new BusinessPartnerNumberConstraintFunction<>(bdrsClient);
    private final ParticipantAgentPolicyContext context = new TestParticipantAgentPolicyContext(participantAgent);

    @Test
    void evaluate() {
        var identity = "BPNL00000000001A";
        var bpn1 = Map.of("string", identity);
        var rightValue = List.of(Map.of("@value", bpn1));
        when(participantAgent.getIdentity()).thenReturn(identity);
        assertThat(function.evaluate(Operator.IS_ANY_OF, rightValue, null, context)).isTrue();
    }

    @Test
    void evaluate_withDid() {
        var bpn1 = Map.of("string", "BPNL00000000001A");
        var rightValue = List.of(Map.of("@value", bpn1));
        var identity = "did:example:some-identity";
        when(participantAgent.getIdentity()).thenReturn(identity);
        when(bdrsClient.resolveBpn(identity)).thenReturn("BPNL00000000001A");
        assertThat(function.evaluate(Operator.IS_ANY_OF, rightValue, null, context)).isTrue();
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
}
