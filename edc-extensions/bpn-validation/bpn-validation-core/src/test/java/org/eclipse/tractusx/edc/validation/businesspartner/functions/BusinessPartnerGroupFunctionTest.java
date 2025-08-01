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

import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.store.BusinessPartnerStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.List;
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
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BusinessPartnerGroupFunctionTest {

    public static final String TEST_GROUP_1 = "test-group-1";
    public static final String TEST_GROUP_2 = "test-group-2";
    private static final String TEST_BPN = "BPN000TEST";
    private final BusinessPartnerStore store = mock();
    private final ParticipantAgent agent = mock();
    private final Monitor monitor = mock();
    private final Permission unusedPermission = Permission.Builder.newInstance().build();
    private final ParticipantAgentPolicyContext context = new TestParticipantAgentPolicyContext(agent);
    private final BusinessPartnerGroupFunction<ParticipantAgentPolicyContext> function = new BusinessPartnerGroupFunction<>(store, monitor);

    @ParameterizedTest(name = "Invalid operator {0}")
    @ArgumentsSource(InvalidOperatorProvider.class)
    @DisplayName("Invalid operators, expect report in policy context")
    void evaluate_invalidOperator(Operator invalidOperator) {

        var result = function.evaluate(invalidOperator, "test-group", unusedPermission, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).hasSize(1).anyMatch(it -> it.endsWith("but was [" + invalidOperator.name() + "]"));
    }

    @ParameterizedTest
    @ArgumentsSource(RightOperandNotStringNorCollection.class)
    @DisplayName("Right-hand operand is not String or Collection<?>")
    void evaluate_rightOperandNotStringOrCollection(Object rightValue) {
        when(store.resolveForBpn(TEST_BPN)).thenReturn(StoreResult.success(List.of("test-group")));
        when(agent.getIdentity()).thenReturn(TEST_BPN);

        var result = function.evaluate(IS_ANY_OF, rightValue, unusedPermission, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).containsOnly("Right operand expected to be either String or a Collection, but was " + rightValue.getClass());
    }

    @ParameterizedTest(name = "{1} :: {0}")
    @ArgumentsSource(ValidOperatorProvider.class)
    @DisplayName("Valid operators, evaluating different circumstances")
    void evaluate_validOperator(String ignored, Operator operator, List<String> assignedBpn, boolean expectedOutcome) {
        var allowedGroups = List.of(TEST_GROUP_1, TEST_GROUP_2);
        when(store.resolveForBpn(TEST_BPN)).thenReturn(StoreResult.success(assignedBpn));
        when(agent.getIdentity()).thenReturn(TEST_BPN);

        var result = function.evaluate(operator, allowedGroups, unusedPermission, context);

        assertThat(result).isEqualTo(expectedOutcome);
    }

    @Test
    void evaluate_failedResolveForBpn_shouldBeFalse() {
        var allowedGroups = List.of(TEST_GROUP_1, TEST_GROUP_2);
        when(agent.getIdentity()).thenReturn(TEST_BPN);
        when(store.resolveForBpn(TEST_BPN)).thenReturn(StoreResult.notFound("foobar"));

        var result = function.evaluate(IS_ANY_OF, allowedGroups, unusedPermission, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).containsOnly("foobar");
    }

    @ArgumentsSource(OperatorForEmptyGroupsProvider.class)
    @ParameterizedTest
    void evaluate_groupsAssignedButNoGroupsSentToEvaluate(Operator operator, List<String> assignedBpnGroups,
                                                          boolean expectedOutcome) {
        List<String> allowedGroups = List.of();
        when(agent.getIdentity()).thenReturn(TEST_BPN);
        when(store.resolveForBpn(TEST_BPN)).thenReturn(StoreResult.success(assignedBpnGroups));

        var result = function.evaluate(operator, allowedGroups, unusedPermission, context);

        assertThat(result).isEqualTo(expectedOutcome);
    }

    private static class InvalidOperatorProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
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
                    Arguments.of("Overlapping groups", IN, List.of(TEST_GROUP_1, "different-group"), true),
                    Arguments.of("Disjoint groups", IN, List.of("different-group", "another-different-group"), false),

                    Arguments.of("Disjoint groups", IS_ALL_OF, List.of("different-group", "another-different-group"), false),
                    Arguments.of("Matching groups", IS_ALL_OF, List.of(TEST_GROUP_1, TEST_GROUP_2), true),
                    Arguments.of("Overlapping groups", IS_ALL_OF, List.of(TEST_GROUP_1, TEST_GROUP_2, "different-group", "another-different-group"), true),
                    Arguments.of("Overlapping groups (1 overlap)", IS_ALL_OF, List.of(TEST_GROUP_1, "different-group"), false),
                    Arguments.of("Overlapping groups (1 overlap)", IS_ALL_OF, List.of(TEST_GROUP_1), false),

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

    private static class RightOperandNotStringNorCollection implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    arguments(42),
                    arguments(42L),
                    arguments(true),
                    arguments(new Object())
            );
        }
    }

}
