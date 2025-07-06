package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArrayIsEmptyTest {

    private final JsonLdPath path = JsonLdPath.path();
    private final String ARRAY_ATTRIBUTE = "testArray";

    private ArrayIsEmpty validator;

    @BeforeEach
    void setUp() {
        validator = new ArrayIsEmpty(path, ARRAY_ATTRIBUTE);
    }

    @Test
    void shouldReturnSuccess_whenArrayAttributeMissing() {
        JsonObject input = Json.createObjectBuilder()
                .add("other", "value")
                .build();

        ValidationResult result = validator.validate(input);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnSuccess_whenArrayIsEmpty() {
        JsonArray emptyArray = Json.createArrayBuilder().build();
        JsonObject input = Json.createObjectBuilder()
                .add(ARRAY_ATTRIBUTE, emptyArray)
                .build();

        ValidationResult result = validator.validate(input);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnFailure_whenArrayIsNotEmpty() {
        JsonArray array = Json.createArrayBuilder()
                .add("item")
                .build();
        JsonObject input = Json.createObjectBuilder()
                .add(ARRAY_ATTRIBUTE, array)
                .build();

        ValidationResult result = validator.validate(input);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailure().getMessages()).anyMatch(v -> v.contains(ARRAY_ATTRIBUTE));
    }

    @Test
    void shouldReturnFailure_whenAttributeIsNotArray() {
        JsonObject input = Json.createObjectBuilder()
                .add(ARRAY_ATTRIBUTE, "notAnArray")
                .build();

        ValidationResult result = validator.validate(input);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailure().getMessages()).anyMatch(v -> v.contains(ARRAY_ATTRIBUTE));
    }

    @Test
    void shouldReturnSuccess_whenAttributeIsNull() {
        JsonObject input = Json.createObjectBuilder()
                .addNull(ARRAY_ATTRIBUTE)
                .build();

        ValidationResult result = validator.validate(input);

        assertThat(result.succeeded()).isTrue();
    }
}