/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.iam.ssi.spi.jsonld;

import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.JsonLdValueFunctions.extractStringValue;

class JsonLdValueFunctionsTest {
    private static final String VALUE = "@value";

    @Test
    void validate_nested_array() {
        var root = createArrayBuilder()
                .add(createObjectBuilder().add(VALUE, "test").build())
                .build();

        assertThat(extractStringValue(root)).isEqualTo("test");
    }

    @Test
    void validate_empty_array() {
        var root = createArrayBuilder().build();
        assertThat(extractStringValue(root)).isNull();
    }

    @Test
    void validate_object() {
        var root = createObjectBuilder().add(VALUE, "test").build();
        assertThat(extractStringValue(root)).isEqualTo("test");
    }

    @Test
    void validate_object_int() {
        var root = createObjectBuilder().add(VALUE, 1).build();
        assertThat(extractStringValue(root)).isEqualTo("1");
    }

    @Test
    void validate_object_double() {
        var root = createObjectBuilder().add(VALUE, 1.1d).build();
        assertThat(extractStringValue(root)).isEqualTo("1.1");
    }

    @Test
    void validate_null() {
        assertThat(extractStringValue(null)).isNull();
    }

}
