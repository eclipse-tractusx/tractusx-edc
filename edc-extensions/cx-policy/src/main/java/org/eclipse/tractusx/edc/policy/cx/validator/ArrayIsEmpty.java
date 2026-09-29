/********************************************************************************
 * Copyright (c) 2025 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
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

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.validator.spi.Violation.violation;

public class ArrayIsEmpty implements Validator<JsonObject> {
    private final JsonLdPath path;

    public ArrayIsEmpty(JsonLdPath path) {
        this.path = path;
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        if (!input.containsKey(path.last())) {
            return ValidationResult.success();
        }
        var value = input.get(path.last());

        if (value.getValueType() == JsonValue.ValueType.NULL) {
            return ValidationResult.success();
        }

        if (value.getValueType() != JsonValue.ValueType.ARRAY) {
            return ValidationResult.failure(
                    violation("Must be an empty array", path.toString())
            );
        }

        if (!value.asJsonArray().isEmpty()) {
            return ValidationResult.failure(
                    violation("Array must be empty", path.toString())
            );
        }
        return ValidationResult.success();
    }
}
