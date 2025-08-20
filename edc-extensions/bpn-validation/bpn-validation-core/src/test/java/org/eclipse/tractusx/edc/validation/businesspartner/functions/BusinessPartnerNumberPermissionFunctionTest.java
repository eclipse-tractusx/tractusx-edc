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
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BusinessPartnerNumberPermissionFunctionTest {

    private final ParticipantAgent participantAgent = mock();
    private final BdrsClient bdrsClient = mock();
    private final Permission unusedPermission = Permission.Builder.newInstance().build();
    private final ParticipantAgentPolicyContext policyContext = new TestParticipantAgentPolicyContext(participantAgent);
    private final BusinessPartnerNumberPermissionFunction<TestParticipantAgentPolicyContext> validation = new BusinessPartnerNumberPermissionFunction<>(bdrsClient);

    @ParameterizedTest(name = "Illegal Operator {0}")
    @ArgumentsSource(IllegalOperatorProvider.class)
    void testFailsOnUnsupportedOperations(Operator illegalOperator) {
        var result = validation.evaluate(illegalOperator, "foo", unusedPermission, policyContext);

        assertFalse(result);
        assertThat(policyContext.getProblems()).hasSize(1)
                .anyMatch(it -> it.startsWith("Operator %s is not supported.".formatted(illegalOperator)));
    }

    @Test
    void testBdrsClientCalledWhenIdentityIsDid() {
        var did = "did:web:foo";
        var bpn = "foo";
        when(participantAgent.getIdentity()).thenReturn(did);
        when(bdrsClient.resolveBpn(did)).thenReturn(bpn);

        var result = validation.evaluate(Operator.EQ, "foo", unusedPermission, policyContext);

        assertThat(result).isTrue();
        verify(bdrsClient).resolveBpn(did);
    }

    @Test
    void testValidationFailsInvalidIdentity() {
        when(participantAgent.getIdentity()).thenReturn("bar");

        var result = validation.evaluate(Operator.EQ, "foo", unusedPermission, policyContext);

        assertThat(result).isFalse();
    }

    @Test
    void testValidationForMultipleParticipants() {
        when(participantAgent.getIdentity()).thenReturn("quazz");

        assertThat(validation.evaluate(Operator.IN, List.of("foo", "bar"), unusedPermission, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.IN, List.of(1, "foo"), unusedPermission, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.IN, List.of("bar", "bar"), unusedPermission, policyContext)).isFalse();
    }

    @Test
    void evaluate_isAnyOf() {
        when(participantAgent.getIdentity()).thenReturn("foo");
        assertThat(validation.evaluate(Operator.IS_ANY_OF, bpnList("foo", "bar"), unusedPermission, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.IS_ANY_OF, bpnList("foo"), unusedPermission, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.IS_ANY_OF, bpnList("bar"), unusedPermission, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.IS_ANY_OF, "bar", unusedPermission, policyContext)).isFalse();
        assertThat(policyContext.getProblems()).containsOnly("Invalid right-value: operator 'IS_ANY_OF' requires a 'List' but got a 'java.lang.String'");

    }

    @Test
    void evaluate_isNoneOf() {
        when(participantAgent.getIdentity()).thenReturn("foo");
        assertThat(validation.evaluate(Operator.IS_NONE_OF, bpnList("foo", "bar"), unusedPermission, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.IS_NONE_OF, bpnList("foo"), unusedPermission, policyContext)).isFalse();
        assertThat(validation.evaluate(Operator.IS_NONE_OF, bpnList("bar"), unusedPermission, policyContext)).isTrue();
        assertThat(validation.evaluate(Operator.IS_NONE_OF, "bar", unusedPermission, policyContext)).isFalse();
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

    private List<LinkedHashMap<String, LinkedHashMap<String, String>>> bpnList(String... bpns) {
        if (bpns == null || bpns.length == 0) {
            return Collections.emptyList();
        }

        return Arrays.stream(bpns)
                .map(bpn -> {
                    LinkedHashMap<String, String> valueMap = new LinkedHashMap<>();
                    valueMap.put("string", bpn);

                    LinkedHashMap<String, LinkedHashMap<String, String>> bpnMap = new LinkedHashMap<>();
                    bpnMap.put("@value", valueMap);
                    return bpnMap;
                })
                .collect(Collectors.toList());
    }
}
