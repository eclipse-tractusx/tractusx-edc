/********************************************************************************
 * Copyright (c) 2025 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
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
import org.eclipse.edc.junit.assertions.FailureAssert;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSTRAINT_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_LEFT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OPERATOR_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_RIGHT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.atomicConstraint;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACTION_ACCESS;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACTION_USAGE;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.AFFILIATES_REGION_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.BUSINESS_PARTNER_GROUP_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.BUSINESS_PARTNER_NUMBER_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.DATA_PROVISIONING_END_DURATION_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.FRAMEWORK_AGREEMENT_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.MEMBERSHIP_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.USAGE_PURPOSE_LITERAL;

class AtomicConstraintValidatorTest {

    private AtomicConstraintValidator accessPolicyValidator;
    private AtomicConstraintValidator usagePolicyValidator;
    private JsonLdPath path;

    @BeforeEach
    void setUp() {
        path = JsonLdPath.path();
        accessPolicyValidator = AtomicConstraintValidator.instance(path, ACTION_ACCESS, ODRL_PERMISSION_ATTRIBUTE);
        usagePolicyValidator = AtomicConstraintValidator.instance(path, ACTION_USAGE, ODRL_PERMISSION_ATTRIBUTE);
    }

    @Test
    void validate_shouldSucceed_whenValidAccessPolicyConstraint() {
        var constraint = atomicConstraint(FRAMEWORK_AGREEMENT_LITERAL);
        ValidationResult result = accessPolicyValidator.validate(constraint);

        assertThat(result).isSucceeded();
    }

    @Test
    void validate_shouldSucceed_whenValidUsagePolicyConstraint() {
        var constraint = atomicConstraint(USAGE_PURPOSE_LITERAL);

        ValidationResult result = usagePolicyValidator.validate(constraint);

        assertThat(result).isSucceeded();
    }

    @Test
    void validate_shouldFail_whenLeftOperandMissing() {
        var constraint = Json.createObjectBuilder()
                .add(TYPE, ODRL_CONSTRAINT_TYPE)
                .add(ODRL_OPERATOR_ATTRIBUTE, Json.createArrayBuilder().add(Json.createObjectBuilder().add(ID, "odrl:eq")))
                .add(ODRL_RIGHT_OPERAND_ATTRIBUTE, Json.createArrayBuilder().add(Json.createObjectBuilder().add(VALUE, "test-value")))
                .build();

        ValidationResult result = accessPolicyValidator.validate(constraint);

        assertThat(result).isFailed();
        FailureAssert.assertThat(result.getFailure()).messages().anyMatch(msg ->
                msg.contains("leftOperand") && msg.contains("mandatory"));
    }

    @Test
    void validate_shouldFail_whenOperatorMissing() {
        var constraint = Json.createObjectBuilder()
                .add(TYPE, ODRL_CONSTRAINT_TYPE)
                .add(ODRL_LEFT_OPERAND_ATTRIBUTE, Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .add(ID, FRAMEWORK_AGREEMENT_LITERAL)))
                .add(ODRL_RIGHT_OPERAND_ATTRIBUTE, Json.createArrayBuilder().add(Json.createObjectBuilder().add(VALUE, "test-value")))
                .build();

        ValidationResult result = accessPolicyValidator.validate(constraint);

        assertThat(result).isFailed();
        FailureAssert.assertThat(result.getFailure()).messages().anyMatch(msg ->
                msg.contains("operator") && msg.contains("mandatory"));
    }

    @Test
    void validate_shouldFail_whenRightOperandMissing() {
        var constraint = Json.createObjectBuilder()
                .add(TYPE, ODRL_CONSTRAINT_TYPE)
                .add(ODRL_LEFT_OPERAND_ATTRIBUTE, Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .add(ID, FRAMEWORK_AGREEMENT_LITERAL)))
                .add(ODRL_OPERATOR_ATTRIBUTE, Json.createArrayBuilder().add(Json.createObjectBuilder().add(ID, "odrl:eq")))
                .build();

        ValidationResult result = accessPolicyValidator.validate(constraint);

        assertThat(result).isFailed();
        FailureAssert.assertThat(result.getFailure()).messages().anyMatch(msg ->
                msg.contains("rightOperand") && msg.contains("mandatory"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            FRAMEWORK_AGREEMENT_LITERAL,
            MEMBERSHIP_LITERAL,
            BUSINESS_PARTNER_GROUP_LITERAL,
            BUSINESS_PARTNER_NUMBER_LITERAL
    })
    void validate_shouldSucceed_whenValidAccessPolicyLeftOperand(String leftOperand) {
        var constraint = atomicConstraint(leftOperand);

        ValidationResult result = accessPolicyValidator.validate(constraint);

        assertThat(result).isSucceeded();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            USAGE_PURPOSE_LITERAL,
            MEMBERSHIP_LITERAL,
            AFFILIATES_REGION_LITERAL
    })
    void validate_shouldSucceed_whenValidUsagePolicyLeftOperand(String leftOperand) {
        var constraint = atomicConstraint(leftOperand);

        ValidationResult result = usagePolicyValidator.validate(constraint);

        assertThat(result).isSucceeded();
    }

    @Test
    void validate_shouldFail_whenInvalidLeftOperandForAccessPolicy() {
        var constraint = atomicConstraint(USAGE_PURPOSE_LITERAL);

        ValidationResult result = accessPolicyValidator.validate(constraint);

        assertThat(result).isFailed();
        FailureAssert.assertThat(result.getFailure()).messages().anyMatch(msg ->
                msg.contains("UsagePurpose") && msg.contains("not allowed"));
    }

    @Test
    void validate_shouldFail_whenInvalidLeftOperandForUsagePolicy() {
        var constraint = atomicConstraint(DATA_PROVISIONING_END_DURATION_LITERAL);

        ValidationResult result = usagePolicyValidator.validate(constraint);

        assertThat(result).isFailed();
        FailureAssert.assertThat(result.getFailure()).messages().anyMatch(msg ->
                msg.contains("DataProvisioningEndDurationDays") && msg.contains("not allowed"));
    }

    @Test
    void validate_shouldFail_whenLeftOperandIdIsEmpty() {
        var constraint = atomicConstraint("");

        ValidationResult result = accessPolicyValidator.validate(constraint);

        assertThat(result).isFailed();
        FailureAssert.assertThat(result.getFailure()).messages().anyMatch(msg ->
                msg.contains("leftOperand") && msg.contains("empty"));
    }

    @Test
    void validate_shouldFail_whenLeftOperandIdIsNull() {
        var constraint = Json.createObjectBuilder()
                .add(TYPE, ODRL_CONSTRAINT_TYPE)
                .add(ODRL_LEFT_OPERAND_ATTRIBUTE, Json.createArrayBuilder().add(Json.createObjectBuilder()))
                .add(ODRL_OPERATOR_ATTRIBUTE, Json.createArrayBuilder().add(Json.createObjectBuilder().add(ID, "odrl:eq")))
                .add(ODRL_RIGHT_OPERAND_ATTRIBUTE, Json.createArrayBuilder().add(Json.createObjectBuilder().add(VALUE, "test-value")))
                .build();

        ValidationResult result = accessPolicyValidator.validate(constraint);

        assertThat(result).isFailed();
        FailureAssert.assertThat(result.getFailure()).messages().anyMatch(msg ->
                msg.contains("leftOperand") && msg.contains("null"));
    }

    @Test
    void validate_shouldSucceed_whenOperatorHasNoId() {
        var constraint = Json.createObjectBuilder()
                .add(TYPE, ODRL_CONSTRAINT_TYPE)
                .add(ODRL_LEFT_OPERAND_ATTRIBUTE, Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .add("@id", FRAMEWORK_AGREEMENT_LITERAL)))
                .add(ODRL_OPERATOR_ATTRIBUTE, Json.createArrayBuilder().add(Json.createObjectBuilder()))
                .add(ODRL_RIGHT_OPERAND_ATTRIBUTE, Json.createArrayBuilder().add(Json.createObjectBuilder().add(VALUE, "test-value")))
                .build();

        ValidationResult result = accessPolicyValidator.validate(constraint);

        assertThat(result).isSucceeded();
    }

    @Test
    void validate_shouldFail_whenCompletelyInvalidJson() {
        var constraint = Json.createObjectBuilder()
                .add("invalid", "structure")
                .build();

        ValidationResult result = accessPolicyValidator.validate(constraint);

        assertThat(result).isFailed();
    }
}
