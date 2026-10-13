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

import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.services.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.tractusx.edc.policy.cx.validator.PolicyTypeResolver;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_POLICY;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.spi.query.CriterionOperatorRegistry.EQUAL;
import static org.eclipse.edc.spi.result.ServiceResult.success;
import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACTION_ACCESS;

/**
 * Prevents a policy definition from being switched between access and usage while a contract definition
 * references it. Updates that keep the policy type, and policies that no contract definition references, are allowed.
 */
public class PolicyTypeNotChangedWhenReferenced implements Validator<JsonObject> {

    private final JsonLdPath path;
    private final ContractDefinitionService contractDefinitionService;
    private final PolicyDefinitionService policyDefinitionService;

    public PolicyTypeNotChangedWhenReferenced(JsonLdPath path, ContractDefinitionService contractDefinitionService,
                                             PolicyDefinitionService policyDefinitionService) {
        this.path = path;
        this.contractDefinitionService = contractDefinitionService;
        this.policyDefinitionService = policyDefinitionService;
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        if (!(input.get(ID) instanceof JsonString id)) {
            return ValidationResult.success();
        }

        var existing = policyDefinitionService.findById(id.getString());
        if (existing == null || sameType(existing.getPolicy(), input)) {
            return ValidationResult.success();
        }

        return findReferencingContractDefinitions(id.getString())
                .compose(referencing -> referencing.isEmpty()
                        ? success()
                        : ServiceResult.<Void>conflict("Changing the policy type is forbidden if a contract definition references the policy definition."))
                .map(v -> ValidationResult.success())
                .orElse(failure -> ValidationResult.failure(violation(failure.getFailureDetail(), path.toString())));
    }

    private ServiceResult<List<ContractDefinition>> findReferencingContractDefinitions(String policyId) {
        var queryAccessPolicy = QuerySpec.Builder.newInstance()
                .filter(criterion("accessPolicyId", EQUAL, policyId))
                .build();

        var queryContractPolicy = QuerySpec.Builder.newInstance()
                .filter(criterion("contractPolicyId", EQUAL, policyId))
                .build();

        return contractDefinitionService.search(queryAccessPolicy)
                .compose(accessPolicyMatches -> contractDefinitionService.search(queryContractPolicy)
                        .map(contractPolicyMatches -> Stream.concat(accessPolicyMatches.stream(), contractPolicyMatches.stream()).toList()));
    }

    private boolean sameType(Policy stored, JsonObject input) {
        try {
            return storedType(stored).equals(PolicyTypeResolver.resolve(input.getJsonArray(EDC_POLICY_DEFINITION_POLICY).getJsonObject(0)));
        } catch (RuntimeException e) {
            // unresolvable new type: cannot prove it is unchanged
            return false;
        }
    }

    // same defaulting as PolicyTypeResolver: no rule actions means access
    private String storedType(Policy policy) {
        return Stream.of(policy.getPermissions(), policy.getProhibitions(), policy.getObligations())
                .flatMap(List::stream)
                .map(Rule::getAction)
                .filter(Objects::nonNull)
                .map(Action::getType)
                .findFirst()
                .orElse(ACTION_ACCESS);
    }
}
