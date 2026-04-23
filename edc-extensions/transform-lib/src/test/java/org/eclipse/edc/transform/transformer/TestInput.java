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

package org.eclipse.edc.transform.transformer;

import com.apicatalog.jsonld.document.JsonDocument;
import jakarta.json.JsonObject;

import static com.apicatalog.jsonld.JsonLd.expand;

/**
 * Functions for shaping test input.
 */
public class TestInput {

    /**
     * Expands test input as Json-ld is required to be in this form
     */
    public static JsonObject getExpanded(JsonObject message) {
        try {
            return expand(JsonDocument.of(message)).get().asJsonArray().getJsonObject(0);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private TestInput() {
    }
}
