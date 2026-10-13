/********************************************************************************
 * Copyright (c) 2026 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2026 Cofinity-X GmbH
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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.assertj.core.api.Assertions;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.services.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationFailure;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_POLICY;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ACTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACTION_ACCESS;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACTION_USAGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PolicyTypeNotChangedWhenReferencedTest {

    private final JsonLdPath path = new JsonLdPath("@id");
    private final ContractDefinitionService contractDefinitionService = mock();
    private final PolicyDefinitionService policyDefinitionService = mock();
    private final PolicyTypeNotChangedWhenReferenced validator =
            new PolicyTypeNotChangedWhenReferenced(path, contractDefinitionService, policyDefinitionService);

    @Test
    void shouldPass_whenPolicyDoesNotExistYet() {
        when(policyDefinitionService.findById(anyString())).thenReturn(null);

        var result = validator.validate(input(ACTION_ACCESS));

        assertThat(result).isSucceeded();
        verifyNoInteractions(contractDefinitionService);
    }

    @Test
    void shouldPass_whenPolicyIsNotReferenced() {
        stored(ACTION_ACCESS);
        referencedBy(List.of(), List.of());

        var result = validator.validate(input(ACTION_USAGE));

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldPass_whenReferencedAndTypeUnchanged() {
        stored(ACTION_USAGE);

        var result = validator.validate(input(ACTION_USAGE));

        assertThat(result).isSucceeded();
        verifyNoInteractions(contractDefinitionService);
    }

    @Test
    void shouldFail_whenReferencedAndTypeChanged() {
        stored(ACTION_ACCESS);
        referencedBy(List.of(), List.of(mock(ContractDefinition.class)));

        var result = validator.validate(input(ACTION_USAGE));

        assertThat(result).isFailed()
                .extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .anySatisfy(violation -> Assertions.assertThat(violation.path()).isEqualTo(path.toString()))
                .anySatisfy(violation -> Assertions.assertThat(violation.message())
                        .isEqualTo("Changing the policy type is forbidden if a contract definition references the policy definition."));
    }

    @Test
    void shouldFail_whenReferencedAndNewTypeCannotBeResolved() {
        stored(ACTION_ACCESS);
        referencedBy(List.of(mock(ContractDefinition.class)), List.of());

        var result = validator.validate(input("unknown-action"));

        assertThat(result).isFailed();
    }

    @Test
    void shouldFail_whenSearchAccessPolicyFails() {
        stored(ACTION_ACCESS);
        when(contractDefinitionService.search(any(QuerySpec.class)))
                .thenReturn(ServiceResult.conflict("accessPolicy search failed"));

        var result = validator.validate(input(ACTION_USAGE));

        assertThat(result).isFailed()
                .extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .anySatisfy(violation -> Assertions.assertThat(violation.message()).isEqualTo("accessPolicy search failed"));
    }

    @Test
    void shouldFail_whenSearchContractPolicyFails() {
        stored(ACTION_ACCESS);
        when(contractDefinitionService.search(any(QuerySpec.class)))
                .thenReturn(ServiceResult.success(List.of()))
                .thenReturn(ServiceResult.conflict("contractPolicy search failed"));

        var result = validator.validate(input(ACTION_USAGE));

        assertThat(result).isFailed()
                .extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .anySatisfy(violation -> Assertions.assertThat(violation.message()).isEqualTo("contractPolicy search failed"));
    }

    private void stored(String actionType) {
        var policy = Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance().action(Action.Builder.newInstance().type(actionType).build()).build())
                .build();
        when(policyDefinitionService.findById("policy-id"))
                .thenReturn(PolicyDefinition.Builder.newInstance().id("policy-id").policy(policy).build());
    }

    private void referencedBy(List<ContractDefinition> asAccess, List<ContractDefinition> asContract) {
        when(contractDefinitionService.search(any(QuerySpec.class)))
                .thenReturn(ServiceResult.success(asAccess))
                .thenReturn(ServiceResult.success(asContract));
    }

    private JsonObject input(String actionType) {
        var rule = Json.createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, Json.createArrayBuilder().add(Json.createObjectBuilder().add(ID, actionType)));
        var policy = Json.createObjectBuilder()
                .add(ODRL_PERMISSION_ATTRIBUTE, Json.createArrayBuilder().add(rule));
        return Json.createObjectBuilder()
                .add(ID, "policy-id")
                .add(EDC_POLICY_DEFINITION_POLICY, Json.createArrayBuilder().add(policy))
                .build();
    }
}
