package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.*;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACCESS_POLICY_TYPE;

/**
 * Resolves the type of policy based on the action type of its rules.
 * It checks for a non-empty permission, obligations, or prohibitions and determines the
 * policy type from the first found action.
 * If all of these are empty, it defaults to the access policy type.
 */
public class PolicyTypeResolver {
    public static String resolve(JsonObject policy) {
        if (policy.containsKey(ODRL_PERMISSION_ATTRIBUTE)) {
            JsonValue permissions = policy.get(ODRL_PERMISSION_ATTRIBUTE);
            if (!permissions.asJsonArray().isEmpty()) {
                return getActionFromRule(permissions.asJsonArray().get(0).asJsonObject());
            }
        }

        if (policy.containsKey(ODRL_OBLIGATION_ATTRIBUTE)) {
            JsonValue obligations = policy.get(ODRL_OBLIGATION_ATTRIBUTE);
            if (!obligations.asJsonArray().isEmpty()) {
                return getActionFromRule(obligations.asJsonArray().get(0).asJsonObject());
            }
        }

        if (policy.containsKey(ODRL_PROHIBITION_ATTRIBUTE)) {
            JsonValue prohibitions = policy.get(ODRL_PROHIBITION_ATTRIBUTE);
            if (!prohibitions.asJsonArray().isEmpty()) {
                return getActionFromRule(prohibitions.asJsonArray().get(0).asJsonObject());
            }
        }

        return ACCESS_POLICY_TYPE;
    }

    private static String getActionFromRule(JsonObject rule) {
        if (rule.containsKey(ODRL_ACTION_ATTRIBUTE)) {
            JsonValue action = rule.get(ODRL_ACTION_ATTRIBUTE);
            switch (action.getValueType()) {
                case STRING:
                    return action.toString().replaceAll("\"", "");
                case OBJECT:
                    return action.asJsonObject().getString("@id", "");
                case ARRAY:
                    JsonArray actionArray = action.asJsonArray();
                    if (!actionArray.isEmpty() && actionArray.get(0).getValueType() == JsonValue.ValueType.OBJECT) {
                        return actionArray.getJsonObject(0).getString("@id", "");
                    }
                    break;
                default:
                    break;
            }

            return action.toString();
        }
        return "";
    }
}
