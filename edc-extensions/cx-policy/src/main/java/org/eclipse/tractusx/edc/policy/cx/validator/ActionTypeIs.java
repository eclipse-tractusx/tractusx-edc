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
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ACTION_ATTRIBUTE;
import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Validator that ensures an action property matches a specific expected value.
 * The validator can be configured to either require the action property to be present
 * or allow it to be absent. When absent actions are allowed, validation succeeds
 * if the property is missing.
 */
public class ActionTypeIs implements Validator<JsonObject> {
    private final JsonLdPath path;
    private final String expectedAction;
    private final boolean allowAbsent;

    private ActionTypeIs(JsonLdPath path, String expectedAction, boolean allowAbsent) {
        this.path = path;
        this.expectedAction = expectedAction;
        this.allowAbsent = allowAbsent;
    }

    public ActionTypeIs(JsonLdPath path, String expectedAction) {
        this(path, expectedAction, false);
    }

    public static ActionTypeIs orAbsent(JsonLdPath path, String expectedType) {
        return new ActionTypeIs(path, expectedType, true);
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        if (!input.containsKey(ODRL_ACTION_ATTRIBUTE)) {
            if (allowAbsent) {
                return ValidationResult.success();
            }
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
