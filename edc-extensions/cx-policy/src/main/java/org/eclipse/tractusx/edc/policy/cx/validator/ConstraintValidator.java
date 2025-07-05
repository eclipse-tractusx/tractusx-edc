package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.*;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.Set;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.*;
import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.eclipse.tractusx.edc.policy.cx.validator.AllowedConstraints.*;

public class ConstraintValidator implements Validator<JsonObject> {
    private final JsonLdPath path;
    private final String policyType;

    private ConstraintValidator(JsonLdPath path, String policyType) {
        this.path = path;
        this.policyType = policyType;
    }

    public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder, String policyType) {
        return builder.verify(path -> new ConstraintValidator(path, policyType));
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        boolean isLogical = input.keySet().stream()
                .anyMatch(key -> ALLOWED_LOGICAL_CONSTRAINTS.contains(key) ||
                        NOT_ALLOWED_LOGICAL_CONSTRAINTS.contains(key));

        return isLogical ?
                new LogicalConstraintValidator(path, policyType).validate(input) :
                validateAtomicConstraint(input, path, policyType);
    }

    private static ValidationResult validateAtomicConstraint(JsonObject input, JsonLdPath path, String policyType) {
        return JsonObjectValidator.newValidator()
                .verify(ODRL_LEFT_OPERAND_ATTRIBUTE, MandatoryObject::new)
                .verifyObject(ODRL_LEFT_OPERAND_ATTRIBUTE, b -> LeftOperandValidator.instance(b, policyType))
                .verify(ODRL_OPERATOR_ATTRIBUTE, MandatoryObject::new)
                .verifyObject(ODRL_OPERATOR_ATTRIBUTE, b -> b.verifyId(OptionalIdNotBlank::new))
                .verify(ODRL_RIGHT_OPERAND_ATTRIBUTE, MandatoryValue::new)
                .build()
                .validate(input);
    }

    private record LogicalConstraintValidator(JsonLdPath path, String policyType) implements Validator<JsonObject> {

        @Override
            public ValidationResult validate(JsonObject input) {
                // Check for not allowed logical constraints
                // TODO: Check only against allowed operator
                var notAllowedKeys = input.keySet().stream()
                        .filter(NOT_ALLOWED_LOGICAL_CONSTRAINTS::contains)
                        .toList();
                if (!notAllowedKeys.isEmpty()) {
                    return ValidationResult.failure(
                            violation("Policy includes not allowed Logical constraints: " + notAllowedKeys,
                                    path.toString())
                    );
                }

                // Validate allowed logical constraints
                var logicalKeys = input.keySet().stream()
                        .filter(ALLOWED_LOGICAL_CONSTRAINTS::contains)
                        .toList();
                if (logicalKeys.size() != 1) {
                    return ValidationResult.failure(
                            violation("Policy must have exactly one Logical constraint: " + logicalKeys,
                                    path.toString())
                    );
                }

                return validateLogicalConstraintContent(input, logicalKeys.get(0));
            }

            private ValidationResult validateLogicalConstraintContent(JsonObject input, String logicalKey) {
                return JsonObjectValidator.newValidator()
                        .verify(logicalKey, MandatoryArray::new)
                        .verifyArrayItem(logicalKey, b -> ConstraintValidator.instance(b, policyType))
                        .build()
                        .validate(input);
            }
        }

    private static final class LeftOperandValidator {
        public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder, String policyType) {
            // TODO: Should we check the left operands in expanded form?
            return builder
                    .verifyId(MandatoryIdNotBlank::new)
                    .verify(ID, path -> new ValueIn(path,
                            policyType.equals(ACCESS_POLICY_TYPE) ? ACCESS_POLICY_ALLOWED_LEFT_OPERANDS : USAGE_POLICY_ALLOWED_LEFT_OPERANDS));
        }


    }

    private static final class ValueIn implements Validator<JsonObject> {
        private final Set<String> allowedValues;
        private final JsonLdPath path;
        private final String id;

        private ValueIn(JsonLdPath path, Set<String> allowedValues) {
            this.path = path;
            this.allowedValues = allowedValues;
            this.id = path.last();
        }

        @Override
        public ValidationResult validate(JsonObject input) {
            // TODO: Should we check the left operands in expanded form?
            var value = input.getString(id, null);
            if (value == null || value.isBlank() || value.substring(value.lastIndexOf("/") + 1).isBlank()) {
                return ValidationResult.failure(
                        violation("leftOperand.'"+ id +"' value can not be null or empty", path.toString())
                );
            }
            if (!allowedValues.contains(value)) {
                return ValidationResult.failure(
                        violation("leftOperand.@id value '" + value + "' is not allowed", path.toString())
                );
            }
            return ValidationResult.success();
        }
    }
}
