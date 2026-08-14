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

import jakarta.json.Json;
import jakarta.json.JsonString;
import org.assertj.core.api.Assertions;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.services.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationFailure;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotReferencedByContractDefinitionTest {

    private final JsonLdPath path = new JsonLdPath("@id");
    private final ContractDefinitionService contractDefinitionService = mock();
    private final NotReferencedByContractDefinition validator = new NotReferencedByContractDefinition(path, contractDefinitionService);
    private final JsonString policyId = Json.createValue("policy-id");

    @Test
    void shouldFail_whenSearchAccessPolicyFails() {
        when(contractDefinitionService.search(any(QuerySpec.class)))
                .thenReturn(ServiceResult.conflict("accessPolicy search failed"));

        var result = validator.validate(policyId);

        assertThat(result).isFailed()
                .extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .anySatisfy(violation -> Assertions.assertThat(violation.path()).isEqualTo(path.toString()))
                .anySatisfy(violation -> Assertions.assertThat(violation.message()).isEqualTo("accessPolicy search failed"));
    }

    @Test
    void shouldFail_whenSearchContractPolicyFails() {
        when(contractDefinitionService.search(any(QuerySpec.class)))
                .thenReturn(ServiceResult.success(List.of()))
                .thenReturn(ServiceResult.conflict("contractPolicy search failed"));

        var result = validator.validate(policyId);

        assertThat(result).isFailed()
                .extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .anySatisfy(violation -> Assertions.assertThat(violation.path()).isEqualTo(path.toString()))
                .anySatisfy(violation -> Assertions.assertThat(violation.message()).isEqualTo("contractPolicy search failed"));
    }

    @Test
    void shouldFail_whenPolicyIsReferencedByContractDefinition() {
        when(contractDefinitionService.search(any(QuerySpec.class)))
                .thenReturn(ServiceResult.success(List.of(mock(ContractDefinition.class))))
                .thenReturn(ServiceResult.success(List.of()));

        var result = validator.validate(policyId);

        assertThat(result).isFailed()
                .extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .isNotEmpty()
                .anySatisfy(violation -> Assertions.assertThat(violation.path()).isEqualTo(path.toString()))
                .anySatisfy(violation -> Assertions.assertThat(violation.message())
                        .isEqualTo("Policy Definition is referenced by a Contract Definition"));
    }

    @Test
    void shouldPass_whenPolicyIsNotReferencedByContractDefinition() {
        when(contractDefinitionService.search(any(QuerySpec.class)))
                .thenReturn(ServiceResult.success(List.of()))
                .thenReturn(ServiceResult.success(List.of()));

        var result = validator.validate(policyId);

        assertThat(result).isSucceeded();
    }
}