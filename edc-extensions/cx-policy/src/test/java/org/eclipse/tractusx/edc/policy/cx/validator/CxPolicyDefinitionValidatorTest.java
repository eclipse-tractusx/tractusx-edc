package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.*;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.*;

class CxPolicyDefinitionValidatorTest {

    @Test
    void shouldReturnSuccess_whenValidAccessPolicy() {
        JsonObject constraint = atomicConstraint(MEMBERSHIP_LITERAL);
        JsonObject permission = rule(ACTION_ACCESS, constraint);
        JsonObject policy = policy(ODRL_PERMISSION_ATTRIBUTE, permission);
        JsonObject input = policyDefinition(policy, "some-id");

        ValidationResult result = CxPolicyDefinitionValidator.instance().validate(input);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnSuccess_whenValidUsagePolicy() {
        JsonObject constraint = atomicConstraint(USAGE_PURPOSE_LITERAL);
        JsonObject permission = rule(ACTION_USAGE, constraint);
        JsonObject policy = policy(ODRL_PERMISSION_ATTRIBUTE, permission);
        JsonObject input = policyDefinition(policy, "some-id");

        ValidationResult result = CxPolicyDefinitionValidator.instance().validate(input);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnFailure_whenPolicyTypeUnknown() {
        JsonObject constraint = atomicConstraint(USAGE_PURPOSE_LITERAL);
        JsonObject permission = rule("Unknown-type", constraint);
        JsonObject policy = policy(ODRL_PERMISSION_ATTRIBUTE, permission);
        JsonObject input = policyDefinition(policy, "some-id");

        ValidationResult result = CxPolicyDefinitionValidator.instance().validate(input);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anyMatch(msg -> msg.contains("Policy type is not recognized"));
    }

    @Test
    void shouldReturnFailure_whenPolicyMissing() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, "some-id")
                .build();

        ValidationResult result = CxPolicyDefinitionValidator.instance().validate(input);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anyMatch(msg -> msg.contains("mandatory object '"+EDC_NAMESPACE + "policy"+"' is missing"));
    }

//    @Test
//    void shouldReturnFailure_whenPolicyTypeMissing() {
//        JsonObject constraint = atomicConstraint(USAGE_PURPOSE_LITERAL);
//        JsonObject permission = ruleWithoutActionType(constraint);
//        JsonObject policy = policy(ODRL_PERMISSION_ATTRIBUTE, permission);
//        JsonObject input = policyDefinition(policy, "some-id");
//
//        ValidationResult result = CxPolicyDefinitionValidator.instance().validate(input);
//
//        assertThat(result.succeeded()).isTrue();
//        //assertThat(result.getFailureMessages()).anyMatch(msg -> msg.contains("Failed to resolve policy type"));
//    }

//    @Test
//    void shouldReturnFailure_whenPolicyTypeMissing() {
//        JsonObject constraint = atomicConstraint(MEMBERSHIP_LITERAL);
//        JsonObject permission = ruleWithoutActionType(constraint);
//        JsonObject policy = policy(ODRL_PERMISSION_ATTRIBUTE, permission);
//        JsonObject input = policyDefinition(policy, "some-id");
//
//        ValidationResult result = CxPolicyDefinitionValidator.instance().validate(input);
//
//        assertThat(result.failed()).isTrue();
//        //assertThat(result.getFailureMessages()).anyMatch(msg -> msg.contains("Failed to resolve policy type"));
//    }

//    @Test
//    void shouldReturnFailure_whenPolicyTypeMissing() {
//        JsonObject constraint = atomicConstraint(MEMBERSHIP_LITERAL);
//        JsonObject permission = emptyRule();
//        JsonObject policy = policy(ODRL_PERMISSION_ATTRIBUTE, permission);
//        JsonObject input = policyDefinition(policy, "some-id");
//
//        ValidationResult result = CxPolicyDefinitionValidator.instance().validate(input);
//
//        assertThat(result.failed()).isTrue();
//        assertThat(result.getFailureMessages()).anyMatch(msg -> msg.contains("Failed to resolve policy type"));
//    }
}