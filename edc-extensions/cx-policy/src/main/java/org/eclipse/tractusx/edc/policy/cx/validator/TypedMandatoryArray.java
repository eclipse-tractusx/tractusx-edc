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

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;
import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Validator for JSON arrays that enforces mandatory presence and optional minimum size constraints.
 * <p>
 * This validator can be configured to:
 * <ul>
 *   <li>Require a mandatory array at the specified path</li>
 *   <li>Allow the array to be absent (orAbsent mode)</li>
 *   <li>Enforce a minimum number of elements in the array</li>
 * </ul>
 * <p>
 * The validator fails if:
 * <ul>
 *   <li>The field is not an array type</li>
 *   <li>The array is missing when not in orAbsent mode</li>
 *   <li>The array size is below the specified minimum</li>
 * </ul>
 */
public class TypedMandatoryArray implements Validator<JsonObject> {
    private final JsonLdPath path;
    private final Integer min;
    private final boolean orAbsent;

    public TypedMandatoryArray(JsonLdPath path) {
        this(path, null, false);
    }

    public TypedMandatoryArray(JsonLdPath path, Integer min, boolean orAbsent) {
        this.path = path;
        this.min = min;
        this.orAbsent = orAbsent;
    }

    public static Function<JsonLdPath, Validator<JsonObject>> min(Integer min) {
        return path -> new TypedMandatoryArray(path, min, false);
    }

    public static Function<JsonLdPath, Validator<JsonObject>> orAbsent() {
        return path -> new TypedMandatoryArray(path, null, true);
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        return Optional.ofNullable(input.get(path.last()))
                .map(value -> {
                    if (value.getValueType() != JsonValue.ValueType.ARRAY) {
                        return ValidationResult.failure(violation(format("Expected array for '%s' but found %s",
                                path, value.getValueType().toString().toLowerCase()), path.toString()));
                    }
                    return validateMin(value.asJsonArray());
                })
                .orElseGet(
                        () -> {
                            if (orAbsent) {
                                return ValidationResult.success();
                            }
                            return ValidationResult.failure(violation(format("Mandatory array '%s' is missing", path), path.toString()));
                        }
                );
    }

    private ValidationResult validateMin(JsonArray array) {
        if (min == null || (array.size() >= min)) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(violation(format("Array '%s' should at least contains '%s' elements", path, min), path.toString()));
    }
}
