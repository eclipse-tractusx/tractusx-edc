/********************************************************************************
 * Copyright (c) 2025 Cofinity-X
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

package org.eclipse.edc.transform.transformer.edc.to;

import jakarta.json.JsonValue;
import org.eclipse.edc.json.JacksonTypeManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JsonValueToGenericTypeTransformerTest {

    private final JsonValueToGenericTypeTransformer transformer = new JsonValueToGenericTypeTransformer(new JacksonTypeManager(), "any");

    @Test
    void shouldConvertBooleanTrue() {
        var transform = transformer.transform(JsonValue.TRUE, mock());

        assertThat(transform).isInstanceOf(Boolean.class).isEqualTo(Boolean.TRUE);
    }

    @Test
    void shouldConvertBooleanFalse() {
        var transform = transformer.transform(JsonValue.FALSE, mock());

        assertThat(transform).isInstanceOf(Boolean.class).isEqualTo(Boolean.FALSE);
    }
}
