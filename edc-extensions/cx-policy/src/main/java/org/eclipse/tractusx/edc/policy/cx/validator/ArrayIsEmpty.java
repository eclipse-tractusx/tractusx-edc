package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.validator.spi.Violation.violation;

public class ArrayIsEmpty implements Validator<JsonObject> {
    private final JsonLdPath path;
    private final String arrayAttribute;

    public ArrayIsEmpty(JsonLdPath path, String arrayAttribute) {
        this.path = path;
        this.arrayAttribute = arrayAttribute;
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        // Missing attribute - return success
        if (!input.containsKey(arrayAttribute)) {
            return ValidationResult.success();
        }

        var value = input.get(arrayAttribute);

        // Null value - return success
        if (value.getValueType() == JsonValue.ValueType.NULL) {
            return ValidationResult.success();
        }

        // Non-array type - return failure with specific message
        if (value.getValueType() != JsonValue.ValueType.ARRAY) {
            return ValidationResult.failure(
                    violation(arrayAttribute + " must be of type array", path.toString())
            );
        }

        // Array type - must be empty
        if (!value.asJsonArray().isEmpty()) {
            return ValidationResult.failure(
                    violation(arrayAttribute + " must be empty", path.toString())
            );
        }
        return ValidationResult.success();
    }
}
