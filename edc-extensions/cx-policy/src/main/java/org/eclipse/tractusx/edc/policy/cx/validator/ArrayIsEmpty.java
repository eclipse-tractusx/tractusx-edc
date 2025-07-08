package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.validator.spi.Violation.violation;

public class ArrayIsEmpty implements Validator<JsonObject> {
    private final JsonLdPath path;

    public ArrayIsEmpty(JsonLdPath path) {
        this.path = path;
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        // Missing attribute - return success
        String key = path.last();
        if (!input.containsKey(path.last())) {
            return ValidationResult.success();
        }

        var value = input.get(path.last());

        // Null value - return success
        if (value.getValueType() == JsonValue.ValueType.NULL) {
            return ValidationResult.success();
        }

        // Non-array type - return failure with specific message
        if (value.getValueType() != JsonValue.ValueType.ARRAY) {
            return ValidationResult.failure(
                    violation("Array must be of type array", path.toString())
            );
        }

        // Array type - must be empty
        if (!value.asJsonArray().isEmpty()) {
            return ValidationResult.failure(
                    violation("Array must be empty", path.toString())
            );
        }
        return ValidationResult.success();
    }
}
