package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ALLOWED_LOGICAL_CONSTRAINTS;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.NOT_ALLOWED_LOGICAL_CONSTRAINTS;

public class LogicalConstraintValidator implements Validator<JsonObject> {
    private final JsonLdPath path;
    private final String policyType;
    private final String ruleType;

    private LogicalConstraintValidator(JsonLdPath path, String policyType, String ruleType) {
        this.path = path;
        this.policyType = policyType;
        this.ruleType = ruleType;
    }

    public static LogicalConstraintValidator instance(JsonLdPath path, String policyType, String ruleType) {
        return new LogicalConstraintValidator(path, policyType, ruleType);
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        var invalidConstraintsResult = validateNoInvalidConstraints(input);
        if (invalidConstraintsResult.failed()) {
            return invalidConstraintsResult;
        }

        var logicalConstraints = input.keySet().stream()
                .filter(ALLOWED_LOGICAL_CONSTRAINTS::contains)
                .toList();
        if (logicalConstraints.size() != 1) {
            return ValidationResult.failure(
                    violation("Policy must have exactly one logical constraint: " + logicalConstraints,
                            path.toString())
            );
        }
        return validateLogicalConstraintContent(input, logicalConstraints.get(0));
    }

    private ValidationResult validateLogicalConstraintContent(JsonObject input, String logicalConstraint) {
        var arrayValidationResult = validateLogicalConstraintIsArray(input, logicalConstraint);
        if (arrayValidationResult.failed()) {
            return arrayValidationResult;
        }
        return JsonObjectValidator.newValidator()
                .verifyArrayItem(logicalConstraint, b -> ConstraintValidator.instance(b, policyType, ruleType))
                .build()
                .validate(input);
    }

    private ValidationResult validateLogicalConstraintIsArray(JsonObject input, String logicalConstraint) {
        return JsonObjectValidator.newValidator()
                .verify(logicalConstraint, TypedMandatoryArray::new)
                .build()
                .validate(input);
    }

    private ValidationResult validateNoInvalidConstraints(JsonObject input) {
        var invalidLogicalConstraints = input.keySet().stream()
                .filter(NOT_ALLOWED_LOGICAL_CONSTRAINTS::contains)
                .toList();

        if (!invalidLogicalConstraints.isEmpty()) {
            return ValidationResult.failure(
                    violation("Policy includes not allowed logical constraints: " + invalidLogicalConstraints, path.toString())
            );
        }
        return ValidationResult.success();
    }
}
