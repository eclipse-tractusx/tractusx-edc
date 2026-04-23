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

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;

public class JsonObjectFromCriterionTransformer extends AbstractJsonLdTransformer<Criterion, JsonObject> {
    private final JsonBuilderFactory jsonFactory;
    private final TypeManager typeManager;
    private final String typeContext;

    public JsonObjectFromCriterionTransformer(JsonBuilderFactory jsonFactory, TypeManager typeManager, String typeContext) {
        super(Criterion.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
        this.typeManager = typeManager;
        this.typeContext = typeContext;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull Criterion querySpec, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder();
        builder.add(TYPE, Criterion.CRITERION_TYPE);

        addValue(builder, Criterion.CRITERION_OPERAND_LEFT, querySpec.getOperandLeft(), context);

        builder.add(Criterion.CRITERION_OPERATOR, querySpec.getOperator());

        if (querySpec.getOperandRight() != null) {
            addValue(builder, Criterion.CRITERION_OPERAND_RIGHT, querySpec.getOperandRight(), context);
        }

        return builder.build();
    }

    private void addValue(JsonObjectBuilder builder, String field, Object value, TransformerContext context) {
        try {
            builder.add(field, typeManager.getMapper(typeContext).convertValue(value, JsonValue.class));
        } catch (IllegalArgumentException e) {
            context.problem()
                    .invalidProperty()
                    .type(VALUE)
                    .property(field)
                    .value(value != null ? value.toString() : "null")
                    .error(e.getMessage())
                    .report();
        }
    }
}
