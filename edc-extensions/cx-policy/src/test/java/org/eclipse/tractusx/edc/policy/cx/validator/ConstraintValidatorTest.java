package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.Json;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.*;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.atomicConstraint;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.logicalConstraint;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.*;

class ConstraintValidatorTest {

    private JsonObjectValidator accessPolicyValidator;

    @BeforeEach
    void setUp() {
        accessPolicyValidator = ConstraintValidator.instance(JsonObjectValidator.newValidator(), ACCESS_POLICY_TYPE).build();
    }

    @Test
    void shouldReturnSuccess_whenValidAtomicConstraint() {
        var constraint = atomicConstraint(FRAMEWORK_AGREEMENT_LITERAL);

        ValidationResult result = accessPolicyValidator.validate(constraint);

        assertThat(result.succeeded()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {ODRL_AND_CONSTRAINT_ATTRIBUTE})
    void shouldReturnSuccess_whenAllowedLogicalOperator(String operator) {
        var logicalConstraint = logicalConstraint(operator,
                atomicConstraint(FRAMEWORK_AGREEMENT_LITERAL),
                atomicConstraint(MEMBERSHIP_LITERAL));

        ValidationResult result = accessPolicyValidator.validate(logicalConstraint);

        assertThat(result.succeeded()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {ODRL_OR_CONSTRAINT_ATTRIBUTE, ODRL_XONE_CONSTRAINT_ATTRIBUTE, ODRL_AND_SEQUENCE_CONSTRAINT_ATTRIBUTE})
    void shouldReturnFailure_whenNotAllowedLogicalOperator(String operator) {
        var logicalConstraint = logicalConstraint(operator,
                atomicConstraint(FRAMEWORK_AGREEMENT_LITERAL),
                atomicConstraint(MEMBERSHIP_LITERAL));

        ValidationResult result = accessPolicyValidator.validate(logicalConstraint);

        assertThat(result.failed()).isTrue();
    }

    @Test
    void shouldReturnFailure_whenEmptyConstraint() {
        var constraint = Json.createObjectBuilder().build();

        ValidationResult result = accessPolicyValidator.validate(constraint);

        assertThat(result.failed()).isTrue();
    }

}