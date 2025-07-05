package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ACTION_ATTRIBUTE;

class ActionTypeIsTest {

    private static final String ACTION_ATTR = ODRL_ACTION_ATTRIBUTE;
    private static final String EXPECTED_ACTION = "odrl:use";
    private final JsonLdPath path = JsonLdPath.path();
            

    @Test
    void shouldReturnSuccess_whenActionIsStringAndMatches() {
        JsonObject input = Json.createObjectBuilder()
                .add(ACTION_ATTR, EXPECTED_ACTION)
                .build();

        var validator = new ActionTypeIs(path, EXPECTED_ACTION);
        ValidationResult result = validator.validate(input);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnSuccess_whenActionIsStringAndInUpperCase() {
        JsonObject input = Json.createObjectBuilder()
                .add(ACTION_ATTR, "odrl:USE")
                .build();

        var validator = new ActionTypeIs(path, EXPECTED_ACTION);
        ValidationResult result = validator.validate(input);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnFailure_whenActionIsStringAndDoesNotMatch() {
        JsonObject input = Json.createObjectBuilder()
                .add(ACTION_ATTR, "READ")
                .build();

        var validator = new ActionTypeIs(path, EXPECTED_ACTION);
        ValidationResult result = validator.validate(input);

        assertThat(result.failed()).isTrue();
    }

    @Test
    void validate_shouldReturnSuccess_whenActionIsObjectAndMatches() {
        JsonObject actionObj = Json.createObjectBuilder()
                .add("@id", EXPECTED_ACTION)
                .build();
        JsonObject input = Json.createObjectBuilder()
                .add(ACTION_ATTR, actionObj)
                .build();

        var validator = new ActionTypeIs(path, EXPECTED_ACTION);
        ValidationResult result = validator.validate(input);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validate_shouldReturnFailure_whenActionIsObjectAndDoesNotMatch() {
        JsonObject actionObj = Json.createObjectBuilder()
                .add("@id", "read")
                .build();
        JsonObject input = Json.createObjectBuilder()
                .add(ACTION_ATTR, actionObj)
                .build();

        var validator = new ActionTypeIs(path, EXPECTED_ACTION);
        ValidationResult result = validator.validate(input);

        assertThat(result.failed()).isTrue();
    }

    @Test
    void validate_shouldReturnSuccess_whenActionIsArrayAndFirstObjectMatches() {
        JsonObject actionObj = Json.createObjectBuilder()
                .add("@id", EXPECTED_ACTION)
                .build();
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder().add(actionObj);
        JsonObject input = Json.createObjectBuilder()
                .add(ACTION_ATTR, arrayBuilder)
                .build();

        var validator = new ActionTypeIs(path, EXPECTED_ACTION);
        ValidationResult result = validator.validate(input);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validate_shouldReturnFailure_whenActionIsArrayAndFirstObjectDoesNotMatch() {
        JsonObject actionObj = Json.createObjectBuilder()
                .add("@id", "READ")
                .build();
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder().add(actionObj);
        JsonObject input = Json.createObjectBuilder()
                .add(ACTION_ATTR, arrayBuilder)
                .build();

        var validator = new ActionTypeIs(path, EXPECTED_ACTION);
        ValidationResult result = validator.validate(input);

        assertThat(result.failed()).isTrue();
    }

    @Test
    void validate_shouldReturnFailure_whenActionIsMissing() {
        JsonObject input = Json.createObjectBuilder().build();

        var validator = new ActionTypeIs(path, EXPECTED_ACTION);
        ValidationResult result = validator.validate(input);

        assertThat(result.failed()).isTrue();
    }
}