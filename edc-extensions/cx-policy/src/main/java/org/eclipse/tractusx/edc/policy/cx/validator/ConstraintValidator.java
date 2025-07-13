package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ALLOWED_LOGICAL_CONSTRAINTS;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.NOT_ALLOWED_LOGICAL_CONSTRAINTS;

public class ConstraintValidator implements Validator<JsonObject> {
    private final JsonLdPath path;
    private final String policyType;
    private final String ruleType;

    private ConstraintValidator(JsonLdPath path, String policyType, String ruleType) {
        this.path = path;
        this.policyType = policyType;
        this.ruleType = ruleType;
    }

    public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder, String policyType, String ruleType) {
        return builder.verify(path -> new ConstraintValidator(path, policyType, ruleType));
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        boolean isLogical = input.keySet().stream()
                .anyMatch(key -> ALLOWED_LOGICAL_CONSTRAINTS.contains(key) ||
                        NOT_ALLOWED_LOGICAL_CONSTRAINTS.contains(key));

        return isLogical ?
                LogicalConstraintValidator.instance(path, policyType, ruleType).validate(input) :
                AtomicConstraintValidator.instance(path, policyType, ruleType).validate(input);
    }
}
