/********************************************************************************
 * Copyright (c) 2025 Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.util.Collection;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.*;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.*;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public final class PolicyBuilderFixtures {

    private PolicyBuilderFixtures() {
    }

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

    public static JsonObject policy(String ruleType, JsonObject... rules) {
        var rulesArrayBuilder = Json.createArrayBuilder();
        for (JsonObject rule : rules) {
            rulesArrayBuilder.add(rule);
        }
        return Json.createObjectBuilder()
                .add(TYPE, Json.createArrayBuilder().add(ODRL_POLICY_TYPE_SET))
                .add(ruleType, rulesArrayBuilder)
                .build();
    }

    public static JsonObject policyDefinition(JsonObject policy, String id) {
        return Json.createObjectBuilder()
                .add(ID, id)
                .add(EDC_NAMESPACE + "policy", Json.createArrayBuilder().add(policy))
                .build();
    }

}