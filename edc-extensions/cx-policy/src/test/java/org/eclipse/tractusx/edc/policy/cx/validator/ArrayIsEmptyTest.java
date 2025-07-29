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
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArrayIsEmptyTest {

    private final String arrayAttribute = "testArray";
    private final JsonLdPath path = JsonLdPath.path(arrayAttribute);

    private ArrayIsEmpty validator;

    @BeforeEach
    void setUp() {
        validator = new ArrayIsEmpty(path);
    }

    @Test
    void shouldReturnSuccess_whenArrayAttributeMissing() {
        JsonObject input = Json.createObjectBuilder()
                .add("other", "value")
                .build();

        ValidationResult result = validator.validate(input);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnSuccess_whenArrayIsEmpty() {
        JsonArray emptyArray = Json.createArrayBuilder().build();
        JsonObject input = Json.createObjectBuilder()
                .add(arrayAttribute, emptyArray)
                .build();

        ValidationResult result = validator.validate(input);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldReturnFailure_whenArrayIsNotEmpty() {
        JsonArray array = Json.createArrayBuilder()
                .add("item")
                .build();
        JsonObject input = Json.createObjectBuilder()
                .add(arrayAttribute, array)
                .build();

        ValidationResult result = validator.validate(input);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailure().getMessages()).anyMatch(v -> v.contains("Array must be empty"));
    }

    @Test
    void shouldReturnFailure_whenAttributeIsNotArray() {
        JsonObject input = Json.createObjectBuilder()
                .add(arrayAttribute, "notAnArray")
                .build();

        ValidationResult result = validator.validate(input);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailure().getMessages()).anyMatch(v -> v.contains("Must be an empty array"));
    }

    @Test
    void shouldReturnSuccess_whenAttributeIsNull() {
        JsonObject input = Json.createObjectBuilder()
                .addNull(arrayAttribute)
                .build();

        ValidationResult result = validator.validate(input);

        assertThat(result.succeeded()).isTrue();
    }
}