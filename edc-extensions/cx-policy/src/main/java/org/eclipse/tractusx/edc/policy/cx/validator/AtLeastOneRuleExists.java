package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.*;
import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Validator that ensures a policy contains at least one rule of any type.
 * <p>
 * This validator enforces that the policy must have at least one:
 * <ul>
 *   <li>Permission rule</li>
 *   <li>Obligation rule</li>
 *   <li>Prohibition rule</li>
 * </ul>
 * <p>
 */
public class AtLeastOneRuleExists implements Validator<JsonObject> {
    private final JsonLdPath path;

    public AtLeastOneRuleExists(JsonLdPath path) {
        this.path = path;
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        boolean hasPermission = hasNonEmptyArray(input, ODRL_PERMISSION_ATTRIBUTE);
        boolean hasObligation = hasNonEmptyArray(input, ODRL_OBLIGATION_ATTRIBUTE);
        boolean hasProhibition = hasNonEmptyArray(input, ODRL_PROHIBITION_ATTRIBUTE);

        if (hasPermission || hasObligation || hasProhibition) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(
                violation("Policy must contain at least one permission, obligation, or prohibition", path.toString())
        );
    }

    private boolean hasNonEmptyArray(JsonObject input, String attribute) {
        return input.containsKey(attribute) &&
                input.get(attribute).getValueType() == JsonValue.ValueType.ARRAY &&
                !input.get(attribute).asJsonArray().isEmpty();
    }
}
