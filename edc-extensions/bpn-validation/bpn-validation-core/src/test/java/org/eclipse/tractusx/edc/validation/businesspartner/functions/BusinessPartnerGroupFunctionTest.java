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
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.BusinessPartnerStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.policy.model.Operator.EQ;
import static org.eclipse.edc.policy.model.Operator.GEQ;
import static org.eclipse.edc.policy.model.Operator.GT;
import static org.eclipse.edc.policy.model.Operator.HAS_PART;
import static org.eclipse.edc.policy.model.Operator.IN;
import static org.eclipse.edc.policy.model.Operator.IS_A;
import static org.eclipse.edc.policy.model.Operator.IS_ALL_OF;
import static org.eclipse.edc.policy.model.Operator.IS_ANY_OF;
import static org.eclipse.edc.policy.model.Operator.IS_NONE_OF;
import static org.eclipse.edc.policy.model.Operator.LEQ;
import static org.eclipse.edc.policy.model.Operator.LT;
import static org.eclipse.edc.policy.model.Operator.NEQ;
import static org.eclipse.edc.spi.agent.ParticipantAgent.PARTICIPANT_IDENTITY;
import static org.eclipse.tractusx.edc.validation.businesspartner.functions.BusinessPartnerGroupFunction.BUSINESS_PARTNER_CONSTRAINT_KEY;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BusinessPartnerGroupFunctionTest {

    public static final String TEST_GROUP_1 = "test-group-1";
    public static final String TEST_GROUP_2 = "test-group-2";
    private static final String TEST_BPN = "BPN000TEST";
    private final PolicyContext context = mock();
    private BusinessPartnerGroupFunction function;
    private BusinessPartnerStore store;

    @BeforeEach
    void setUp() {
        store = mock();
        function = new BusinessPartnerGroupFunction(store);
    }

    @Test
    @DisplayName("PolicyContext does not carry ParticipantAgent")
    void evaluate_noParticipantAgentOnContext() {
        reset(context);
        assertThat(function.evaluate(EQ, "test-group", createPermission(EQ, List.of()), context)).isFalse();
        verify(context).reportProblem(eq("ParticipantAgent not found on PolicyContext"));
    }

    @ParameterizedTest(name = "Invalid operator {0}")
    @ArgumentsSource(InvalidOperatorProvider.class)
    @DisplayName("Invalid operators, expect report in policy context")
    void evaluate_invalidOperator(Operator invalidOperator) {
        when(context.getContextData(eq(ParticipantAgent.class))).thenReturn(new ParticipantAgent(Map.of(), Map.of()));
        assertThat(function.evaluate(invalidOperator, "test-group", createPermission(invalidOperator, List.of()), context)).isFalse();
        verify(context).reportProblem(endsWith("but was [" + invalidOperator.name() + "]"));
    }

    @Test
    @DisplayName("Right-hand operand is not String or Collection<?>")
    void evaluate_rightOperandNotStringOrCollection() {
        when(store.resolveForBpn(TEST_BPN)).thenReturn(StoreResult.success(List.of("test-group")));
        when(context.getContextData(eq(ParticipantAgent.class))).thenReturn(new ParticipantAgent(Map.of(), Map.of(PARTICIPANT_IDENTITY, TEST_BPN)));

        assertThat(function.evaluate(EQ, 42, createPermission(EQ, List.of("test-group")), context)).isFalse();
        assertThat(function.evaluate(EQ, 42L, createPermission(EQ, List.of("test-group")), context)).isFalse();
        assertThat(function.evaluate(EQ, true, createPermission(EQ, List.of("test-group")), context)).isFalse();
        assertThat(function.evaluate(EQ, new Object(), createPermission(EQ, List.of("test-group")), context)).isFalse();

        verify(context).reportProblem("Right operand expected to be either String or a Collection, but was " + Integer.class);
        verify(context).reportProblem("Right operand expected to be either String or a Collection, but was " + Long.class);
        verify(context).reportProblem("Right operand expected to be either String or a Collection, but was " + Boolean.class);
        verify(context).reportProblem("Right operand expected to be either String or a Collection, but was " + Object.class);
    }

    @ParameterizedTest(name = "{1} :: {0}")
    @ArgumentsSource(ValidOperatorProvider.class)
    @DisplayName("Valid operators, evaluating different circumstances")
    void evaluate_validOperator(String ignored, Operator operator, List<String> assignedBpn, boolean expectedOutcome) {
        var allowedGroups = List.of(TEST_GROUP_1, TEST_GROUP_2);
        when(context.getContextData(eq(ParticipantAgent.class))).thenReturn(new ParticipantAgent(Map.of(), Map.of(PARTICIPANT_IDENTITY, TEST_BPN)));
        when(store.resolveForBpn(TEST_BPN)).thenReturn(StoreResult.success(assignedBpn));
        assertThat(function.evaluate(operator, allowedGroups, createPermission(operator, allowedGroups), context)).isEqualTo(expectedOutcome);
    }

    @Test
    void evaluate_failedResolveForBpn_shouldBeFalse() {
        var allowedGroups = List.of(TEST_GROUP_1, TEST_GROUP_2);
        var operator = EQ;
        when(context.getContextData(eq(ParticipantAgent.class))).thenReturn(new ParticipantAgent(Map.of(), Map.of(PARTICIPANT_IDENTITY, TEST_BPN)));
        when(store.resolveForBpn(TEST_BPN)).thenReturn(StoreResult.notFound("foobar"));

        assertThat(function.evaluate(operator, allowedGroups, createPermission(operator, allowedGroups), context)).isFalse();
        verify(context).reportProblem("foobar");
    }

    @ArgumentsSource(OperatorForEmptyGroupsProvider.class)
    @ParameterizedTest
    void evaluate_groupsAssignedButNoGroupsSentToEvaluate(Operator operator, List<String> assignedBpnGroups,
                                                          boolean expectedOutcome) {
        List<String> allowedGroups = List.of();

        when(context.getContextData(eq(ParticipantAgent.class))).thenReturn(new ParticipantAgent(Map.of(), Map.of(PARTICIPANT_IDENTITY, TEST_BPN)));
        when(store.resolveForBpn(TEST_BPN)).thenReturn(StoreResult.success(assignedBpnGroups));

        assertThat(function.evaluate(operator, allowedGroups, createPermission(operator, allowedGroups), context)).isEqualTo(expectedOutcome);
    }

    private Permission createPermission(Operator op, List<String> rightOperand) {
        return Permission.Builder.newInstance()
                .constraint(AtomicConstraint.Builder.newInstance()
                        .leftExpression(new LiteralExpression(BUSINESS_PARTNER_CONSTRAINT_KEY))
                        .operator(op)
                        .rightExpression(new LiteralExpression(rightOperand)).build())
                .build();
    }

    private static class InvalidOperatorProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
            return Stream.of(
                    Arguments.of(GEQ),
                    Arguments.of(GT),
                    Arguments.of(HAS_PART),
                    Arguments.of(LT),
                    Arguments.of(LEQ),
                    Arguments.of(IS_A)
            );
        }
    }

    private static class ValidOperatorProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    Arguments.of("Matching groups", EQ, List.of(TEST_GROUP_1, TEST_GROUP_2), true),
                    Arguments.of("Disjoint groups", EQ, List.of("test-group", "different-group"), false),
                    Arguments.of("Overlapping groups", EQ, List.of("different-group"), false),

                    Arguments.of("Disjoint groups", NEQ, List.of("different-group", "another-different-group"), true),
                    Arguments.of("Overlapping groups", NEQ, List.of(TEST_GROUP_1, "different-group"), true),
                    Arguments.of("Matching groups", NEQ, List.of(TEST_GROUP_1, TEST_GROUP_2), false),
                    Arguments.of("Empty groups", NEQ, List.of(), true),

                    Arguments.of("Matching groups", IN, List.of(TEST_GROUP_1, TEST_GROUP_2), true),
                    Arguments.of("Overlapping groups", IN, List.of(TEST_GROUP_1, "different-group"), false),
                    Arguments.of("Disjoint groups", IN, List.of("different-group", "another-different-group"), false),

                    Arguments.of("Disjoint groups", IS_ALL_OF, List.of("different-group", "another-different-group"), false),
                    Arguments.of("Matching groups", IS_ALL_OF, List.of(TEST_GROUP_1, TEST_GROUP_2), true),
                    Arguments.of("Overlapping groups", IS_ALL_OF, List.of(TEST_GROUP_1, TEST_GROUP_2, "different-group", "another-different-group"), false),
                    Arguments.of("Overlapping groups (1 overlap)", IS_ALL_OF, List.of(TEST_GROUP_1, "different-group"), false),
                    Arguments.of("Overlapping groups (1 overlap)", IS_ALL_OF, List.of(TEST_GROUP_1), true),

                    Arguments.of("Disjoint groups", IS_ANY_OF, List.of("different-group", "another-different-group"), false),
                    Arguments.of("Matching groups", IS_ANY_OF, List.of(TEST_GROUP_1, TEST_GROUP_2), true),
                    Arguments.of("Overlapping groups (1 overlap)", IS_ANY_OF, List.of(TEST_GROUP_1, "different-group", "another-different-group"), true),
                    Arguments.of("Overlapping groups (2 overlap)", IS_ANY_OF, List.of(TEST_GROUP_1, TEST_GROUP_2, "different-group", "another-different-group"), true),

                    Arguments.of("Disjoint groups", IS_NONE_OF, List.of("different-group", "another-different-group"), true),
                    Arguments.of("Matching groups", IS_NONE_OF, List.of(TEST_GROUP_1, TEST_GROUP_2), false),
                    Arguments.of("Overlapping groups", IS_NONE_OF, List.of(TEST_GROUP_1, "another-different-group"), false),
                    Arguments.of("Empty groups", IS_NONE_OF, List.of(), true)
            );
        }
    }

    private static class OperatorForEmptyGroupsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            var assignedBpnGroups = List.of(TEST_GROUP_1, TEST_GROUP_2);

            return Stream.of(
                    Arguments.of(EQ, assignedBpnGroups, false),
                    Arguments.of(EQ, List.of(), true),
                    Arguments.of(NEQ, assignedBpnGroups, true),
                    Arguments.of(NEQ, List.of(), false),
                    Arguments.of(IN, assignedBpnGroups, false),
                    Arguments.of(IN, List.of(), true),
                    Arguments.of(IS_ALL_OF, assignedBpnGroups, false),
                    Arguments.of(IS_ALL_OF, List.of(), true),
                    Arguments.of(IS_ANY_OF, assignedBpnGroups, false),
                    Arguments.of(IS_ANY_OF, List.of(), true),
                    Arguments.of(IS_NONE_OF, assignedBpnGroups, true),
                    Arguments.of(IS_NONE_OF, List.of(), false)
            );
        }
    }
}
