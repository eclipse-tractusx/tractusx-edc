/********************************************************************************
 * Copyright (c) 2026 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
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
import java.util.UUID;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public final class PolicyBuilderFixturesV4 {

    private static final String LEFT_OPERAND = "leftOperand";
    private static final String OPERATOR = "operator";
    private static final String RIGHT_OPERAND = "rightOperand";
    private static final String ACTION = "action";
    private static final String CONSTRAINT = "constraint";

    private PolicyBuilderFixturesV4() {
    }

    public static JsonObject atomicConstraint(String leftOperand, String operator, Object rightOperand) {
        var builder = Json.createObjectBuilder()
                .add(LEFT_OPERAND, leftOperand)
                .add(OPERATOR, operator);

        if (rightOperand instanceof Collection<?> collection) {
            var array = Json.createArrayBuilder();
            collection.forEach(item -> array.add(item.toString()));
            builder.add(RIGHT_OPERAND, array);
        } else {
            builder.add(RIGHT_OPERAND, rightOperand.toString());
        }
        return builder.build();
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
                .add(ACTION, actionType)
                .add(CONSTRAINT, arrayBuilder)
                .build();
    }

    public static JsonObject ruleWithoutActionType(JsonObject... constraints) {
        var arrayBuilder = Json.createArrayBuilder();
        for (JsonObject constraint : constraints) {
            arrayBuilder.add(constraint);
        }
        return Json.createObjectBuilder()
                .add(CONSTRAINT, arrayBuilder)
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
                .add(ID, UUID.randomUUID().toString())
                .add(TYPE, Json.createArrayBuilder().add("Set"))
                .add(ruleType, rulesArrayBuilder)
                .build();
    }
}