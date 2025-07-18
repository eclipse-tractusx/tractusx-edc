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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_AND_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OR_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.atomicConstraint;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.logicalConstraint;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACCESS_POLICY_TYPE;

class LogicalConstraintValidatorTest {

    private final JsonLdPath path = JsonLdPath.path();
    private final String validAccessPolicyLeftOperand = "https://w3id.org/catenax/policy/FrameworkAgreement";

    @Test
    void shouldReturnSuccess_whenValidAndConstraint() {
        JsonObject constraint = atomicConstraint(validAccessPolicyLeftOperand);
        JsonObject input = logicalConstraint(ODRL_AND_CONSTRAINT_ATTRIBUTE, constraint);

        ValidationResult result = validateLogicalConstraint(input, ACCESS_POLICY_TYPE);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnFailure_whenNotAllowedLogicalConstraint() {
        JsonObject constraint = atomicConstraint(validAccessPolicyLeftOperand);
        JsonObject input = logicalConstraint(ODRL_OR_CONSTRAINT_ATTRIBUTE, constraint);

        ValidationResult result = validateLogicalConstraint(input, ACCESS_POLICY_TYPE);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages())
                .anyMatch(msg -> msg.contains("not allowed logical constraints"));
    }

    @Test
    void shouldReturnFailure_whenNoLogicalConstraints() {
        JsonObject input = Json.createObjectBuilder()
                .add("someProperty", "someValue")
                .build();

        ValidationResult result = validateLogicalConstraint(input, ACCESS_POLICY_TYPE);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages())
                .anyMatch(msg -> msg.contains("exactly one logical constraint"));
    }

    @Test
    void shouldReturnFailure_whenLogicalConstraintIsNotArray() {
        JsonObject input = Json.createObjectBuilder()
                .add(ODRL_AND_CONSTRAINT_ATTRIBUTE, "not-an-array")
                .build();

        ValidationResult result = validateLogicalConstraint(input, ACCESS_POLICY_TYPE);

        assertThat(result.failed()).isTrue();
    }

    @Test
    void shouldReturnSuccess_whenLogicalConstraintArrayIsEmpty() {
        JsonObject input = Json.createObjectBuilder()
                .add(ODRL_AND_CONSTRAINT_ATTRIBUTE, Json.createArrayBuilder())
                .build();

        ValidationResult result = validateLogicalConstraint(input, ACCESS_POLICY_TYPE);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnFailure_whenConstraintContentIsInvalid() {
        JsonObject invalidConstraint = Json.createObjectBuilder()
                .add("@id", "invalid-constraint")
                .build();
        JsonObject input = logicalConstraint(ODRL_AND_CONSTRAINT_ATTRIBUTE, invalidConstraint);

        ValidationResult result = validateLogicalConstraint(input, ACCESS_POLICY_TYPE);

        assertThat(result.failed()).isTrue();
    }


    private ValidationResult validateLogicalConstraint(JsonObject input, String policyType) {
        return LogicalConstraintValidator.instance(path, policyType, ODRL_PERMISSION_ATTRIBUTE).validate(input);
    }
}