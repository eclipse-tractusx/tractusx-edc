package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ACTION_ATTRIBUTE;
import static org.eclipse.edc.validator.spi.Violation.violation;

public class ActionTypeIs implements Validator<JsonObject> {
    private final JsonLdPath path;
    private final String expectedAction;

    public ActionTypeIs(JsonLdPath path, String expectedAction) {
        this.path = path;
        this.expectedAction = expectedAction;
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        if (!input.containsKey(ODRL_ACTION_ATTRIBUTE)) {
            return ValidationResult.failure(
                    violation("Action property is missing", path.toString())
            );
        }

        var action = input.get(ODRL_ACTION_ATTRIBUTE);
        String actionValue = "";

        switch (action.getValueType()) {
            case STRING:
                actionValue = action.toString().replaceAll("\"", "");
                break;
            case OBJECT:
                actionValue = action.asJsonObject().getString("@id", "");
                break;
            case ARRAY:
                JsonArray actionArray = action.asJsonArray();
                if (!actionArray.isEmpty() && actionArray.get(0).getValueType() == JsonValue.ValueType.OBJECT) {
                    actionValue = actionArray.getJsonObject(0).getString("@id", "");
                }
                break;
            default:
                break;
        }
        if (actionValue.equalsIgnoreCase(expectedAction)) {
            return ValidationResult.success();
        }

        return ValidationResult.failure(
                violation("Action was expected to be '%s' but was '%s'".formatted(expectedAction, actionValue),
                        path.append(ODRL_ACTION_ATTRIBUTE).toString())
        );

    }
}
