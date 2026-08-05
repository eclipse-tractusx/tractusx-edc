/********************************************************************************
 * Copyright (c) 2026 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.validators.contractdefinitionpolicies;

import jakarta.json.JsonObject;
import org.assertj.core.api.Assertions;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationFailure;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicyActionMatchesExpectedTest {

    private final JsonLdPath path = new JsonLdPath("contractPolicyId");
    private final PolicyDefinitionService policyDefinitionService = mock();
    private final String expectedAction = "use";
    private final PolicyActionMatchesExpected policyActionMatchesExpected = new PolicyActionMatchesExpected(
            path,
            policyDefinitionService,
            expectedAction);

    private JsonObject createInput() {
        // Simplified input for testing purposes.
        // Real input is a Contract Definition in expanded JsonLD
        return createObjectBuilder()
                .add("@id", "id")
                .add("accessPolicyId", createArrayBuilder().add(createObjectBuilder().add(VALUE, "policy-id")))
                .add("contractPolicyId", createArrayBuilder().add(createObjectBuilder().add(VALUE, "policy-id")))
                .build();
    }

    private PolicyDefinition createPolicyDefinitionWithAction(String actionType) {
        return PolicyDefinition.Builder.newInstance()
                .id("policy-id")
                .policy(Policy.Builder.newInstance()
                        .permission(Permission.Builder.newInstance()
                                .action(Action.Builder.newInstance()
                                        .type(actionType)
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Test
    void shouldFail_whenJsonLdPathIsNotFound() {
        var input = createObjectBuilder().build();

        var result = policyActionMatchesExpected.validate(input);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .anySatisfy(violation -> Assertions.assertThat(violation.path()).isEqualTo(path.toString()))
                .anySatisfy(violation -> Assertions.assertThat(violation.message())
                        .isEqualTo("Could not get value for path 'contractPolicyId' in '{}'"));
    }

    @Test
    void shouldFail_whenJsonLdPathIsNotJsonArray() {
        var input = createObjectBuilder()
                .add("contractPolicyId", 123)
                .build();

        var result = policyActionMatchesExpected.validate(input);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .anySatisfy(violation -> Assertions.assertThat(violation.path()).isEqualTo(path.toString()))
                .anySatisfy(violation -> Assertions.assertThat(violation.message())
                        .isEqualTo("Could not get value for path 'contractPolicyId' in '{\"contractPolicyId\":123}'"));
    }

    @Test
    void shouldFail_whenJsonLdPathIsNotJsonLdValue() {
        var input = createObjectBuilder()
                .add("contractPolicyId", createArrayBuilder())
                .build();

        var result = policyActionMatchesExpected.validate(input);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .anySatisfy(violation -> Assertions.assertThat(violation.path()).isEqualTo(path.toString()))
                .anySatisfy(violation -> Assertions.assertThat(violation.message())
                        .isEqualTo("Could not get value for path 'contractPolicyId' in '{\"contractPolicyId\":[]}'"));
    }

    @Test
    void shouldFail_whenPolicyDoesNotExist() {
        when(policyDefinitionService.findById("policy-id")).thenReturn(null);

        var result = policyActionMatchesExpected.validate(createInput());

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .anySatisfy(violation -> Assertions.assertThat(violation.path()).isEqualTo(path.toString()))
                .anySatisfy(violation -> Assertions.assertThat(violation.message())
                        .isEqualTo("Policy with ID 'policy-id' does not exist"));
    }

    @Test
    void shouldFail_whenExpectedActionDoesNotMatchActualAction() {
        var policyDefinition = createPolicyDefinitionWithAction("access");
        when(policyDefinitionService.findById("policy-id")).thenReturn(policyDefinition);

        var result = policyActionMatchesExpected.validate(createInput());

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .anySatisfy(violation -> Assertions.assertThat(violation.path()).isEqualTo(path.toString()))
                .anySatisfy(violation -> Assertions.assertThat(violation.message())
                        .isEqualTo("Policy 'policy-id' does not have the expected permission action 'use'"));
    }

    @Test
    void shouldPass_whenExpectedActionMatchesActualAction() {
        var policyDefinition = createPolicyDefinitionWithAction(expectedAction);
        when(policyDefinitionService.findById("policy-id")).thenReturn(policyDefinition);

        var result = policyActionMatchesExpected.validate(createInput());

        assertThat(result).isSucceeded();
    }
}
