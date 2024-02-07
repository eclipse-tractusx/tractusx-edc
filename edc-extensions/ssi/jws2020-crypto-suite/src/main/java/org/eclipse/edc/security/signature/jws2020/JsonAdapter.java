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

package org.eclipse.edc.security.signature.jws2020;

import com.apicatalog.ld.schema.adapter.LdValueAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;

import java.util.Map;

class JsonAdapter implements LdValueAdapter<JsonValue, Object> {
    private final ObjectMapper mapper;

    JsonAdapter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Object read(JsonValue value) {
        var input = value;
        if (value instanceof JsonObject) {
            var jo = value.asJsonObject();
            input = jo.get(JsonLdKeywords.VALUE);
        }
        return mapper.convertValue(input, Object.class);
    }

    @Override
    public JsonValue write(Object value) {
        if (value instanceof Map) {
            var jo = Json.createObjectBuilder();
            jo.add(JsonLdKeywords.VALUE, Json.createObjectBuilder((Map) value));
            jo.add(JsonLdKeywords.TYPE, JsonLdKeywords.JSON);
            return mapper.convertValue(jo.build(), JsonValue.class);
        }
        return mapper.convertValue(value, JsonValue.class);
    }

}
