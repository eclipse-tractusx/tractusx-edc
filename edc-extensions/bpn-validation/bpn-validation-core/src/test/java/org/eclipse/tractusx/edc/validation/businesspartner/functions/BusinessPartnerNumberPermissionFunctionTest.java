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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BusinessPartnerNumberPermissionFunctionTest {

    private BusinessPartnerNumberPermissionFunction validation;

    private PolicyContext policyContext;
    private ParticipantAgent participantAgent;

    private Permission permission = mock();

    @BeforeEach
    void beforeEach() {
        this.policyContext = mock(PolicyContext.class);
        this.participantAgent = mock(ParticipantAgent.class);

        when(policyContext.getContextData(eq(ParticipantAgent.class))).thenReturn(participantAgent);

        validation = new BusinessPartnerNumberPermissionFunction() {
        };
    }

    @ParameterizedTest
    @EnumSource(Operator.class)
    void testFailsOnUnsupportedOperations(Operator operator) {
        if (operator == Operator.EQ) { // only allowed operator
            return;
        }
        assertFalse(validation.evaluate(operator, "foo", permission, policyContext));
        verify(policyContext).reportProblem(argThat(message -> message.contains("As operator only 'EQ' is supported")));
    }

    @Test
    void testFailsOnUnsupportedRightValue() {
        when(participantAgent.getIdentity()).thenReturn("foo");
        assertFalse(validation.evaluate(Operator.EQ, 1, permission, policyContext));
        verify(policyContext).reportProblem(argThat(message -> message.contains("Invalid right operand value: expected 'String' but got")));
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
        verify(policyContext).reportProblem(argThat(message -> message.contains("Identity of the participant not matching the expected one: foo")));
    }

    @Test
    void testValidationForMultipleParticipants() {

        assertFalse(validation.evaluate(Operator.IN, List.of("foo", "bar"), permission, policyContext));
        assertFalse(validation.evaluate(Operator.IN, List.of(1, "foo"), permission, policyContext));
        assertFalse(validation.evaluate(Operator.IN, List.of("bar", "bar"), permission, policyContext));
    }
}
