/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2025 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.policy.cx.legacy.framework;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.tractusx.edc.policy.cx.legacy.CredentialFunctions;
import org.eclipse.tractusx.edc.policy.cx.legacy.TestParticipantAgentPolicyContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FrameworkAgreementConstraintFunctionTest {
    private final ParticipantAgent participantAgent = mock();
    private final FrameworkAgreementConstraintFunction<ParticipantAgentPolicyContext> function = new FrameworkAgreementConstraintFunction<>();
    private final ParticipantAgentPolicyContext context = new TestParticipantAgentPolicyContext(participantAgent);

    @Test
    void evaluate_leftOperandInvalid() {
        var result = function.evaluate("ThisIsInvalid", Operator.EQ, "irrelevant", null, context);

        assertThat(result).isFalse();

        assertThat(context.getProblems()).containsOnly("Constraint left-operand must start with 'FrameworkAgreement' but was 'ThisIsInvalid'.");
    }

    @Test
    void evaluate_invalidOperator() {
        var result = function.evaluate(CX_POLICY_NS + "FrameworkAgreement.foobar", Operator.HAS_PART, "irrelevant", null, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).containsOnly("Invalid operator: this constraint only allows the following operators: [EQ, NEQ], but received 'HAS_PART'.");
    }

    @Test
    void evaluate_vcClaimNotPresent() {
        when(participantAgent.getClaims()).thenReturn(Map.of());

        var result = function.evaluate(CX_POLICY_NS + "FrameworkAgreement.foobar", Operator.EQ, "active", null, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).containsOnly("ParticipantAgent did not contain a 'vc' claim.");
    }

    @Test
    void evaluate_vcClaimNotListOfCredentials() {
        when(participantAgent.getClaims()).thenReturn(Map.of(
                "vc", new Object()
        ));

        var result = function.evaluate(CX_POLICY_NS + "FrameworkAgreement.foobar", Operator.EQ, "active:0.0.1", null, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).containsOnly("ParticipantAgent contains a 'vc' claim, but the type is incorrect. Expected java.util.List, received java.lang.Object.");
    }

    @Test
    void evaluate_vcClaimCredentialsEmpty() {
        when(participantAgent.getClaims()).thenReturn(Map.of(
                "vc", new ArrayList<VerifiableCredential>()
        ));

        var result = function.evaluate(CX_POLICY_NS + "FrameworkAgreement.foobar", Operator.EQ, "active", null, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).containsOnly("ParticipantAgent contains a 'vc' claim but it did not contain any VerifiableCredentials.");
    }

    @Test
    void evaluate_rightOperandInvalidFormat() {
        when(participantAgent.getClaims()).thenReturn(Map.of(
                "vc", List.of(CredentialFunctions.createDataExchangeGovernanceCredential().build())
        ));

        var result = function.evaluate(CX_POLICY_NS + "FrameworkAgreement.dataExchangeGovernance", Operator.EQ, "/violate$", null, context);

        assertThat(result).isFalse();
        assertThat(context.getProblems()).containsOnly("Right-operand must contain the keyword 'active' followed by an optional version string: 'active'[:version], but was '/violate$'.");
    }

    @Test
    void evaluate_requiredCredentialNotFound() {
        when(participantAgent.getClaims()).thenReturn(Map.of(
                "vc", List.of(
                        CredentialFunctions.createCredential(CX_POLICY_NS + "DataExchangeGovernanceCredential", "1.3.0").build(),
                        CredentialFunctions.createCredential(CX_POLICY_NS + "DataExchangeGovernanceCredential", "1.0.0").build())
        ));

        var result = function.evaluate("FrameworkAgreement", Operator.EQ, "someOther:1.3.0", null, context);

        assertThat(result).isFalse();
    }

    @Test
    void evaluate_requiredCredential_wrongVersion() {
        when(participantAgent.getClaims()).thenReturn(Map.of(
                "vc", List.of(
                        CredentialFunctions.createCredential("SomeOtherCredential", "2.0.0").build(),
                        CredentialFunctions.createCredential("DataExchangeGovernanceCredential", "1.8.0").build(),
                        CredentialFunctions.createCredential("DataExchangeGovernanceCredential", "1.0.0").build())
        ));

        var result = function.evaluate(CX_POLICY_NS + "FrameworkAgreement", Operator.EQ, "dataExchangeGovernance:1.3.0", null, context);

        assertThat(result).isFalse();
    }

    @Test
    void evaluate_requiredCredentialFound() {
        when(participantAgent.getClaims()).thenReturn(Map.of(
                "vc", List.of(
                        CredentialFunctions.createCredential("DataExchangeGovernanceCredential", "6.0.0").build(),
                        CredentialFunctions.createCredential("SomeOtherType", "3.4.1").build()
                )
        ));

        var result = function.evaluate(CX_POLICY_NS + "FrameworkAgreement", Operator.EQ, "dataExchangeGovernance", null, context);

        assertThat(result).isTrue();
    }

    @Test
    void evaluate_requiredCredentialFound_withCorrectVersion() {
        when(participantAgent.getClaims()).thenReturn(Map.of(
                "vc", List.of(
                        CredentialFunctions.createCredential("DataExchangeGovernanceCredential", "2.0.0").build(),
                        CredentialFunctions.createCredential("DataExchangeGovernanceCredential", "1.3.0").build(),
                        CredentialFunctions.createCredential("DataExchangeGovernanceCredential", "1.0.0").build())
        ));

        var result = function.evaluate(CX_POLICY_NS + "FrameworkAgreement", Operator.EQ, "dataExchangeGovernance:1.3.0", null, context);

        assertThat(result).isTrue();
    }

    @Test
    void evaluate_neq_requiredCredentialFound() {
        when(participantAgent.getClaims()).thenReturn(Map.of(
                "vc", List.of(
                        CredentialFunctions.createCredential("DataExchangeGovernanceCredential", "6.0.0").build(),
                        CredentialFunctions.createCredential("SomeOtherType", "3.4.1").build()
                )
        ));

        var result = function.evaluate(CX_POLICY_NS + "FrameworkAgreement", Operator.NEQ, "sustainability", null, context);

        assertThat(result).isTrue();
    }

    @Test
    void evaluate_neq_oneOfManyViolates() {
        when(participantAgent.getClaims()).thenReturn(Map.of(
                "vc", List.of(
                        CredentialFunctions.createCredential("DataExchangeGovernanceCredential", "6.0.0").build(),
                        CredentialFunctions.createCredential("SustainabilityCredential", "6.0.0").build(),
                        CredentialFunctions.createCredential("SomeOtherType", "3.4.1").build()
                )
        ));

        var result = function.evaluate(CX_POLICY_NS + "FrameworkAgreement", Operator.NEQ, "sustainability", null, context);

        assertThat(result).isTrue();
    }

    @Test
    void evaluate_neq_oneViolates() {
        when(participantAgent.getClaims()).thenReturn(Map.of(
                "vc", List.of(
                        CredentialFunctions.createCredential("SustainabilityCredential", "6.0.0").build()
                )
        ));

        var result = function.evaluate(CX_POLICY_NS + "FrameworkAgreement", Operator.NEQ, "sustainability", null, context);

        assertThat(result).isFalse();
    }

    @Test
    void evaluate_neq_requiredCredentialFound_withCorrectVersion() {
        when(participantAgent.getClaims()).thenReturn(Map.of(
                "vc", List.of(
                        CredentialFunctions.createCredential("DataExchangeGovernanceCredential", "2.0.0").build(),
                        CredentialFunctions.createCredential("FooBarCredential", "1.3.0").build(),
                        CredentialFunctions.createCredential("BarBazCredential", "1.0.0").build())
        ));

        var result = function.evaluate(CX_POLICY_NS + "FrameworkAgreement", Operator.NEQ, "sustainability:1.3.0", null, context);

        assertThat(result).isTrue();
    }


    @Nested
    class LegacyLeftOperand {
        @Test
        void evaluate_leftOperand_notContainSubtype() {
            when(participantAgent.getClaims()).thenReturn(Map.of(
                    "vc", List.of(CredentialFunctions.createDataExchangeGovernanceCredential().build())
            ));

            var result = function.evaluate(CX_POLICY_NS + "FrameworkAgreement.", Operator.EQ, "active:0.4.2", null, context);

            assertThat(result).isFalse();
            assertThat(context.getProblems()).containsOnly("Left-operand must contain the sub-type 'FrameworkAgreement.<subtype>'.");
        }

        @Test
        void evaluate_leftOperand_notContainFrameworkLiteral() {
            when(participantAgent.getClaims()).thenReturn(Map.of(
                    "vc", List.of(CredentialFunctions.createDataExchangeGovernanceCredential().build())
            ));

            var result = function.evaluate(CX_POLICY_NS + "foobar.dataExchangeGovernance", Operator.EQ, "active:0.4.2", null, context);

            assertThat(result).isFalse();
            assertThat(context.getProblems()).hasSize(1).allMatch(it -> it.startsWith("Constraint left-operand must start with 'FrameworkAgreement' but was"));
        }

        @Test
        void evaluate_leftOperand_notStartsWithFrameworkLiteral() {
            when(participantAgent.getClaims()).thenReturn(Map.of(
                    "vc", List.of(CredentialFunctions.createDataExchangeGovernanceCredential().build())
            ));

            var result = function.evaluate(CX_POLICY_NS + "foobarFrameworkAgreement.dataExchangeGovernance", Operator.EQ, "active:0.4.2", null, context);

            assertThat(result).isFalse();
            assertThat(context.getProblems()).hasSize(1).allMatch(it -> it.startsWith("Constraint left-operand must start with 'FrameworkAgreement' but was"));
        }

        @Test
        void evaluate_rightOperand_notStartWithActiveLiteral() {
            when(participantAgent.getClaims()).thenReturn(Map.of(
                    "vc", List.of(CredentialFunctions.createDataExchangeGovernanceCredential().build())
            ));

            var result = function.evaluate(CX_POLICY_NS + "FrameworkAgreement.dataExchangeGovernance", Operator.EQ, "violates", null, context);

            assertThat(result).isFalse();
            assertThat(context.getProblems()).containsOnly("Right-operand must contain the keyword 'active' followed by an optional version string: 'active'[:version], but was 'violates'.");
        }

        @Test
        void evaluate_rightOperandWithVersion() {
            when(participantAgent.getClaims()).thenReturn(Map.of(
                    "vc", List.of(CredentialFunctions.createDataExchangeGovernanceCredential().build())
            ));
            assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement.dataExchangeGovernance", Operator.EQ, "active:1.0.0", null, context)).isTrue();
            assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement.dataExchangeGovernance", Operator.EQ, "active:5.3.1", null, context)).isFalse();
        }

        @Test
        void evaluate_rightOperand() {
            when(participantAgent.getClaims()).thenReturn(Map.of(
                    "vc", List.of(CredentialFunctions.createDataExchangeGovernanceCredential().build())
            ));

            var result = function.evaluate(CX_POLICY_NS + "FrameworkAgreement.dataExchangeGovernance", Operator.EQ, "active", null, context);

            assertThat(result).isTrue();
        }
    }

    @Nested
    class NewLeftOperand {
        @Test
        void evaluate_leftOperandNotFrameworkLiteral() {
            when(participantAgent.getClaims()).thenReturn(Map.of(
                    "vc", List.of(CredentialFunctions.createDataExchangeGovernanceCredential().build())
            ));

            var result = function.evaluate("Foobar", Operator.EQ, "active:0.4.2", null, context);

            assertThat(result).isFalse();
            assertThat(context.getProblems()).containsOnly("Constraint left-operand must start with 'FrameworkAgreement' but was 'Foobar'.");
        }

        @Test
        void evaluate_rightOperand_onlySubtype() {
            when(participantAgent.getClaims()).thenReturn(Map.of(
                    "vc", List.of(CredentialFunctions.createDataExchangeGovernanceCredential().build())
            ));

            var result = function.evaluate(CX_POLICY_NS + "FrameworkAgreement", Operator.EQ, "DataExchangeGovernance", null, context);

            assertThat(result).isTrue();
        }

        @Test
        void evaluate_rightOperand_withVersion() {
            when(participantAgent.getClaims()).thenReturn(Map.of(
                    "vc", List.of(CredentialFunctions.createDataExchangeGovernanceCredential().build())
            ));

            var result = function.evaluate(CX_POLICY_NS + "FrameworkAgreement", Operator.EQ, "DataExchangeGovernance:1.0.0", null, context);

            assertThat(result).isTrue();
            assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement", Operator.EQ, "DataExchangeGovernance:4.2.0", null, context)).isFalse();
        }

        @Test
        void evaluate_withoutNamespace() {
            when(participantAgent.getClaims()).thenReturn(Map.of(
                    "vc", List.of(CredentialFunctions.createPlainDataExchangeGovernanceCredential().build())
            ));
            assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement", Operator.EQ, "DataExchangeGovernance:1.0.0", null, context)).isTrue();
            assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement", Operator.EQ, "DataExchangeGovernance:4.2.0", null, context)).isFalse();
        }

        @Test
        void evaluate_rightOperandMissesSubtype() {
            when(participantAgent.getClaims()).thenReturn(Map.of(
                    "vc", List.of(CredentialFunctions.createMembershipCredential().build())
            ));

            var result = function.evaluate("FrameworkAgreement", Operator.EQ, ":1.0.0", null, context);

            assertThat(result).isFalse();
        }
    }
}
