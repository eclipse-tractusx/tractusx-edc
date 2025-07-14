package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.*;
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
    void shouldReturnSuccess_whenValidAccessPolicyWithLogicalConstraint() {
        JsonObject constraint = atomicConstraint(MEMBERSHIP_LITERAL);
        JsonObject logicalConstraint = logicalConstraint(ODRL_AND_CONSTRAINT_ATTRIBUTE, constraint);
        JsonObject permission = rule(ACTION_ACCESS, logicalConstraint);
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
        assertThat(result.getFailureMessages()).anyMatch(msg -> msg.contains("mandatory object '"+EDC_NAMESPACE + "policy' is missing"));
    }

    @Test
    void shouldReturnFailure_whenPolicyTypeMissing() {
        JsonObject constraint = atomicConstraint(USAGE_PURPOSE_LITERAL);
        JsonObject permission = ruleWithoutActionType(constraint);
        JsonObject policy = policy(ODRL_PERMISSION_ATTRIBUTE, permission);
        JsonObject input = policyDefinition(policy, "some-id");

        ValidationResult result = CxPolicyDefinitionValidator.instance().validate(input);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anyMatch(msg -> msg.contains("Policy type is not recognized"));
    }

    @Test
    void shouldReturnSuccess_whenPolicyContainEmptyRules() {
        JsonObject policy = Json.createObjectBuilder()
                .add(TYPE, Json.createArrayBuilder().add(ODRL_POLICY_TYPE_SET))
                .add(ODRL_PERMISSION_ATTRIBUTE, Json.createArrayBuilder())
                .build();
        JsonObject input = policyDefinition(policy, "some-id");

        ValidationResult result = CxPolicyDefinitionValidator.instance().validate(input);

        assertThat(result.succeeded()).isTrue();
    }
    @Test
    void shouldReturnFailure_whenRuleContainDifferentPolicyType() {
        JsonObject usageConstraint = atomicConstraint(USAGE_PURPOSE_LITERAL);
        JsonObject usagePermission = rule(ACTION_USAGE, usageConstraint);
        JsonObject accessConstraint = atomicConstraint(MEMBERSHIP_LITERAL);
        JsonObject accessPermission = rule(ACTION_ACCESS, accessConstraint);
        JsonObject policy = policy(ODRL_PERMISSION_ATTRIBUTE, usagePermission, accessPermission);
        JsonObject input = policyDefinition(policy, "some-id");

        ValidationResult result = CxPolicyDefinitionValidator.instance().validate(input);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anyMatch(msg -> msg.contains("Action was expected to be '%s' but was '%s'".formatted(ACTION_USAGE, ACTION_ACCESS)));
    }
}