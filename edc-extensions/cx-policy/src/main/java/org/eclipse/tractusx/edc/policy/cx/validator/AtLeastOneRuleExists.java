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

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OBLIGATION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PROHIBITION_ATTRIBUTE;
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
        if (!input.containsKey(attribute) ||
                input.get(attribute).getValueType() != JsonValue.ValueType.ARRAY) {
            return false;
        }

        JsonArray array = input.getJsonArray(attribute);
        if (array.isEmpty()) {
            return false;
        }
        return array.stream()
                .filter(v -> v.getValueType() == JsonValue.ValueType.OBJECT)
                .map(v -> (JsonObject) v)
                .anyMatch(obj -> obj.containsKey(ODRL_CONSTRAINT_ATTRIBUTE));
    }
}
