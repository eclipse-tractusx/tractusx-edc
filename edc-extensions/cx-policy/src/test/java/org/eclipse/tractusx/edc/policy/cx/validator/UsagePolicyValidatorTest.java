/********************************************************************************
 * Copyright (c) 2025 Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
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

package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.*;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.*;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.*;

class UsagePolicyValidatorTest {

    private final JsonLdPath path = JsonLdPath.path();

    private UsagePolicyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new UsagePolicyValidator(path);
    }

    @Test
    void shouldReturnSuccess_whenValidUsagePermissionWithConstraints() {
        JsonObject constraint = atomicConstraint(USAGE_PURPOSE_LITERAL);
        JsonObject permission = rule(ACTION_USAGE, constraint);
        JsonObject input = policy(ODRL_PERMISSION_ATTRIBUTE, permission);

        ValidationResult result = validator.validate(input);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnSuccess_whenValidUsagePermissionWithoutConstraints() {
        JsonObject permission = rule(ACTION_USAGE);
        JsonObject input = policy(ODRL_PERMISSION_ATTRIBUTE, permission);

        ValidationResult result = validator.validate(input);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnFailure_whenPermissionHasWrongActionType() {
        JsonObject constraint = atomicConstraint(FRAMEWORK_AGREEMENT_LITERAL);
        JsonObject permission = rule(ACTION_ACCESS, constraint);
        JsonObject input = policy(ODRL_PERMISSION_ATTRIBUTE, permission);

        ValidationResult result = validator.validate(input);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anyMatch(msg ->
                msg.contains("Action was expected to be") && msg.contains(USAGE_POLICY_TYPE));
    }

    @Test
    void shouldReturnSuccess_whenValidUsageProhibitionWithConstraints() {
        JsonObject constraint = atomicConstraint(USAGE_PURPOSE_LITERAL);
        JsonObject prohibition = rule(ACTION_USAGE, constraint);
        JsonObject input = policy(ODRL_PROHIBITION_ATTRIBUTE, prohibition);

        ValidationResult result = validator.validate(input);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnSuccess_whenValidUsageProhibitionWithoutConstraints() {
        JsonObject prohibition = rule(ACTION_USAGE);
        JsonObject input = policy(ODRL_PROHIBITION_ATTRIBUTE, prohibition);

        ValidationResult result = validator.validate(input);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnFailure_whenProhibitionHasWrongActionType() {
        JsonObject constraint = atomicConstraint(USAGE_PURPOSE_LITERAL);
        JsonObject prohibition = rule(ACTION_ACCESS, constraint);
        JsonObject input = policy(ODRL_PROHIBITION_ATTRIBUTE, prohibition);

        ValidationResult result = validator.validate(input);

        assertThat(result.failed()).isTrue();
    }

    @Test
    void shouldReturnSuccess_whenValidUsageObligationWithConstraints() {
        JsonObject constraint = atomicConstraint(AFFILIATES_REGION_LITERAL);
        JsonObject obligation = rule(ACTION_USAGE, constraint);
        JsonObject input = policy(ODRL_OBLIGATION_ATTRIBUTE, obligation);

        ValidationResult result = validator.validate(input);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnSuccess_whenValidUsageObligationWithoutConstraints() {
        JsonObject obligation = rule(ACTION_USAGE);
        JsonObject input = policy(ODRL_OBLIGATION_ATTRIBUTE, obligation);

        ValidationResult result = validator.validate(input);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnFailure_whenObligationHasWrongActionType() {
        JsonObject constraint = atomicConstraint(USAGE_PURPOSE_LITERAL);
        JsonObject obligation = rule(ACTION_ACCESS, constraint);
        JsonObject input = policy(ODRL_OBLIGATION_ATTRIBUTE, obligation);

        ValidationResult result = validator.validate(input);

        assertThat(result.failed()).isTrue();
    }

    @Test
    void shouldReturnFailure_whenEmptyArrays() {
        JsonObject input = Json.createObjectBuilder()
                .add(ODRL_PERMISSION_ATTRIBUTE, Json.createArrayBuilder())
                .add(ODRL_PROHIBITION_ATTRIBUTE, Json.createArrayBuilder())
                .add(ODRL_OBLIGATION_ATTRIBUTE, Json.createArrayBuilder())
                .build();

        ValidationResult result = validator.validate(input);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anyMatch(msg ->
                msg.contains("Policy must contain at least one permission, obligation, or prohibition"));
    }

    @Test
    void shouldReturnSuccess_whenCompleteValidUsagePolicy() {
        JsonObject permission = rule(ACTION_USAGE, atomicConstraint(USAGE_PURPOSE_LITERAL));
        JsonObject prohibition = rule(ACTION_USAGE, atomicConstraint(USAGE_PURPOSE_LITERAL));
        JsonObject obligation = rule(ACTION_USAGE, atomicConstraint(AFFILIATES_REGION_LITERAL));

        JsonObject input = Json.createObjectBuilder()
                .add(ODRL_PERMISSION_ATTRIBUTE, Json.createArrayBuilder().add(permission))
                .add(ODRL_PROHIBITION_ATTRIBUTE, Json.createArrayBuilder().add(prohibition))
                .add(ODRL_OBLIGATION_ATTRIBUTE, Json.createArrayBuilder().add(obligation))
                .build();

        ValidationResult result = validator.validate(input);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnFailure_whenRuleIsNotArray() {
        JsonObject constraint = atomicConstraint(USAGE_PURPOSE_LITERAL);

        JsonObject permission = rule(ACTION_USAGE, constraint);

        JsonObject input = Json.createObjectBuilder()
                .add(ODRL_PERMISSION_ATTRIBUTE, permission)
                .build();

        ValidationResult result = validator.validate(input);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anyMatch(msg ->
                msg.contains("Expected array") && msg.contains(ODRL_PERMISSION_ATTRIBUTE));
    }

    @Test
    void shouldReturnFailure_whenEmptyPolicy() {
        JsonObject input = Json.createObjectBuilder().build();

        ValidationResult result = validator.validate(input);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anyMatch(msg ->
                msg.contains("Policy must contain at least one permission, obligation, or prohibition"));
    }

    @Test
    void shouldReturnFailure_whenRuleMissingAction() {
        JsonObject permission = emptyRule();
        JsonObject input = policy(ODRL_PERMISSION_ATTRIBUTE, permission);

        ValidationResult result = validator.validate(input);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anyMatch(msg ->
                msg.contains("Action property is missing"));
    }
}