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

package org.eclipse.tractusx.edc.policy.cx.usage;

import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.tractusx.edc.policy.cx.TestParticipantAgentPolicyContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class UsagePurposeConstraintFunctionTest {

    private final ParticipantAgent participantAgent = mock();
    private final UsagePurposeConstraintFunction<ParticipantAgentPolicyContext> function = new UsagePurposeConstraintFunction<>();
    private final ParticipantAgentPolicyContext context = new TestParticipantAgentPolicyContext(participantAgent);

    @ParameterizedTest
    @EnumSource(value = Operator.class, names = "IS_ANY_OF", mode = EnumSource.Mode.EXCLUDE)
    void validate_shouldReturnFalse_forAllOperatorsExceptIsAnyOf(Operator operator) {
        var result = function.validate(operator, "someValue", null);
        assertThat(result.succeeded()).isFalse();
    }

    @Test
    void validate_shouldReturnTrue_forIsAnyOfOperator() {
        var result = function.validate(Operator.IS_ANY_OF, "someValue", null);
        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void evaluate_shouldReturnTrue_withAnyRightValue() {
        assertThat(function.evaluate(Operator.IS_ANY_OF, "anyValue", null, context)).isTrue();
    }
}
