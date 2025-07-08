package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.*;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.*;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.*;

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

//    @Test
//    void shouldReturnFailure_whenDutyArrayIsNotEmpty() {
//        JsonObject input = rule(ACTION_ACCESS, ODRL_DUTY_ATTRIBUTE);
//
//        ValidationResult result = validator.validate(input);
//
//        assertThat(result.failed()).isTrue();
//    }
//
//    @Test
//    void shouldReturnSuccess_whenDutyArrayIsEmpty() {
//        JsonObject input = Json.createObjectBuilder()
//                .add(ODRL_DUTY_ATTRIBUTE, Json.createArrayBuilder())
//                .build();
//
//        ValidationResult result = validator.validate(input);
//
//        assertThat(result.succeeded()).isTrue();
//    }

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