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

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.junit.assertions.FailureAssert;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;


class TypedMandatoryArrayTest {
    private static final String ARRAY_ATTRIBUTE = "arrayAttribute";
    private final JsonLdPath path = JsonLdPath.path(ARRAY_ATTRIBUTE);

    @Test
    void shouldReturnSuccess_whenArrayPresent() {
        JsonArray arr = Json.createArrayBuilder().add(1).add(2).build();
        JsonObject obj = Json.createObjectBuilder().add(ARRAY_ATTRIBUTE, arr).build();

        TypedMandatoryArray validator = new TypedMandatoryArray(path);
        ValidationResult result = validator.validate(obj);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldReturnFailure_whenArrayMissing() {
        JsonObject obj = Json.createObjectBuilder().build();

        TypedMandatoryArray validator = new TypedMandatoryArray(path);
        ValidationResult result = validator.validate(obj);

        assertThat(result).isFailed();
        FailureAssert.assertThat(result.getFailure()).messages().anyMatch(msg -> msg.contains("Mandatory array '%s' is missing".formatted(path)));
    }

    @Test
    void shouldReturnFailure_whenNotArray() {
        JsonObject obj = Json.createObjectBuilder().add(ARRAY_ATTRIBUTE, "notAnArray").build();

        TypedMandatoryArray validator = new TypedMandatoryArray(path);
        ValidationResult result = validator.validate(obj);

        assertThat(result).isFailed();
        FailureAssert.assertThat(result.getFailure()).messages().anyMatch(msg -> msg.contains("Expected array for '%s'".formatted(path)));
    }

    @Test
    void shouldReturnSuccess_whenArrayMeetsMin() {
        JsonArray arr = Json.createArrayBuilder().add(1).add(2).build();
        JsonObject obj = Json.createObjectBuilder().add(ARRAY_ATTRIBUTE, arr).build();

        TypedMandatoryArray validator = new TypedMandatoryArray(path, 2, false);

        ValidationResult result = validator.validate(obj);
        assertThat(result).isSucceeded();
    }

    @Test
    void shouldReturnFailure_whenArrayBelowMin() {
        JsonArray arr = Json.createArrayBuilder().add(1).build();
        JsonObject obj = Json.createObjectBuilder().add(ARRAY_ATTRIBUTE, arr).build();

        TypedMandatoryArray validator = new TypedMandatoryArray(path, 2, false);
        ValidationResult result = validator.validate(obj);

        assertThat(result).isFailed();
        FailureAssert.assertThat(result.getFailure()).messages().anyMatch(msg -> msg.contains("Array '%s' should at least contains '%s' elements".formatted(path, 2)));
    }

    /**
     * Tests that validation succeeds when the validator is configured with orAbsent
     * and the array field is missing from the JSON object.
     */
    @Test
    void shouldReturnSuccess_whenConfiguredWithOrAbsentAndArrayMissing() {
        JsonObject obj = Json.createObjectBuilder().build();

        TypedMandatoryArray validator = new TypedMandatoryArray(path, null, true);
        ValidationResult result = validator.validate(obj);

        assertThat(result).isSucceeded();
    }
}
