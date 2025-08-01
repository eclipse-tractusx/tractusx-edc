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
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ACTION_ATTRIBUTE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class ActionTypeIsTest {

    private static final String ACTION_ATTR = ODRL_ACTION_ATTRIBUTE;
    private static final String EXPECTED_ACTION = "odrl:use";
    private final JsonLdPath path = JsonLdPath.path();

    private ActionTypeIs validator;

    @BeforeEach
    void setUp() {
        validator = new ActionTypeIs(path, EXPECTED_ACTION);
    }

    @Test
    void shouldReturnSuccess_whenActionIsStringAndMatches() {
        JsonObject input = Json.createObjectBuilder()
                .add(ACTION_ATTR, EXPECTED_ACTION)
                .build();

        ValidationResult result = validator.validate(input);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldReturnSuccess_whenActionIsStringAndInUpperCaseAndMatches() {
        JsonObject input = Json.createObjectBuilder()
                .add(ACTION_ATTR, "odrl:USE")
                .build();

        ValidationResult result = validator.validate(input);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldReturnFailure_whenActionIsStringAndDoesNotMatch() {
        JsonObject input = Json.createObjectBuilder()
                .add(ACTION_ATTR, "READ")
                .build();

        ValidationResult result = validator.validate(input);

        assertThat(result).isFailed();
    }

    @Test
    void shouldReturnSuccess_whenActionIsObjectAndMatches() {
        JsonObject actionObj = Json.createObjectBuilder()
                .add("@id", EXPECTED_ACTION)
                .build();
        JsonObject input = Json.createObjectBuilder()
                .add(ACTION_ATTR, actionObj)
                .build();

        ValidationResult result = validator.validate(input);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldReturnFailure_whenActionIsObjectAndDoesNotMatch() {
        JsonObject actionObj = Json.createObjectBuilder()
                .add("@id", "read")
                .build();
        JsonObject input = Json.createObjectBuilder()
                .add(ACTION_ATTR, actionObj)
                .build();

        ValidationResult result = validator.validate(input);

        assertThat(result).isFailed();
    }

    @Test
    void shouldReturnSuccess_whenActionIsArrayAndFirstObjectMatches() {
        JsonObject actionObj = Json.createObjectBuilder()
                .add("@id", EXPECTED_ACTION)
                .build();
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder().add(actionObj);
        JsonObject input = Json.createObjectBuilder()
                .add(ACTION_ATTR, arrayBuilder)
                .build();

        ValidationResult result = validator.validate(input);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldReturnFailure_whenActionIsArrayAndFirstObjectDoesNotMatch() {
        JsonObject actionObj = Json.createObjectBuilder()
                .add("@id", "READ")
                .build();
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder().add(actionObj);
        JsonObject input = Json.createObjectBuilder()
                .add(ACTION_ATTR, arrayBuilder)
                .build();

        ValidationResult result = validator.validate(input);

        assertThat(result).isFailed();
    }

    @Test
    void shouldReturnFailure_whenActionIsMissing() {
        JsonObject input = Json.createObjectBuilder().build();

        ValidationResult result = validator.validate(input);

        assertThat(result).isFailed();
    }
}