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

import jakarta.json.JsonString;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.services.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.List;
import java.util.stream.Stream;

import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.spi.query.CriterionOperatorRegistry.EQUAL;
import static org.eclipse.edc.spi.result.ServiceResult.conflict;
import static org.eclipse.edc.spi.result.ServiceResult.success;
import static org.eclipse.edc.validator.spi.Violation.violation;

public class NotReferencedByContractDefinition implements Validator<JsonString> {

    private final JsonLdPath path;
    private final ContractDefinitionService contractDefinitionService;

    public NotReferencedByContractDefinition(JsonLdPath path, ContractDefinitionService contractDefinitionService) {
        this.path = path;
        this.contractDefinitionService = contractDefinitionService;
    }

    @Override
    public ValidationResult validate(JsonString id) {
        var queryAccessPolicy = QuerySpec.Builder.newInstance()
                .filter(criterion("accessPolicyId", EQUAL, id.getString()))
                .build();

        var queryContractPolicy = QuerySpec.Builder.newInstance()
                .filter(criterion("contractPolicyId", EQUAL, id.getString()))
                .build();

        var referencedContractDefinitions = contractDefinitionService.search(queryAccessPolicy)
                .compose(accessPolicyMatches -> contractDefinitionService.search(queryContractPolicy)
                        .compose(contractPolicyMatches -> ServiceResult.success(
                                Stream.concat(accessPolicyMatches.stream(), contractPolicyMatches.stream()).toList())));

        return referencedContractDefinitions
                .compose(this::isListEmpty)
                .map(v -> ValidationResult.success())
                .orElse(failure -> ValidationResult.failure(violation(failure.getFailureDetail(), path.toString())));
    }

    private ServiceResult<Void> isListEmpty(List<ContractDefinition> list) {
        return list.isEmpty() ? success() : conflict("Policy Definition is referenced by a Contract Definition");
    }
}
