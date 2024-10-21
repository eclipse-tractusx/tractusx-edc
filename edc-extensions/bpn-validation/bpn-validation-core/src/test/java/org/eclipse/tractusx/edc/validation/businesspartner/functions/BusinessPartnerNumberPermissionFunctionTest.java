/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.validation.businesspartner.functions;

import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BusinessPartnerNumberPermissionFunctionTest {

    private final PolicyContext policyContext = new PolicyContextImpl() {
        @Override
        public String scope() {
            return "any";
        }
    };
    private final ParticipantAgent participantAgent = mock();
    private final BusinessPartnerNumberPermissionFunction validation = new BusinessPartnerNumberPermissionFunction();

    @ParameterizedTest(name = "Illegal Operator {0}")
    @ArgumentsSource(IllegalOperatorProvider.class)
    void testFailsOnUnsupportedOperations(Operator illegalOperator) {
        var result = validation.evaluate(illegalOperator, "foo", participantAgent, policyContext);

        assertFalse(result);
        assertThat(policyContext.getProblems()).hasSize(1)
                .anyMatch(it -> it.startsWith("Operator %s is not supported.".formatted(illegalOperator)));
    }

    @Test
    void testFailsOnUnsupportedRightValue() {
        when(participantAgent.getIdentity()).thenReturn("foo");
        var result = validation.evaluate(Operator.EQ, 1, participantAgent, policyContext);

        assertFalse(result);
        assertThat(policyContext.getProblems()).hasSize(1)
                        .anyMatch(it -> it.contains("Invalid right-value: operator 'EQ' requires a 'String' or a 'List' but got a 'java.lang.Integer'"));
    }

    @Test
    void testValidationFailsIdentityIsMissing() {
        var result = validation.evaluate(Operator.EQ, "foo", participantAgent, policyContext);

        assertThat(result).isFalse();
        assertThat(policyContext.getProblems()).hasSize(1)
                .anyMatch(it -> it.contains("Identity of the participant agent cannot be null"));
    }

    @Test
    void testValidationFailsParticipantAgentMissing() {
        var result = validation.evaluate(Operator.EQ, "foo", null, policyContext);

        assertThat(result).isFalse();
        assertThat(policyContext.getProblems()).hasSize(1)
                .anyMatch(it -> it.contains("Required PolicyContext data not found"));
    }

    @Test
    void testValidationWhenSingleParticipantIsValid() {
        when(participantAgent.getIdentity()).thenReturn("foo");

        var result = validation.evaluate(Operator.EQ, "foo", participantAgent, policyContext);

        assertThat(result).isTrue();
    }

    @Test
    void testValidationFailsInvalidIdentity() {
        when(participantAgent.getIdentity()).thenReturn("bar");

        var result = validation.evaluate(Operator.EQ, "foo", participantAgent, policyContext);

        assertThat(result).isFalse();
    }

    @Test
    void testValidationForMultipleParticipants() {
        when(participantAgent.getIdentity()).thenReturn("quazz");

        assertThat(validation.evaluate(Operator.IN, List.of("foo", "bar"), participantAgent, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.IN, List.of(1, "foo"), participantAgent, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.IN, List.of("bar", "bar"), participantAgent, policyContext)).isFalse();
    }

    @Test
    void evaluate_neq() {
        when(participantAgent.getIdentity()).thenReturn("foo");
        assertThat(validation.evaluate(Operator.NEQ, "bar", participantAgent, policyContext)).isTrue();

        // these two should report a problem
        assertThat(validation.evaluate(Operator.NEQ, 1, participantAgent, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.NEQ, List.of("foo", "bar"), participantAgent, policyContext)).isTrue();
    }

    @Test
    void evaluate_hasPart() {
        when(participantAgent.getIdentity()).thenReturn("quizzquazz");
        assertThat(validation.evaluate(Operator.HAS_PART, "quizz", participantAgent, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.HAS_PART, "quazz", participantAgent, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.HAS_PART, "zzqua", participantAgent, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.HAS_PART, "zzqui", participantAgent, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.HAS_PART, "Quizz", participantAgent, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.HAS_PART, List.of("quizz"), participantAgent, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.HAS_PART, List.of("quizz", "quazz"), participantAgent, policyContext)).isFalse();
        assertThat(policyContext.getProblems()).hasSize(2)
                .allMatch(it -> it.startsWith("Invalid right-value: operator 'HAS_PART' requires a 'String' but got a"));
    }

    @Test
    void evaluate_in() {
        when(participantAgent.getIdentity()).thenReturn("foo");
        assertThat(validation.evaluate(Operator.IN, List.of("foo", "bar"), participantAgent, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.IN, List.of("foo"), participantAgent, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.IN, List.of("bar"), participantAgent, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.IN, "bar", participantAgent, policyContext)).isFalse();
        assertThat(policyContext.getProblems()).containsOnly("Invalid right-value: operator 'IN' requires a 'List' but got a 'java.lang.String'");
    }

    @Test
    void evaluate_isAnyOf() {
        when(participantAgent.getIdentity()).thenReturn("foo");
        assertThat(validation.evaluate(Operator.IS_ANY_OF, List.of("foo", "bar"), participantAgent, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.IS_ANY_OF, List.of("foo"), participantAgent, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.IS_ANY_OF, List.of("bar"), participantAgent, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.IS_ANY_OF, "bar", participantAgent, policyContext)).isFalse();
        assertThat(policyContext.getProblems()).containsOnly("Invalid right-value: operator 'IS_ANY_OF' requires a 'List' but got a 'java.lang.String'");

    }

    @Test
    void evaluate_isA() {
        when(participantAgent.getIdentity()).thenReturn("foo");
        assertThat(validation.evaluate(Operator.IS_A, List.of("foo", "bar"), participantAgent, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.IS_A, List.of("foo"), participantAgent, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.IS_A, List.of("bar"), participantAgent, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.IS_A, "bar", participantAgent, policyContext)).isFalse();
        assertThat(policyContext.getProblems()).containsOnly("Invalid right-value: operator 'IS_A' requires a 'List' but got a 'java.lang.String'");
    }

    @Test
    void evaluate_isAllOf() {
        when(participantAgent.getIdentity()).thenReturn("foo");
        assertThat(validation.evaluate(Operator.IS_ALL_OF, List.of("foo", "bar"), participantAgent, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.IS_ALL_OF, List.of("foo"), participantAgent, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.IS_ALL_OF, List.of("bar"), participantAgent, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.IS_ALL_OF, "bar", participantAgent, policyContext)).isFalse();
    }

    @Test
    void evaluate_isNoneOf() {
        when(participantAgent.getIdentity()).thenReturn("foo");
        assertThat(validation.evaluate(Operator.IS_NONE_OF, List.of("foo", "bar"), participantAgent, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.IS_NONE_OF, List.of("foo"), participantAgent, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.IS_NONE_OF, List.of("bar"), participantAgent, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.IS_NONE_OF, "bar", participantAgent, policyContext)).isFalse();
        assertThat(policyContext.getProblems()).containsOnly("Invalid right-value: operator 'IS_NONE_OF' requires a 'List' but got a 'java.lang.String'");
    }

    private static class IllegalOperatorProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    Arguments.of(Operator.GEQ),
                    Arguments.of(Operator.GT),
                    Arguments.of(Operator.LEQ),
                    Arguments.of(Operator.LT)
            );
        }
    }
}
