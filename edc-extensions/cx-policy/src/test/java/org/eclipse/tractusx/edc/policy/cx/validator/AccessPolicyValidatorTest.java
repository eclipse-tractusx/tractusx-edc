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
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OBLIGATION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PROHIBITION_ATTRIBUTE;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.atomicConstraint;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.emptyRule;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.policy;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.rule;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACTION_ACCESS;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACTION_USAGE;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.FRAMEWORK_AGREEMENT_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.MEMBERSHIP_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.USAGE_PURPOSE_LITERAL;

class AccessPolicyValidatorTest {

    private final JsonLdPath path = JsonLdPath.path();

    private AccessPolicyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AccessPolicyValidator(path);
    }

    @Test
    void shouldReturnSuccess_whenValidAccessPermissionWithConstraints() {
        JsonObject constraint = atomicConstraint(FRAMEWORK_AGREEMENT_LITERAL);
        JsonObject permission = rule(ACTION_ACCESS, constraint);
        JsonObject input = policy(ODRL_PERMISSION_ATTRIBUTE, permission);

        ValidationResult result = validator.validate(input);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnSuccess_whenValidAccessPermissionWithoutConstraints() {
        JsonObject permission = rule(ACTION_ACCESS);
        JsonObject input = policy(ODRL_PERMISSION_ATTRIBUTE, permission);

        ValidationResult result = validator.validate(input);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnFailure_whenPermissionHasWrongActionType() {
        JsonObject constraint = atomicConstraint(USAGE_PURPOSE_LITERAL);
        JsonObject permission = rule(ACTION_USAGE, constraint);
        JsonObject input = policy(ODRL_PERMISSION_ATTRIBUTE, permission);

        ValidationResult result = validator.validate(input);

        assertThat(result.failed()).isTrue();
    }

    @Test
    void shouldReturnSuccess_whenPermissionMissingAction() {
        JsonObject permission = emptyRule();

        JsonObject input = policy(ODRL_PERMISSION_ATTRIBUTE, permission);

        ValidationResult result = validator.validate(input);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnFailure_whenProhibitionArrayIsNotEmpty() {
        JsonObject prohibition = rule(ACTION_ACCESS);
        JsonObject input = policy(ODRL_PROHIBITION_ATTRIBUTE, prohibition);

        ValidationResult result = validator.validate(input);

        assertThat(result.failed()).isTrue();
    }

    @Test
    void shouldReturnSuccess_whenProhibitionArrayIsEmpty() {
        JsonObject input = Json.createObjectBuilder()
                .add(ODRL_PROHIBITION_ATTRIBUTE, Json.createArrayBuilder())
                .build();

        ValidationResult result = validator.validate(input);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnSuccess_whenObligationArrayIsEmpty() {
        JsonObject input = Json.createObjectBuilder()
                .add(ODRL_OBLIGATION_ATTRIBUTE, Json.createArrayBuilder())
                .build();

        ValidationResult result = validator.validate(input);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnFailure_whenObligationArrayIsNotEmpty() {
        JsonObject obligation = rule(ACTION_ACCESS);
        JsonObject input = policy(ODRL_PROHIBITION_ATTRIBUTE, obligation);

        ValidationResult result = validator.validate(input);

        assertThat(result.failed()).isTrue();
    }

    @Test
    void shouldReturnSuccess_whenCompleteValidAccessPolicy() {
        JsonObject constraint = atomicConstraint(MEMBERSHIP_LITERAL);

        JsonObject permission = rule(ACTION_ACCESS, constraint);

        JsonObject input = Json.createObjectBuilder()
                .add(ODRL_PERMISSION_ATTRIBUTE, Json.createArrayBuilder().add(permission))
                .add(ODRL_PROHIBITION_ATTRIBUTE, Json.createArrayBuilder())
                .add(ODRL_OBLIGATION_ATTRIBUTE, Json.createArrayBuilder())
                .build();

        ValidationResult result = validator.validate(input);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnSuccess_whenEmptyPolicy() {
        JsonObject input = Json.createObjectBuilder().build();

        ValidationResult result = validator.validate(input);

        assertThat(result.succeeded()).isTrue();
    }
}