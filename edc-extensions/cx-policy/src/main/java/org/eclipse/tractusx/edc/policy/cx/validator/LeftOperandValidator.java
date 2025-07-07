package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryIdNotBlank;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.Set;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.*;

public class LeftOperandValidator implements Validator<JsonObject> {
    private final JsonLdPath path;
    private final String policyType;

    private LeftOperandValidator(JsonLdPath path, String policyType) {
        this.path = path;
        this.policyType = policyType;
    }

    public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder, String policyType) {
        return builder.verify(path -> new LeftOperandValidator(path, policyType));
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        return JsonObjectValidator.newValidator()
                .verifyId(MandatoryIdNotBlank::new)
                .verify(ID, path -> new ValueIn(path, getAllowedLeftOperands()))
                .build()
                .validate(input);
    }

    private Set<String> getAllowedLeftOperands() {
        return policyType.equals(ACCESS_POLICY_TYPE) ?
                ACCESS_POLICY_ALLOWED_LEFT_OPERANDS :
                USAGE_POLICY_ALLOWED_LEFT_OPERANDS;
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
            var value = input.getString(id, null);
            if (value == null || value.isBlank() || value.substring(value.lastIndexOf("/") + 1).isBlank()) {
                return ValidationResult.failure(
                        violation("leftOperand.'" + id + "' value can not be null or empty. Expected one of: " + allowedValues, path.toString())
                );
            }
            if (!allowedValues.contains(value)) {
                return ValidationResult.failure(
                        violation("leftOperand.@id value '" + value + "' is not allowed. Expected one of: " + allowedValues, path.toString())
                );
            }
            return ValidationResult.success();
        }
    }
}