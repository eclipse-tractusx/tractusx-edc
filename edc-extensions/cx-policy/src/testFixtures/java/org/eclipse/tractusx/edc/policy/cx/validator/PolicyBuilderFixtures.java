package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.*;

import java.util.Collection;
import java.util.Map;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.*;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.*;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACCESS_POLICY_TYPE;

public final class PolicyBuilderFixtures {

    private PolicyBuilderFixtures() {}

    public static JsonObject atomicConstraint(String leftOperand, String operator, Object rightOperand) {
        var builder = Json.createObjectBuilder()
                .add(TYPE, ODRL_CONSTRAINT_TYPE)
                .add(ODRL_LEFT_OPERAND_ATTRIBUTE, Json.createArrayBuilder().add(Json.createObjectBuilder().add(ID, leftOperand)))
                .add(ODRL_OPERATOR_ATTRIBUTE, Json.createArrayBuilder().add(Json.createObjectBuilder().add(ID, operator)));

        if (rightOperand instanceof Collection<?> coll) {
            var rightArray = Json.createArrayBuilder();
            coll.forEach(item -> rightArray.add(Json.createObjectBuilder().add(VALUE, item.toString())));
            builder.add(ODRL_RIGHT_OPERAND_ATTRIBUTE, rightArray);
        } else {
            builder.add(ODRL_RIGHT_OPERAND_ATTRIBUTE, Json.createArrayBuilder().add(Json.createObjectBuilder().add(VALUE, rightOperand.toString())));
        }
        return builder.build();
    }

    public static JsonObject atomicConstraint(String leftOperand) {
        return atomicConstraint(leftOperand, "odrl:eq", "test-value");
    }

    public static JsonObject logicalConstraint(String constraintType, JsonObject... constraints) {
        var arrayBuilder = Json.createArrayBuilder();
        for (JsonObject constraint : constraints) {
            arrayBuilder.add(constraint);
        }
        return Json.createObjectBuilder()
                .add(constraintType, arrayBuilder)
                .build();
    }

    public static JsonObject rule(String actionType, JsonObject... constraints) {
        var arrayBuilder = Json.createArrayBuilder();
        for (JsonObject constraint : constraints) {
            arrayBuilder.add(constraint);
        }
        return Json.createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, actionType)
                .add(ODRL_CONSTRAINT_ATTRIBUTE, arrayBuilder)
                .build();
    }
    public static JsonObject ruleWithoutActionType(JsonObject... constraints) {
        var arrayBuilder = Json.createArrayBuilder();
        for (JsonObject constraint : constraints) {
            arrayBuilder.add(constraint);
        }
        return Json.createObjectBuilder()
                .add(ODRL_CONSTRAINT_ATTRIBUTE, arrayBuilder)
                .build();
    }

    public static JsonObject emptyRule() {
        return Json.createObjectBuilder()
                .build();
    }


    public static JsonObject policy(String policyType, String ruleType, JsonObject... constraints) {
        JsonObject rule = rule(policyType, constraints);

        return Json.createObjectBuilder()
                .add(ruleType, Json.createArrayBuilder().add(rule))
                .build();
    }

    public static JsonObject policy(String policyType, String ruleType) {
        JsonObject rule = rule(policyType);

        return Json.createObjectBuilder()
                .add(ruleType, Json.createArrayBuilder().add(rule))
                .build();
    }

    public static JsonObject policy(String ruleType, JsonObject rule) {
        return Json.createObjectBuilder()
                .add(TYPE, Json.createArrayBuilder().add(ODRL_POLICY_TYPE_SET))
                .add(ruleType, Json.createArrayBuilder().add(rule))
                .build();
    }

    public static JsonObject policyDefinition(JsonObject policy, String id) {
        return Json.createObjectBuilder()
                .add(ID, id)
                .add(EDC_NAMESPACE + "policy", Json.createArrayBuilder().add(policy))
                .build();
    }

}