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

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ACTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OBLIGATION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PROHIBITION_ATTRIBUTE;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACTION_ACCESS;

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

        return ACTION_ACCESS;
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
