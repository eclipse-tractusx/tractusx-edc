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

package org.eclipse.tractusx.edc.policy.cx.dismantler;

import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.policy.model.Operator.IN;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;
import static org.eclipse.tractusx.edc.policy.cx.CredentialFunctions.createDismantlerCredential;
import static org.eclipse.tractusx.edc.policy.cx.CredentialFunctions.createPcfCredential;
import static org.eclipse.tractusx.edc.policy.cx.CredentialFunctions.createPlainDismantlerCredential;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DismantlerCredentialConstraintFunctionTest {

    private final ParticipantAgent participantAgent = mock();
    private final DismantlerCredentialConstraintFunction<PolicyContext> function = new DismantlerCredentialConstraintFunction<>() {
        @Override
        protected ParticipantAgent getParticipantAgent(PolicyContext context) {
            return participantAgent;
        }
    };
    private final PolicyContext context = mock();

    @Test
    void evaluate_leftOperandInvalid() {
        when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Tatra", "Moskvich").build())));
        assertThat(function.evaluate("foobar", Operator.EQ, "active", null, context)).isFalse();
        verify(context).reportProblem(eq("Invalid left-operand: must be 'Dismantler[.activityType | .allowedBrands ], but was 'foobar'"));
    }

    @Test
    void evaluate_noVcClaimOnParticipantAgent() {
        assertThat(function.evaluate("Dismantler", Operator.EQ, "active", null, context)).isFalse();
        verify(context).reportProblem(eq("ParticipantAgent did not contain a 'vc' claim."));
    }

    @Test
    void evaluate_vcClaimEmpty() {
        when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of()));
        assertThat(function.evaluate("Dismantler", Operator.EQ, "active", null, context)).isFalse();
        verify(context).reportProblem(eq("ParticipantAgent contains a 'vc' claim but it did not contain any VerifiableCredentials."));
    }

    @Test
    void evaluate_vcClaimNotList() {
        when(participantAgent.getClaims()).thenReturn(Map.of("vc", new Object()));
        assertThat(function.evaluate("Dismantler", Operator.EQ, "active", null, context)).isFalse();
        verify(context).reportProblem(eq("ParticipantAgent contains a 'vc' claim, but the type is incorrect. Expected java.util.List, received java.lang.Object."));
    }

    @Nested
    class Active {
        @Test
        void evaluate_eq_satisfied() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Tatra", "Moskvich").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler", Operator.EQ, "active", null, context)).isTrue();
        }

        @Test
        void evaluate_eq_withoutNamespace() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Tatra", "Moskvich").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler", Operator.EQ, "active", null, context)).isTrue();
        }

        @Test
        void evaluate_eq_notSatisfied() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createPcfCredential().build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler", Operator.EQ, "active", null, context)).isFalse();
        }

        @Test
        void evaluate_neq_satisfied() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createPcfCredential().build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler", Operator.NEQ, "active", null, context)).isTrue();
        }

        @Test
        void evaluate_neq_notSatisfied() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Yugo", "Tatra").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler", Operator.NEQ, "active", null, context)).isFalse();
        }

        @Test
        void evaluate_invalidOperators() {
            var invalidOperators = new ArrayList<>(Arrays.asList(Operator.values()));
            invalidOperators.remove(Operator.EQ);
            invalidOperators.remove(Operator.NEQ);
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createPcfCredential().build())));

            assertThat(invalidOperators).allSatisfy(invalidOperator -> assertThat(function.evaluate(CX_POLICY_NS + "Dismantler", invalidOperator, "active", null, context)).isFalse());

        }

        @Test
        void evaluate_rightOperandInvalid() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createPcfCredential().build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler", Operator.EQ, "invalid", null, context)).isFalse();
            verify(context).reportProblem("Right-operand must be equal to 'active', but was 'invalid'");
        }
    }

    @Nested
    class AllowedBrands {

        @DisplayName("Constraint (list) must match credential EXACTLY")
        @Test
        void evaluate_eq_list() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Tatra", "Moskvich").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.EQ, List.of("Tatra", "Moskvich"), null, context)).isTrue();
        }

        @DisplayName("Constraint (list) must match credential EXACTLY")
        @Test
        void evaluate_eq_list_withoutNamespace() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createPlainDismantlerCredential("Tatra", "Moskvich").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.EQ, List.of("Tatra", "Moskvich"), null, context)).isTrue();
        }

        @DisplayName("Constraint (list) must credential EXACTLY - failure")
        @Test
        void evaluate_eq_list_notSatisfied() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Tatra", "Moskvich", "Lada").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.EQ, List.of("Tatra", "Moskvich"), null, context)).isFalse();
            verify(context, never()).reportProblem(anyString());
        }

        @DisplayName("Constraint (scalar) must match credential EXACTLY")
        @Test
        void evaluate_eq_scalar() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Yugo").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.EQ, "Yugo", null, context)).isTrue();
        }

        @DisplayName("Constraint (scalar) must credential EXACTLY - failure")
        @Test
        void evaluate_eq_scalar_notSatisfied() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Tatra").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.EQ, "Yugo", null, context)).isFalse();
            verify(context, never()).reportProblem(anyString());
        }

        @DisplayName("Constraint and credential must be DISJOINT")
        @Test
        void evaluate_neq_satisfied() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Tatra", "Moskvich").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.NEQ, List.of("Lada", "Yugo"), null, context)).isTrue();

            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Tatra", "Moskvich", "Yugo", "Lada").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.NEQ, List.of("Lada", "Yugo"), null, context)).isTrue();
        }

        @DisplayName("Constraint and credential must be DISJOINT - failure")
        @Test
        void evaluate_neq_notSatisfied() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Lada", "Yugo").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.NEQ, List.of("Lada", "Yugo"), null, context)).isFalse();
        }

        @DisplayName("Constraint and credential must INTERSECT")
        @Test
        void evaluate_isAnyOf_satisfied() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Lada").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.IS_ANY_OF, List.of("Tatra", "Moskvich", "Lada"), null, context)).isTrue();

            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Moskvich", "Tatra").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.IS_ANY_OF, List.of("Lada", "Moskvich"), null, context)).isTrue();
        }

        @DisplayName("Constraint and credential must INTERSECT - failure")
        @Test
        void evaluate_isAnyOf_notSatisfied() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Gaz").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.IS_ANY_OF, List.of("Tatra", "Moskvich", "Lada"), null, context)).isFalse();

            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Gaz", "Yugo").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.IS_ANY_OF, List.of("Tatra", "Moskvich", "Lada"), null, context)).isFalse();

            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential().build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.IS_ANY_OF, List.of("Tatra", "Moskvich", "Lada"), null, context)).isFalse();

            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Moskvich", "Tatra").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.IS_ANY_OF, List.of(), null, context)).isFalse();
        }

        @DisplayName("Constraint and credential must NOT INTERSECT")
        @Test
        void evaluate_isNoneOf_satisfied() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Gaz", "Yugo").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.IS_NONE_OF, List.of("Tatra", "Moskvich", "Lada"), null, context)).isTrue();

            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Gaz", "Yugo").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.IS_NONE_OF, List.of(), null, context)).isTrue();

            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential().build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.IS_NONE_OF, List.of("Tatra", "Moskvich", "Lada"), null, context)).isTrue();
        }

        @DisplayName("Constraint and credential must NOT INTERSECT - failure")
        @Test
        void evaluate_isNoneOf_notSatisfied() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Gaz", "Yugo").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.IS_NONE_OF, List.of("Tatra", "Moskvich", "Yugo"), null, context)).isFalse();

        }

        @DisplayName("Brand list from credential must be FULLY CONTAINED within constraint")
        @Test
        void evaluate_in_satisfied() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Gaz", "Yugo").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.allowedBrands", IN, List.of("Gaz", "Moskvich", "Yugo", "Lada"), null, context)).isTrue();

            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Gaz").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.allowedBrands", IN, List.of("Gaz"), null, context)).isTrue();
        }

        @DisplayName("Brand list from credential must be FULLY CONTAINED within constraint - failure")
        @Test
        void evaluate_in_notSatisfied() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Gaz", "Yugo").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.allowedBrands", IN, List.of("Gaz", "Moskvich", "Yugo", "Lada"), null, context)).isTrue();
        }

        @DisplayName("Illegal operator when constraint contains a list value")
        @Test
        void evaluate_illegalOperator_constraintIsList() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Gaz", "Yugo").build())));
            var illegalOp = List.of(Operator.HAS_PART, Operator.GEQ, Operator.LEQ, Operator.GT, Operator.LT, Operator.IS_ALL_OF);

            assertThat(illegalOp).allSatisfy(op -> {
                assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.allowedBrands", op, List.of("Gaz", "Moskvich"), null, context)).isFalse();
                verify(context).reportProblem("Invalid operator: this constraint only allows the following operators: [EQ, NEQ], but received '%s'.".formatted(op));
            });
        }

        @DisplayName("Illegal operator when constraint contains a scalar value")
        @Test
        void evaluate_illegalOperator_constraintIsScalar() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Gaz", "Yugo").build())));

            var invalidOperators = new ArrayList<>(Arrays.asList(Operator.values()));
            invalidOperators.remove(Operator.EQ);
            invalidOperators.remove(Operator.NEQ);

            assertThat(invalidOperators).allSatisfy(op -> {
                assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.allowedBrands", op, "Moskvich", null, context)).isFalse();
                verify(context).reportProblem("Invalid operator: this constraint only allows the following operators: [EQ, NEQ], but received '%s'.".formatted(op));
            });
        }

        @DisplayName("Constraint right-operand has an invalid type")
        @Test
        void evaluate_righOpInvalidType() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential("Gaz", "Yugo").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.allowedBrands", IN, Map.of("foo", "bar"), null, context)).isFalse();
            verify(context).reportProblem(startsWith("Invalid right-operand type: expected String or List, but received:"));
        }
    }

    @Nested
    class ActivityType {
        private final Collection<String> brands = List.of("Tatra", "Yugo");

        @DisplayName("Constraint (list) must match credential EXACTLY")
        @Test
        void evaluate_eq_list() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential(brands, "vehicleDismantle", "vehicleScrap").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.activityType", Operator.EQ, List.of("vehicleDismantle", "vehicleScrap"), null, context)).isTrue();
        }

        @DisplayName("Constraint (list) must match credential EXACTLY")
        @Test
        void evaluate_eq_list_withoutNamespace() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createPlainDismantlerCredential(brands, "vehicleDismantle", "vehicleScrap").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.activityType", Operator.EQ, List.of("vehicleDismantle", "vehicleScrap"), null, context)).isTrue();
        }

        @DisplayName("Constraint (list) must credential EXACTLY - failure")
        @Test
        void evaluate_eq_list_notSatisfied() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential(brands, "vehicleDismantle", "vehicleScrap").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.activityType", Operator.EQ, List.of("vehicleRefurbish"), null, context)).isFalse();
            verify(context, never()).reportProblem(anyString());
        }

        @DisplayName("Constraint (scalar) must match credential EXACTLY")
        @Test
        void evaluate_eq_scalar() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential(brands, "vehicleDismantle").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.activityType", Operator.EQ, "vehicleDismantle", null, context)).isTrue();
        }

        @DisplayName("Constraint (scalar) must credential EXACTLY - failure")
        @Test
        void evaluate_eq_scalar_notSatisfied() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential(brands, "vehicleDismantle").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.activityType", Operator.EQ, "vehicleScrap", null, context)).isFalse();
            verify(context, never()).reportProblem(anyString());
        }

        @DisplayName("Constraint and credential must be DISJOINT")
        @Test
        void evaluate_neq_satisfied() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential(brands, "vehicleDismantle").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.activityType", Operator.NEQ, "vehicleScrap", null, context)).isTrue();

            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential(brands, "vehicleDismantle").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.activityType", Operator.NEQ, List.of("vehicleScrap", "vehicleRefurbish"), null, context)).isTrue();
        }

        @DisplayName("Constraint and credential must be DISJOINT - failure")
        @Test
        void evaluate_neq_notSatisfied() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential(brands, "vehicleDismantle").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.activityType", Operator.NEQ, "vehicleDismantle", null, context)).isFalse();

            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential(brands, "vehicleDismantle").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.activityType", Operator.NEQ, List.of("vehicleDismantle", "vehicleRefurbish"), null, context)).isTrue();
        }

        @DisplayName("Constraint and credential must INTERSECT")
        @Test
        void evaluate_isAnyOf_satisfied() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential(brands, "vehicleDismantle", "vehicleScrap").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.activityType", Operator.IS_ANY_OF, List.of("vehicleDismantle", "vehicleRefurbish"), null, context)).isTrue();
        }

        @DisplayName("Constraint and credential must INTERSECT - failure")
        @Test
        void evaluate_isAnyOf_notSatisfied() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential(brands, "vehicleDismantle", "vehicleScrap").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.activityType", Operator.IS_ANY_OF, List.of("vehicleCrush", "vehicleRefurbish"), null, context)).isFalse();

            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential(brands, "vehicleDismantle", "vehicleScrap").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.activityType", Operator.IS_ANY_OF, List.of(), null, context)).isFalse();

            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential(brands).build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.activityType", Operator.IS_ANY_OF, List.of("vehicleCrush", "vehicleRefurbish"), null, context)).isFalse();
        }

        @DisplayName("Constraint and credential must NOT INTERSECT")
        @Test
        void evaluate_isNoneOf_satisfied() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential(brands, "vehicleDismantle", "vehicleScrap").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.activityType", Operator.IS_NONE_OF, List.of("vehicleRefurbish"), null, context)).isTrue();


            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential(brands, "vehicleDismantle", "vehicleScrap").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.activityType", Operator.IS_NONE_OF, List.of(), null, context)).isTrue();

            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential(brands).build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.activityType", Operator.IS_NONE_OF, List.of("vehicleRefurbish"), null, context)).isTrue();
        }

        @DisplayName("Constraint and credential must NOT INTERSECT - failure")
        @Test
        void evaluate_isNoneOf_notSatisfied() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential(brands, "vehicleDismantle", "vehicleRefurbish").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.activityType", Operator.IS_NONE_OF, List.of("vehicleRefurbish"), null, context)).isFalse();
        }

        @DisplayName("Activity list from credential must be FULLY CONTAINED within constraint")
        @Test
        void evaluate_in_satisfied() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential(brands, "vehicleRefurbish").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.activityType", Operator.IN, List.of("vehicleRefurbish", "vehicleDismantle"), null, context)).isTrue();

            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential(brands, "vehicleDismantle").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.activityType", Operator.IN, List.of("vehicleDismantle"), null, context)).isTrue();
        }

        @DisplayName("Activity list from credential must be FULLY CONTAINED within constraint - failure")
        @Test
        void evaluate_in_notSatisfied() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential(brands, "vehicleRefurbish", "vehicleScrap").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.activityType", Operator.IN, List.of("vehicleRefurbish", "vehicleDismantle"), null, context)).isFalse();

            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential(brands, "vehicleScrap").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.activityType", Operator.IN, List.of("vehicleRefurbish", "vehicleDismantle"), null, context)).isFalse();

        }

        @DisplayName("Illegal operator when constraint contains a list value")
        @Test
        void evaluate_illegalOperator_constraintIsList() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential(brands, "vehicleDismantle").build())));
            var illegalOp = List.of(Operator.HAS_PART, Operator.GEQ, Operator.LEQ, Operator.GT, Operator.LT, Operator.IS_ALL_OF);

            assertThat(illegalOp).allSatisfy(op -> {
                assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.activityType", op, List.of("vehicleDismantle"), null, context)).isFalse();
            });
        }

        @DisplayName("Illegal operator when constraint contains a scalar value")
        @Test
        void evaluate_illegalOperator_constraintIsScalar() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential(brands, "vehicleDismantle").build())));

            var invalidOperators = new ArrayList<>(Arrays.asList(Operator.values()));
            invalidOperators.remove(Operator.EQ);
            invalidOperators.remove(Operator.NEQ);

            assertThat(invalidOperators).allSatisfy(op -> {
                assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.activityType", op, "vehicleDismantle", null, context)).isFalse();
                verify(context).reportProblem("Invalid operator: this constraint only allows the following operators: [EQ, NEQ], but received '%s'.".formatted(op));
            });
        }

        @DisplayName("Constraint right-operand has an invalid type")
        @Test
        void evaluate_righOpInvalidType() {
            when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(createDismantlerCredential(brands, "vehicleDismantle").build())));
            assertThat(function.evaluate(CX_POLICY_NS + "Dismantler.activityType", IN, Map.of("foo", "bar"), null, context)).isFalse();
            verify(context).reportProblem(startsWith("Invalid right-operand type: expected String or List, but received:"));
        }
    }
}
