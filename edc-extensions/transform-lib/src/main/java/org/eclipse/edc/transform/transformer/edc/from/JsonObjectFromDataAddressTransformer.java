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

package org.eclipse.edc.transform.transformer.edc.from;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class JsonObjectFromDataAddressTransformer extends AbstractJsonLdTransformer<DataAddress, JsonObject> {

    private final JsonBuilderFactory jsonBuilderFactory;
    private final ObjectMapper mapper;

    public JsonObjectFromDataAddressTransformer(JsonBuilderFactory jsonBuilderFactory, TypeManager typeManager, String typeContext) {
        super(DataAddress.class, JsonObject.class);
        this.jsonBuilderFactory = jsonBuilderFactory;
        this.mapper = typeManager.getMapper(typeContext);
    }

    @Override
    public @Nullable JsonObject transform(@NotNull DataAddress dataAddress, @NotNull TransformerContext context) {
        var builder = jsonBuilderFactory.createObjectBuilder();

        builder.add(TYPE, EDC_NAMESPACE + "DataAddress");

        Function<Object, JsonValue> func = v -> {
            if (v instanceof DataAddress) {
                return context.transform(v, JsonObject.class);
            }
            return mapper.convertValue(v, JsonValue.class);
        };

        transformProperties(dataAddress.getProperties(), builder, func, context);

        return builder.build();
    }
}
