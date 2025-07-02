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

package org.eclipse.tractusx.edc.policy.cx.warrenty;

import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.tractusx.edc.policy.cx.TestParticipantAgentPolicyContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WarrantyConstraintFunctionTest {

    private final ParticipantAgent participantAgent = mock();
    private final WarrantyConstraintFunction<ParticipantAgentPolicyContext> function = new WarrantyConstraintFunction<>();
    private final ParticipantAgentPolicyContext context = new TestParticipantAgentPolicyContext(participantAgent);

    @Test
    void evaluate() {
        assertThat(function.evaluate(Operator.EQ, "cx.warranty.none:1", null, context)).isTrue();
    }

    @Test
    void validate_whenOperatorAndRightOperandAreValid_thenSuccess() {
        var result = function.validate(Operator.EQ, "cx.warranty.none:1", null);
        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validate_whenInvalidOperator_thenFailure() {
        var result = function.validate(Operator.IS_ANY_OF, "cx.warranty.none:1", null);
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("Invalid operator");
    }

    @Test
    void validate_whenInvalidValue_thenFailure() {
        var result = function.validate(Operator.EQ, "invalid", null);
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("Invalid right-operand: ");
    }
}
