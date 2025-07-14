package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ALLOWED_LOGICAL_CONSTRAINTS;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.NOT_ALLOWED_LOGICAL_CONSTRAINTS;

/**
 * Validator that routes constraint validation to either logical or atomic constraint validators
 * based on the constraint type detected in the JSON object.
 * <p>
 * This validator acts as a dispatcher that:
 * <ul>
 *   <li>Analyzes the constraint structure to determine if it's logical or atomic</li>
 *   <li>Routes logical constraints to {@link LogicalConstraintValidator}</li>
 *   <li>Routes atomic constraints to {@link AtomicConstraintValidator}</li>
 * </ul>
 * <p>
 * The validator is configured with policy type and rule type to pass appropriate
 * context to the underlying specialized validators.
 */
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
