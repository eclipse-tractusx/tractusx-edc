package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.Json;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.*;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.*;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.atomicConstraint;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.*;

class AtomicConstraintValidatorTest {

    private AtomicConstraintValidator accessPolicyValidator;
    private AtomicConstraintValidator usagePolicyValidator;
    private JsonLdPath path;

    @BeforeEach
    void setUp() {
        path = JsonLdPath.path();
        accessPolicyValidator = AtomicConstraintValidator.instance(path, ACCESS_POLICY_TYPE, ODRL_PERMISSION_ATTRIBUTE);
        usagePolicyValidator = AtomicConstraintValidator.instance(path, USAGE_POLICY_TYPE, ODRL_PERMISSION_ATTRIBUTE);
    }

    @Test
    void validate_shouldSucceed_whenValidAccessPolicyConstraint() {
        var constraint = atomicConstraint(FRAMEWORK_AGREEMENT_LITERAL);
        String jsonString = constraint.toString();
        ValidationResult result = accessPolicyValidator.validate(constraint);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validate_shouldSucceed_whenValidUsagePolicyConstraint() {
        var constraint = atomicConstraint(USAGE_PURPOSE_LITERAL);

        ValidationResult result = usagePolicyValidator.validate(constraint);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validate_shouldFail_whenLeftOperandMissing() {
        var constraint = Json.createObjectBuilder()
                .add(TYPE, ODRL_CONSTRAINT_TYPE)
                .add(ODRL_OPERATOR_ATTRIBUTE, Json.createArrayBuilder().add(Json.createObjectBuilder().add(ID, "odrl:eq")))
                .add(ODRL_RIGHT_OPERAND_ATTRIBUTE, Json.createArrayBuilder().add(Json.createObjectBuilder().add(VALUE, "test-value")))
                .build();

        ValidationResult result = accessPolicyValidator.validate(constraint);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anyMatch(msg ->
                msg.contains("leftOperand") && msg.contains("mandatory"));
    }

    @Test
    void validate_shouldFail_whenOperatorMissing() {
        var constraint = Json.createObjectBuilder()
                .add(TYPE, ODRL_CONSTRAINT_TYPE)
                .add(ODRL_LEFT_OPERAND_ATTRIBUTE, Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .add(ID, "https://w3id.org/catenax/policy/FrameworkAgreement")))
                .add(ODRL_RIGHT_OPERAND_ATTRIBUTE, Json.createArrayBuilder().add(Json.createObjectBuilder().add(VALUE, "test-value")))
                .build();

        ValidationResult result = accessPolicyValidator.validate(constraint);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anyMatch(msg ->
                msg.contains("operator") && msg.contains("mandatory"));
    }

    @Test
    void validate_shouldFail_whenRightOperandMissing() {
        var constraint = Json.createObjectBuilder()
                .add(TYPE, ODRL_CONSTRAINT_TYPE)
                .add(ODRL_LEFT_OPERAND_ATTRIBUTE, Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .add(ID, "https://w3id.org/catenax/policy/FrameworkAgreement")))
                .add(ODRL_OPERATOR_ATTRIBUTE, Json.createArrayBuilder().add(Json.createObjectBuilder().add(ID, "odrl:eq")))
                .build();

        ValidationResult result = accessPolicyValidator.validate(constraint);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anyMatch(msg ->
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

        assertThat(result.succeeded()).isTrue();
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

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validate_shouldFail_whenInvalidLeftOperandForAccessPolicy() {
        var constraint = atomicConstraint(USAGE_PURPOSE_LITERAL);

        ValidationResult result = accessPolicyValidator.validate(constraint);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anyMatch(msg ->
                msg.contains("UsagePurpose") && msg.contains("not allowed"));
    }

    @Test
    void validate_shouldFail_whenInvalidLeftOperandForUsagePolicy() {
        var constraint = atomicConstraint(BUSINESS_PARTNER_NUMBER_LITERAL);

        ValidationResult result = usagePolicyValidator.validate(constraint);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anyMatch(msg ->
                msg.contains("BusinessPartnerNumber") && msg.contains("not allowed"));
    }

    @Test
    void validate_shouldFail_whenLeftOperandIdIsEmpty() {
        var constraint = atomicConstraint("");

        ValidationResult result = accessPolicyValidator.validate(constraint);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anyMatch(msg ->
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

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anyMatch(msg ->
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

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validate_shouldFail_whenCompletelyInvalidJson() {
        var constraint = Json.createObjectBuilder()
                .add("invalid", "structure")
                .build();

        ValidationResult result = accessPolicyValidator.validate(constraint);

        assertThat(result.failed()).isTrue();
    }


}