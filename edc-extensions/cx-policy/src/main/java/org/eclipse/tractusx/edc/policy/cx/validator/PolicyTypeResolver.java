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

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ACTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OBLIGATION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PROHIBITION_ATTRIBUTE;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACTION_ACCESS;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACTION_USAGE;

/**
 * Resolves the type of policy based on the action type of its rules.
 * It checks for a non-empty permission, obligations, or prohibitions and determines the
 * policy type from the first found action.
 * If all of these are empty, it defaults to the access policy type.
 */
public class PolicyTypeResolver {
    private static final String EMPTY_ACTION = "";

    public static String resolve(JsonObject policy) {
        Set<String> actionSet = Stream.of(ODRL_PERMISSION_ATTRIBUTE, ODRL_OBLIGATION_ATTRIBUTE, ODRL_PROHIBITION_ATTRIBUTE)
                .map(attribute -> getRuleArray(policy, attribute))
                .filter(rules -> !rules.isEmpty())
                .flatMap(rules -> rules.stream())
                .filter(rule -> rule.getValueType() == JsonValue.ValueType.OBJECT)
                .map(rule -> getActionFromRule(rule.asJsonObject()))
                .peek(action -> {
                    if (action.equals(EMPTY_ACTION)) {
                        throw new IllegalArgumentException("Rule does not contain any action field. Expected one of: " + ODRL_ACTION_ATTRIBUTE);
                    }
                    if (!isActionValid(action)) {
                        throw new IllegalArgumentException("Rule does not contain a valid policy type. Expected one of: " + ACTION_ACCESS + ", " + ACTION_USAGE);
                    }
                })
                .collect(Collectors.toSet());

        if (actionSet.isEmpty()) {
            // actionSet is empty when all rules are empty. Default to access policy type
            return ACTION_ACCESS;
        }

        if (actionSet.size() > 1) {
            throw new IllegalArgumentException("Policy contains inconsistent policy types: " + actionSet + ". Expected only one of: " + ACTION_ACCESS + ", " + ACTION_USAGE);
        }

        return actionSet.iterator().next();
    }

    private static boolean isActionValid(String action) {
        return action.equals(ACTION_ACCESS) || action.equals(ACTION_USAGE);
    }

    private static JsonArray getRuleArray(JsonObject policy, String attribute) {
        if (policy.containsKey(attribute)) {
            return policy.get(attribute).asJsonArray();
        }
        return JsonArray.EMPTY_JSON_ARRAY;
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
        return EMPTY_ACTION;
    }
}
