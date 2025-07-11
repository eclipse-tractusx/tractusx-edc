package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TypedMandatoryArrayTest {
    private static final String ARRAY_ATTRIBUTE = "arrayAttribute";
    private final JsonLdPath path = JsonLdPath.path(ARRAY_ATTRIBUTE);

    @Test
    void shouldReturnSuccess_whenArrayPresent() {
        JsonArray arr = Json.createArrayBuilder().add(1).add(2).build();
        JsonObject obj = Json.createObjectBuilder().add(ARRAY_ATTRIBUTE, arr).build();

        TypedMandatoryArray validator = new TypedMandatoryArray(path);
        ValidationResult result = validator.validate(obj);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnFailure_whenArrayMissing() {
        JsonObject obj = Json.createObjectBuilder().build();

        TypedMandatoryArray validator = new TypedMandatoryArray(path);
        ValidationResult result = validator.validate(obj);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anyMatch(msg -> msg.contains("Mandatory array '%s' is missing".formatted(path)));
    }

    @Test
    void shouldReturnFailure_whenNotArray() {
        JsonObject obj = Json.createObjectBuilder().add(ARRAY_ATTRIBUTE, "notAnArray").build();

        TypedMandatoryArray validator = new TypedMandatoryArray(path);
        ValidationResult result = validator.validate(obj);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anyMatch(msg -> msg.contains("Expected array for '%s'".formatted(path)));
    }

    @Test
    void shouldReturnSuccess_whenArrayMeetsMin() {
        JsonArray arr = Json.createArrayBuilder().add(1).add(2).build();
        JsonObject obj = Json.createObjectBuilder().add(ARRAY_ATTRIBUTE, arr).build();

        TypedMandatoryArray validator = new TypedMandatoryArray(path, 2, false);

        ValidationResult result = validator.validate(obj);
        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnFailure_whenArrayBelowMin() {
        JsonArray arr = Json.createArrayBuilder().add(1).build();
        JsonObject obj = Json.createObjectBuilder().add(ARRAY_ATTRIBUTE, arr).build();

        TypedMandatoryArray validator = new TypedMandatoryArray(path, 2, false);
        ValidationResult result = validator.validate(obj);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anyMatch(
                msg -> msg.contains("Array '%s' should at least contains '%s' elements".formatted(path, 2)));
    }

    @Test
    void validate_shouldReturnSuccess_whenOrAbsentAndMissing() {
        JsonObject obj = Json.createObjectBuilder().build();

        TypedMandatoryArray validator = new TypedMandatoryArray(path, null, true);
        ValidationResult result = validator.validate(obj);

        assertThat(result.succeeded()).isTrue();
    }
}