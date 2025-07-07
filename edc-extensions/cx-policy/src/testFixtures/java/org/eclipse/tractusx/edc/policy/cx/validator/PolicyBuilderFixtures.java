package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.util.Collection;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.*;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.*;

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
}