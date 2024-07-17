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
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BusinessPartnerNumberPermissionFunctionTest {

    private final Permission permission = mock();
    private BusinessPartnerNumberPermissionFunction validation;
    private PolicyContext policyContext;
    private ParticipantAgent participantAgent;

    @BeforeEach
    void beforeEach() {
        this.policyContext = mock(PolicyContext.class);
        this.participantAgent = mock(ParticipantAgent.class);

        when(policyContext.getContextData(eq(ParticipantAgent.class))).thenReturn(participantAgent);

        validation = new BusinessPartnerNumberPermissionFunction() {
        };
    }

    @ParameterizedTest(name = "Illegal Operator {0}")
    @ArgumentsSource(IllegalOperatorProvider.class)
    void testFailsOnUnsupportedOperations(Operator illegalOperator) {
        assertFalse(validation.evaluate(illegalOperator, "foo", permission, policyContext));
        verify(policyContext).reportProblem(argThat(message -> message.startsWith("Operator %s is not supported.".formatted(illegalOperator))));
    }

    @Test
    void testFailsOnUnsupportedRightValue() {
        when(participantAgent.getIdentity()).thenReturn("foo");
        assertFalse(validation.evaluate(Operator.EQ, 1, permission, policyContext));
        verify(policyContext).reportProblem(argThat(message -> message.contains("Invalid right-value: operator 'EQ' requires a 'String' or a 'List' but got a 'java.lang.Integer'")));
    }

    @Test
    void testValidationFailsIdentityIsMissing() {
        assertThat(validation.evaluate(Operator.EQ, "foo", permission, policyContext)).isFalse();
        verify(policyContext).reportProblem(argThat(message -> message.contains("Identity of the participant agent cannot be null")));
    }

    @Test
    void testValidationFailsParticipantAgentMissing() {
        var context = mock(PolicyContext.class);
        assertThat(validation.evaluate(Operator.EQ, "foo", permission, context)).isFalse();
        verify(context).reportProblem(argThat(message -> message.contains("Required PolicyContext data not found")));
    }

    @Test
    void testValidationWhenSingleParticipantIsValid() {
        when(participantAgent.getIdentity()).thenReturn("foo");
        assertThat(validation.evaluate(Operator.EQ, "foo", permission, policyContext)).isTrue();
    }

    @Test
    void testValidationFailsInvalidIdentity() {
        when(participantAgent.getIdentity()).thenReturn("bar");
        assertThat(validation.evaluate(Operator.EQ, "foo", permission, policyContext)).isFalse();
    }

    @Test
    void testValidationForMultipleParticipants() {
        when(participantAgent.getIdentity()).thenReturn("quazz");
        assertThat(validation.evaluate(Operator.IN, List.of("foo", "bar"), permission, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.IN, List.of(1, "foo"), permission, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.IN, List.of("bar", "bar"), permission, policyContext)).isFalse();
    }

    @Test
    void evaluate_neq() {
        when(participantAgent.getIdentity()).thenReturn("foo");
        assertThat(validation.evaluate(Operator.NEQ, "bar", permission, policyContext)).isTrue();

        // these two should report a problem
        assertThat(validation.evaluate(Operator.NEQ, 1, permission, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.NEQ, List.of("foo", "bar"), permission, policyContext)).isTrue();
    }

    @Test
    void evaluate_hasPart() {
        when(participantAgent.getIdentity()).thenReturn("quizzquazz");
        assertThat(validation.evaluate(Operator.HAS_PART, "quizz", permission, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.HAS_PART, "quazz", permission, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.HAS_PART, "zzqua", permission, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.HAS_PART, "zzqui", permission, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.HAS_PART, "Quizz", permission, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.HAS_PART, List.of("quizz"), permission, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.HAS_PART, List.of("quizz", "quazz"), permission, policyContext)).isFalse();
        verify(policyContext, times(2)).reportProblem(startsWith("Invalid right-value: operator 'HAS_PART' requires a 'String' but got a"));
    }

    @Test
    void evaluate_in() {
        when(participantAgent.getIdentity()).thenReturn("foo");
        assertThat(validation.evaluate(Operator.IN, List.of("foo", "bar"), permission, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.IN, List.of("foo"), permission, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.IN, List.of("bar"), permission, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.IN, "bar", permission, policyContext)).isFalse();
        verify(policyContext).reportProblem("Invalid right-value: operator 'IN' requires a 'List' but got a 'java.lang.String'");
    }

    @Test
    void evaluate_isAnyOf() {
        when(participantAgent.getIdentity()).thenReturn("foo");
        assertThat(validation.evaluate(Operator.IS_ANY_OF, List.of("foo", "bar"), permission, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.IS_ANY_OF, List.of("foo"), permission, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.IS_ANY_OF, List.of("bar"), permission, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.IS_ANY_OF, "bar", permission, policyContext)).isFalse();
        verify(policyContext).reportProblem("Invalid right-value: operator 'IS_ANY_OF' requires a 'List' but got a 'java.lang.String'");

    }

    @Test
    void evaluate_isA() {
        when(participantAgent.getIdentity()).thenReturn("foo");
        assertThat(validation.evaluate(Operator.IS_A, List.of("foo", "bar"), permission, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.IS_A, List.of("foo"), permission, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.IS_A, List.of("bar"), permission, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.IS_A, "bar", permission, policyContext)).isFalse();
        verify(policyContext).reportProblem("Invalid right-value: operator 'IS_A' requires a 'List' but got a 'java.lang.String'");

    }

    @Test
    void evaluate_isAllOf() {
        when(participantAgent.getIdentity()).thenReturn("foo");
        assertThat(validation.evaluate(Operator.IS_ALL_OF, List.of("foo", "bar"), permission, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.IS_ALL_OF, List.of("foo"), permission, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.IS_ALL_OF, List.of("bar"), permission, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.IS_ALL_OF, "bar", permission, policyContext)).isFalse();
    }

    @Test
    void evaluate_isNoneOf() {
        when(participantAgent.getIdentity()).thenReturn("foo");
        assertThat(validation.evaluate(Operator.IS_NONE_OF, List.of("foo", "bar"), permission, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.IS_NONE_OF, List.of("foo"), permission, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.IS_NONE_OF, List.of("bar"), permission, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.IS_NONE_OF, "bar", permission, policyContext)).isFalse();
        verify(policyContext).reportProblem("Invalid right-value: operator 'IS_NONE_OF' requires a 'List' but got a 'java.lang.String'");
    }

    private static class IllegalOperatorProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
            return Stream.of(
                    Arguments.of(Operator.GEQ),
                    Arguments.of(Operator.GT),
                    Arguments.of(Operator.LEQ),
                    Arguments.of(Operator.LT)
            );
        }
    }
}
