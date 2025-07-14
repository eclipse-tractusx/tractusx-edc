package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.*;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.*;

class LeftOperandValidatorTest {

    private final JsonLdPath path = JsonLdPath.path();

    @Test
    void shouldReturnSuccess_whenValidAccessPermissionPolicyLeftOperand() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, FRAMEWORK_AGREEMENT_LITERAL)
                .build();

        ValidationResult result = validateLeftOperand(input, ACCESS_POLICY_TYPE, ODRL_PERMISSION_ATTRIBUTE);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnSuccess_whenValidUsagePermissionPolicyLeftOperand() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, USAGE_PURPOSE_LITERAL)
                .build();

        ValidationResult result = validateLeftOperand(input, USAGE_POLICY_TYPE, ODRL_PERMISSION_ATTRIBUTE);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnSuccess_whenValidUsageProhibitionPolicyLeftOperand() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, USAGE_PURPOSE_LITERAL)
                .build();

        ValidationResult result = validateLeftOperand(input, USAGE_POLICY_TYPE, ODRL_PROHIBITION_ATTRIBUTE);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnSuccess_whenValidUsageObligationPolicyLeftOperand() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, AFFILIATES_REGION_LITERAL)
                .build();

        ValidationResult result = validateLeftOperand(input, USAGE_POLICY_TYPE, ODRL_OBLIGATION_ATTRIBUTE);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnFailure_whenIdIsMissing() {
        JsonObject input = Json.createObjectBuilder().build();

        ValidationResult result = validateLeftOperand(input, ACCESS_POLICY_TYPE, ODRL_PERMISSION_ATTRIBUTE);

        assertThat(result.failed()).isTrue();
    }

    @Test
    void shouldReturnFailure_whenLeftOperandNotAllowedForAccessPermissionPolicy() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, USAGE_PURPOSE_LITERAL)
                .build();

        ValidationResult result = validateLeftOperand(input, ACCESS_POLICY_TYPE, ODRL_PERMISSION_ATTRIBUTE);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages())
                .anyMatch(msg -> msg.contains("is not allowed"));
    }

    @Test
    void shouldReturnFailure_whenLeftOperandNotAllowedForUsagePermissionPolicy() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, BUSINESS_PARTNER_GROUP_LITERAL)
                .build();

        ValidationResult result = validateLeftOperand(input, USAGE_POLICY_TYPE, ODRL_PERMISSION_ATTRIBUTE);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages())
                .anyMatch(msg -> msg.contains("is not allowed"));
    }

    @Test
    void shouldReturnFailure_whenLeftOperandNotAllowedForUsageProhibitionPolicy() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, AFFILIATES_REGION_LITERAL)
                .build();

        ValidationResult result = validateLeftOperand(input, USAGE_POLICY_TYPE, ODRL_PROHIBITION_ATTRIBUTE);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages())
                .anyMatch(msg -> msg.contains("is not allowed"));
    }

    @Test
    void shouldReturnFailure_whenLeftOperandNotAllowedForUsageObligationPolicy() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, USAGE_PURPOSE_LITERAL)
                .build();

        ValidationResult result = validateLeftOperand(input, USAGE_POLICY_TYPE, ODRL_OBLIGATION_ATTRIBUTE);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages())
                .anyMatch(msg -> msg.contains("is not allowed"));
    }

    @Test
    void shouldReturnFailure_whenLeftOperandIsCompletelyInvalid() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, "invalid-operand")
                .build();

        ValidationResult result = validateLeftOperand(input, ACCESS_POLICY_TYPE, ODRL_PERMISSION_ATTRIBUTE);

        assertThat(result.failed()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "https://example.com/"})
    void shouldReturnFailure_whenIdEndsWithSlashOrEmpty(String id) {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, id)
                .build();

        ValidationResult result = validateLeftOperand(input, ACCESS_POLICY_TYPE, ODRL_PERMISSION_ATTRIBUTE);

        assertThat(result.failed()).isTrue();
    }

    private ValidationResult validateLeftOperand(JsonObject input, String policyType, String ruleType) {
        return LeftOperandValidator.instance(JsonObjectValidator.newValidator(), policyType, ruleType)
                .build()
                .validate(input);
    }
}