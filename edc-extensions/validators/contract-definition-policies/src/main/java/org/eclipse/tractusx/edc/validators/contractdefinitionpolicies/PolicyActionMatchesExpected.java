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
import org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.Optional;

import static java.lang.String.format;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.validator.spi.Violation.violation;

public class PolicyActionMatchesExpected implements Validator<JsonObject> {

    private final JsonLdPath path;
    private final PolicyDefinitionService policyDefinitionService;
    private final String expectedAction;

    public PolicyActionMatchesExpected(
            JsonLdPath path,
            PolicyDefinitionService policyDefinitionService,
            String expectedAction) {
        this.path = path;
        this.policyDefinitionService = policyDefinitionService;
        this.expectedAction = expectedAction;
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        var policyId = getPolicyId(input);
        if (policyId == null) {
            return ValidationResult.failure(
                    violation(format("Could not get value for path '%s' in '%s'", path, input), path.toString()));
        }

        var policyDefinition = policyDefinitionService.findById(policyId);
        if (policyDefinition == null) {
            return ValidationResult.failure(
                    violation(format("Policy with ID '%s' does not exist", policyId), path.toString()));
        }

        var hasExpectedAction =  policyDefinition.getPolicy().getPermissions().stream()
                .map(Permission::getAction)
                .map(Action::getType)
                .allMatch(expectedAction::equals);

        return hasExpectedAction
                ? ValidationResult.success()
                : ValidationResult.failure(
                violation(format("Policy '%s' does not have the expected permission action '%s'",
                        policyId, expectedAction), path.toString()));
    }

    private String getPolicyId(JsonObject input) {
        try {
            return Optional.ofNullable(input.getJsonArray(path.last()))
                    .filter(it -> !it.isEmpty())
                    .map(it -> it.getJsonObject(0))
                    .map(it -> it.getString(VALUE))
                    .orElse(null);
        } catch (ClassCastException e) {
            return null;
        }
    }
}
