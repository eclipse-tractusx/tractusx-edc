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

package org.eclipse.tractusx.edc.policy.cx.framework;

import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;
import static org.eclipse.tractusx.edc.policy.cx.CredentialFunctions.createCredential;
import static org.eclipse.tractusx.edc.policy.cx.CredentialFunctions.createPcfCredential;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class FrameworkAgreementConstraintFunctionTest {
    private final FrameworkAgreementCredentialConstraintFunction function = new FrameworkAgreementCredentialConstraintFunction();
    private final PolicyContext context = mock();
    private Permission permission;
    private ParticipantAgent participantAgent;

    @BeforeEach
    void setup() {
        participantAgent = mock(ParticipantAgent.class);
        when(context.getContextData(eq(ParticipantAgent.class)))
                .thenReturn(participantAgent);
    }

    @Test
    void evaluate_leftOperandInvalid() {
        assertThat(function.evaluate("ThisIsInvalid", Operator.EQ, "irrelevant", permission, context)).isFalse();
        verify(context).reportProblem(startsWith("Constraint left-operand must start with 'FrameworkAgreement'"));
    }

    @Test
    void evaluate_invalidOperator() {
        assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement.foobar", Operator.HAS_PART, "irrelevant", permission, context)).isFalse();
        verify(context).reportProblem(eq("Invalid operator: this constraint only allows the following operators: [EQ, NEQ], but received 'HAS_PART'."));
        verifyNoMoreInteractions(context);
    }

    @Test
    void evaluate_noParticipantOnContext() {
        when(context.getContextData(eq(ParticipantAgent.class))).thenReturn(null);
        assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement.foobar", Operator.EQ, "irrelevant", permission, context)).isFalse();
        verify(context).reportProblem(eq("Required PolicyContext data not found: " + ParticipantAgent.class.getName()));
        verify(context).getContextData(eq(ParticipantAgent.class));
        verifyNoMoreInteractions(context);
    }

    @Test
    void evaluate_vcClaimNotPresent() {
        when(participantAgent.getClaims()).thenReturn(Map.of());
        assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement.foobar", Operator.EQ, "active", permission, context)).isFalse();
        verify(context).reportProblem(eq("ParticipantAgent did not contain a 'vc' claim."));
    }

    @Test
    void evaluate_vcClaimNotListOfCredentials() {
        when(participantAgent.getClaims()).thenReturn(Map.of(
                "vc", new Object()
        ));
        assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement.foobar", Operator.EQ, "active:0.0.1", permission, context)).isFalse();
        verify(context).reportProblem(eq("ParticipantAgent contains a 'vc' claim, but the type is incorrect. Expected java.util.List, received java.lang.Object."));
    }

    @Test
    void evaluate_vcClaimCredentialsEmpty() {
        when(participantAgent.getClaims()).thenReturn(Map.of(
                "vc", new ArrayList<VerifiableCredential>()
        ));
        assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement.foobar", Operator.EQ, "active", permission, context)).isFalse();
        verify(context).reportProblem(eq("ParticipantAgent contains a 'vc' claim but it did not contain any VerifiableCredentials."));
    }

    @Test
    void evaluate_rightOperandInvalidFormat() {
        when(participantAgent.getClaims()).thenReturn(Map.of(
                "vc", List.of(createPcfCredential().build())
        ));
        assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement.pcf", Operator.EQ, "/violate$", permission, context)).isFalse();
        verify(context).reportProblem(eq("Right-operand must contain the keyword 'active' followed by an optional version string: 'active'[:version], but was '/violate$'."));
    }

    @Test
    void evaluate_requiredCredentialNotFound() {
        when(participantAgent.getClaims()).thenReturn(Map.of(
                "vc", List.of(
                        createCredential(CX_POLICY_NS + "PcfCredential", "1.3.0").build(),
                        createCredential(CX_POLICY_NS + "PcfCredential", "1.0.0").build())
        ));
        assertThat(function.evaluate("FrameworkAgreement", Operator.EQ, "someOther:1.3.0", permission, context)).isFalse();
    }

    @Test
    void evaluate_requiredCredential_wrongVersion() {
        when(participantAgent.getClaims()).thenReturn(Map.of(
                "vc", List.of(
                        createCredential("SomeOtherCredential", "2.0.0").build(),
                        createCredential("PcfCredential", "1.8.0").build(),
                        createCredential("PcfCredential", "1.0.0").build())
        ));
        assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement", Operator.EQ, "pcf:1.3.0", permission, context)).isFalse();
    }

    @Test
    void evaluate_requiredCredentialFound() {
        when(participantAgent.getClaims()).thenReturn(Map.of(
                "vc", List.of(
                        createCredential("PcfCredential", "6.0.0").build(),
                        createCredential("SomeOtherType", "3.4.1").build()
                )
        ));
        assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement", Operator.EQ, "pcf", permission, context)).isTrue();
    }

    @Test
    void evaluate_requiredCredentialFound_withCorrectVersion() {
        when(participantAgent.getClaims()).thenReturn(Map.of(
                "vc", List.of(
                        createCredential("PcfCredential", "2.0.0").build(),
                        createCredential("PcfCredential", "1.3.0").build(),
                        createCredential("PcfCredential", "1.0.0").build())
        ));
        assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement", Operator.EQ, "pcf:1.3.0", permission, context)).isTrue();
    }

    @Test
    void evaluate_neq_requiredCredentialFound() {
        when(participantAgent.getClaims()).thenReturn(Map.of(
                "vc", List.of(
                        createCredential("PcfCredential", "6.0.0").build(),
                        createCredential("SomeOtherType", "3.4.1").build()
                )
        ));
        assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement", Operator.NEQ, "sustainability", permission, context)).isTrue();
    }

    @Test
    void evaluate_neq_oneOfManyViolates() {
        when(participantAgent.getClaims()).thenReturn(Map.of(
                "vc", List.of(
                        createCredential("PcfCredential", "6.0.0").build(),
                        createCredential("SustainabilityCredential", "6.0.0").build(),
                        createCredential("SomeOtherType", "3.4.1").build()
                )
        ));
        assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement", Operator.NEQ, "sustainability", permission, context)).isTrue();
    }

    @Test
    void evaluate_neq_oneViolates() {
        when(participantAgent.getClaims()).thenReturn(Map.of(
                "vc", List.of(
                        createCredential("SustainabilityCredential", "6.0.0").build()
                )
        ));
        assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement", Operator.NEQ, "sustainability", permission, context)).isFalse();
    }

    @Test
    void evaluate_neq_requiredCredentialFound_withCorrectVersion() {
        when(participantAgent.getClaims()).thenReturn(Map.of(
                "vc", List.of(
                        createCredential("PcfCredential", "2.0.0").build(),
                        createCredential("FooBarCredential", "1.3.0").build(),
                        createCredential("BarBazCredential", "1.0.0").build())
        ));
        assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement", Operator.NEQ, "sustainability:1.3.0", permission, context)).isTrue();
    }


    @Nested
    class LegacyLeftOperand {
        @Test
        void evaluate_leftOperand_notContainSubtype() {
            when(participantAgent.getClaims()).thenReturn(Map.of(
                    "vc", List.of(createPcfCredential().build())
            ));
            assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement.", Operator.EQ, "active:0.4.2", permission, context)).isFalse();
            verify(context).reportProblem(eq("Left-operand must contain the sub-type 'FrameworkAgreement.<subtype>'."));
        }

        @Test
        void evaluate_leftOperand_notContainFrameworkLiteral() {
            when(participantAgent.getClaims()).thenReturn(Map.of(
                    "vc", List.of(createPcfCredential().build())
            ));
            assertThat(function.evaluate(CX_POLICY_NS + "foobar.pcf", Operator.EQ, "active:0.4.2", permission, context)).isFalse();
            verify(context).reportProblem(startsWith("Constraint left-operand must start with 'FrameworkAgreement' but was"));
        }

        @Test
        void evaluate_leftOperand_notStartsWithFrameworkLiteral() {
            when(participantAgent.getClaims()).thenReturn(Map.of(
                    "vc", List.of(createPcfCredential().build())
            ));
            assertThat(function.evaluate(CX_POLICY_NS + "foobarFrameworkAgreement.pcf", Operator.EQ, "active:0.4.2", permission, context)).isFalse();
            verify(context).reportProblem(startsWith("Constraint left-operand must start with 'FrameworkAgreement' but was"));
        }

        @Test
        void evaluate_rightOperand_notStartWithActiveLiteral() {
            when(participantAgent.getClaims()).thenReturn(Map.of(
                    "vc", List.of(createPcfCredential().build())
            ));
            assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement.pcf", Operator.EQ, "violates", permission, context)).isFalse();
            verify(context).reportProblem(eq("Right-operand must contain the keyword 'active' followed by an optional version string: 'active'[:version], but was 'violates'."));
        }

        @Test
        void evaluate_rightOperandWithVersion() {
            when(participantAgent.getClaims()).thenReturn(Map.of(
                    "vc", List.of(createPcfCredential().build())
            ));
            assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement.pcf", Operator.EQ, "active:1.0.0", permission, context)).isTrue();
            assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement.pcf", Operator.EQ, "active:5.3.1", permission, context)).isFalse();
        }

        @Test
        void evaluate_rightOperand() {
            when(participantAgent.getClaims()).thenReturn(Map.of(
                    "vc", List.of(createPcfCredential().build())
            ));
            assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement.pcf", Operator.EQ, "active", permission, context)).isTrue();
        }
    }

    @Nested
    class NewLeftOperand {
        @Test
        void evaluate_leftOperandNotFrameworkLiteral() {
            when(participantAgent.getClaims()).thenReturn(Map.of(
                    "vc", List.of(createPcfCredential().build())
            ));
            assertThat(function.evaluate("Foobar", Operator.EQ, "active:0.4.2", permission, context)).isFalse();
            verify(context).reportProblem(eq("Constraint left-operand must start with 'FrameworkAgreement' but was 'Foobar'."));
        }

        @Test
        void evaluate_rightOperand_onlySubtype() {
            when(participantAgent.getClaims()).thenReturn(Map.of(
                    "vc", List.of(createPcfCredential().build())
            ));
            assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement", Operator.EQ, "pcf", permission, context)).isTrue();
        }

        @Test
        void evaluate_rightOperand_withVersion() {
            when(participantAgent.getClaims()).thenReturn(Map.of(
                    "vc", List.of(createPcfCredential().build())
            ));
            assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement", Operator.EQ, "pcf:1.0.0", permission, context)).isTrue();
            assertThat(function.evaluate(CX_POLICY_NS + "FrameworkAgreement", Operator.EQ, "pcf:4.2.0", permission, context)).isFalse();
        }

        @Test
        void evaluate_rightOperandMissesSubtype() {
            when(participantAgent.getClaims()).thenReturn(Map.of(
                    "vc", List.of(createPcfCredential().build())
            ));
            assertThat(function.evaluate("FrameworkAgreement", Operator.EQ, ":1.0.0", permission, context)).isFalse();
        }
    }
}
