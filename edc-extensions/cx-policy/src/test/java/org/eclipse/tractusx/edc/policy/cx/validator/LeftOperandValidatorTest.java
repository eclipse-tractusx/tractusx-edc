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
import org.eclipse.edc.junit.assertions.FailureAssert;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OBLIGATION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PROHIBITION_ATTRIBUTE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACTION_ACCESS;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACTION_USAGE;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.BUSINESS_PARTNER_GROUP_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.DATA_PROVISIONING_END_DURATION_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.FRAMEWORK_AGREEMENT_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.INFORCE_POLICY_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.MEMBERSHIP_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.USAGE_PURPOSE_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.USAGE_RESTRICTION_LITERAL;

class LeftOperandValidatorTest {

    private final JsonLdPath path = JsonLdPath.path();

    @Test
    void shouldReturnSuccess_whenValidAccessPermissionPolicyLeftOperand() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, FRAMEWORK_AGREEMENT_LITERAL)
                .build();

        ValidationResult result = validateLeftOperand(input, ACTION_ACCESS, ODRL_PERMISSION_ATTRIBUTE);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldReturnSuccess_whenValidUsagePermissionPolicyLeftOperand() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, USAGE_PURPOSE_LITERAL)
                .build();

        ValidationResult result = validateLeftOperand(input, ACTION_USAGE, ODRL_PERMISSION_ATTRIBUTE);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldReturnSuccess_whenValidUsageProhibitionPolicyLeftOperand() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, USAGE_RESTRICTION_LITERAL)
                .build();

        ValidationResult result = validateLeftOperand(input, ACTION_USAGE, ODRL_PROHIBITION_ATTRIBUTE);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldReturnSuccess_whenValidUsageObligationPolicyLeftOperand() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, DATA_PROVISIONING_END_DURATION_LITERAL)
                .build();

        ValidationResult result = validateLeftOperand(input, ACTION_USAGE, ODRL_OBLIGATION_ATTRIBUTE);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldReturnFailure_whenIdIsMissing() {
        JsonObject input = Json.createObjectBuilder().build();

        ValidationResult result = validateLeftOperand(input, ACTION_ACCESS, ODRL_PERMISSION_ATTRIBUTE);

        assertThat(result).isFailed();
    }

    @Test
    void shouldReturnFailure_whenLeftOperandNotAllowedForAccessPermissionPolicy() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, USAGE_PURPOSE_LITERAL)
                .build();

        ValidationResult result = validateLeftOperand(input, ACTION_ACCESS, ODRL_PERMISSION_ATTRIBUTE);

        assertThat(result).isFailed();
        FailureAssert.assertThat(result.getFailure()).messages().anyMatch(msg -> msg.contains("is not allowed"));
    }

    @Test
    void shouldReturnFailure_whenLeftOperandNotAllowedForUsagePermissionPolicy() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, BUSINESS_PARTNER_GROUP_LITERAL)
                .build();

        ValidationResult result = validateLeftOperand(input, ACTION_USAGE, ODRL_PERMISSION_ATTRIBUTE);

        assertThat(result).isFailed();
        FailureAssert.assertThat(result.getFailure()).messages().anyMatch(msg -> msg.contains("is not allowed"));
    }

    @Test
    void shouldReturnFailure_whenLeftOperandNotAllowedForUsageProhibitionPolicy() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, USAGE_PURPOSE_LITERAL)
                .build();

        ValidationResult result = validateLeftOperand(input, ACTION_USAGE, ODRL_PROHIBITION_ATTRIBUTE);

        assertThat(result).isFailed();
        FailureAssert.assertThat(result.getFailure()).messages().anyMatch(msg -> msg.contains("is not allowed"));
    }

    @Test
    void shouldReturnFailure_whenLeftOperandNotAllowedForUsageObligationPolicy() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, USAGE_PURPOSE_LITERAL)
                .build();

        ValidationResult result = validateLeftOperand(input, ACTION_USAGE, ODRL_OBLIGATION_ATTRIBUTE);

        assertThat(result).isFailed();
        FailureAssert.assertThat(result.getFailure()).messages().anyMatch(msg -> msg.contains("is not allowed"));
    }

    @Test
    void shouldReturnFailure_whenLeftOperandIsCompletelyInvalid() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, "invalid-operand")
                .build();

        ValidationResult result = validateLeftOperand(input, ACTION_ACCESS, ODRL_PERMISSION_ATTRIBUTE);

        assertThat(result).isFailed();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "https://example.com/"})
    void shouldReturnFailure_whenIdEndsWithSlashOrEmpty(String id) {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, id)
                .build();

        ValidationResult result = validateLeftOperand(input, ACTION_ACCESS, ODRL_PERMISSION_ATTRIBUTE);

        assertThat(result).isFailed();
    }

    @Test
    void shouldReturnSuccess_whenNotMutuallyExclusiveConstraintsPresent() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, INFORCE_POLICY_LITERAL)
                .build();

        ValidationResult result = LeftOperandValidator
                .instance(JsonObjectValidator.newValidator(), ACTION_USAGE, ODRL_PERMISSION_ATTRIBUTE, new HashSet<>(Set.of(MEMBERSHIP_LITERAL)))
                .build()
                .validate(input);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnFailure_whenMutuallyExclusiveConstraintsPresent() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, INFORCE_POLICY_LITERAL)
                .build();

        ValidationResult result = LeftOperandValidator
                .instance(JsonObjectValidator.newValidator(), ACTION_USAGE, ODRL_PERMISSION_ATTRIBUTE, new HashSet<>(Set.of(USAGE_PURPOSE_LITERAL)))
                .build()
                .validate(input);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anyMatch(msg ->
                msg.contains("is mutually exclusive"));
    }

    private ValidationResult validateLeftOperand(JsonObject input, String policyType, String ruleType) {
        return LeftOperandValidator.instance(JsonObjectValidator.newValidator(), policyType, ruleType)
                .build()
                .validate(input);
    }
}
