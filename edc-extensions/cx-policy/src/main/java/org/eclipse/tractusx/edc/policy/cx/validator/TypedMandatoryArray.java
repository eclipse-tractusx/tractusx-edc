package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;
import static org.eclipse.edc.validator.spi.Violation.violation;

public class TypedMandatoryArray implements Validator<JsonObject> {
    private final JsonLdPath path;
    private final Integer min;

    public TypedMandatoryArray(JsonLdPath path) {
        this(path, null);
    }

    public TypedMandatoryArray(JsonLdPath path, Integer min) {
        this.path = path;
        this.min = min;
    }

    public static Function<JsonLdPath, Validator<JsonObject>> min(Integer min) {
        return path -> new TypedMandatoryArray(path, min);
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        return Optional.ofNullable(input.get(path.last()))
                .map(value -> {
                    if (value.getValueType() != JsonValue.ValueType.ARRAY) {
                        return ValidationResult.failure(violation(format("Expected array for '%s' but found %s",
                                path, value.getValueType().toString().toLowerCase()), path.toString()));
                    }
                    return validateMin(value.asJsonArray());
                })
                .orElseGet(() -> ValidationResult.failure(violation(format("mandatory array '%s' is missing", path), path.toString())));
    }

    private ValidationResult validateMin(JsonArray array) {
        if (min == null || (array.size() >= min)) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(violation(format("array '%s' should at least contains '%s' elements", path, min), path.toString()));
    }
}
