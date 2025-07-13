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
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACCESS_POLICY_TYPE;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.USAGE_POLICY_TYPE;

class LeftOperandValidatorTest {

    private final JsonLdPath path = JsonLdPath.path();

    @Test
    void shouldReturnSuccess_whenValidAccessPolicyLeftOperand() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, "https://w3id.org/catenax/policy/FrameworkAgreement")
                .build();

        ValidationResult result = validateLeftOperand(input, ACCESS_POLICY_TYPE);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnSuccess_whenValidUsagePolicyLeftOperand() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, "https://w3id.org/catenax/policy/UsagePurpose")
                .build();

        ValidationResult result = validateLeftOperand(input, USAGE_POLICY_TYPE);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnFailure_whenIdIsMissing() {
        JsonObject input = Json.createObjectBuilder().build();

        ValidationResult result = validateLeftOperand(input, ACCESS_POLICY_TYPE);

        assertThat(result.failed()).isTrue();
    }

    @Test
    void shouldReturnFailure_whenLeftOperandNotAllowedForAccessPolicy() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, "https://w3id.org/catenax/policy/UsagePurpose")
                .build();

        ValidationResult result = validateLeftOperand(input, ACCESS_POLICY_TYPE);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages())
                .anyMatch(msg -> msg.contains("is not allowed"));
    }

    @Test
    void shouldReturnFailure_whenLeftOperandNotAllowedForUsagePolicy() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, "https://w3id.org/catenax/policy/BusinessPartnerGroup")
                .build();

        ValidationResult result = validateLeftOperand(input, USAGE_POLICY_TYPE);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages())
                .anyMatch(msg -> msg.contains("is not allowed"));
    }

    @Test
    void shouldReturnFailure_whenLeftOperandIsCompletelyInvalid() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, "invalid-operand")
                .build();

        ValidationResult result = validateLeftOperand(input, ACCESS_POLICY_TYPE);

        assertThat(result.failed()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "https://example.com/"})
    void shouldReturnFailure_whenIdEndsWithSlashOrEmpty(String id) {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, id)
                .build();

        ValidationResult result = validateLeftOperand(input, ACCESS_POLICY_TYPE);

        assertThat(result.failed()).isTrue();
    }

    private ValidationResult validateLeftOperand(JsonObject input, String policyType) {
        return LeftOperandValidator.instance(JsonObjectValidator.newValidator(), policyType, ODRL_PERMISSION_ATTRIBUTE)
                .build()
                .validate(input);
    }
}