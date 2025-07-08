package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_AND_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OR_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.atomicConstraint;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.logicalConstraint;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACCESS_POLICY_TYPE;

class LogicalConstraintValidatorTest {

    private final JsonLdPath path = JsonLdPath.path();
    private final String VALID_ACCESS_POLICY_LEFT_OPERAND = "https://w3id.org/catenax/policy/FrameworkAgreement";

    @Test
    void shouldReturnSuccess_whenValidAndConstraint() {
        JsonObject constraint = atomicConstraint(VALID_ACCESS_POLICY_LEFT_OPERAND);
        JsonObject input = logicalConstraint(ODRL_AND_CONSTRAINT_ATTRIBUTE, constraint);

        ValidationResult result = validateLogicalConstraint(input, ACCESS_POLICY_TYPE);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnFailure_whenNotAllowedLogicalConstraint() {
        JsonObject constraint = atomicConstraint(VALID_ACCESS_POLICY_LEFT_OPERAND);
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
        return LogicalConstraintValidator.instance(path, policyType).validate(input);
    }
}