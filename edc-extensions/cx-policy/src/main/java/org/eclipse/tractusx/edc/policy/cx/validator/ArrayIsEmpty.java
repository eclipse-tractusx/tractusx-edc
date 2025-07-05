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
        if (!input.containsKey(arrayAttribute)) {
            return ValidationResult.success();
        }

        var array = input.get(arrayAttribute);
        if (array.getValueType() != JsonValue.ValueType.ARRAY || !array.asJsonArray().isEmpty()) {
            return ValidationResult.failure(
                    violation("Access policy must not contain any " + arrayAttribute, path.toString())
            );
        }

        return ValidationResult.success();
    }
}
