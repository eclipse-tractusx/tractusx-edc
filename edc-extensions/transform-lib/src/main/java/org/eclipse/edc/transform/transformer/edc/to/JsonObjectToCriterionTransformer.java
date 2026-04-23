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

package org.eclipse.edc.transform.transformer.edc.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERAND_LEFT;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERAND_RIGHT;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERATOR;

public class JsonObjectToCriterionTransformer extends AbstractJsonLdTransformer<JsonObject, Criterion> {

    public JsonObjectToCriterionTransformer() {
        super(JsonObject.class, Criterion.class);
    }

    @Override
    public @Nullable Criterion transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = Criterion.Builder.newInstance();

        builder.operandLeft(transformString(object.get(CRITERION_OPERAND_LEFT), context));

        var operator = transformString(object.get(CRITERION_OPERATOR), context);
        builder.operator(operator);

        var operandRight = object.get(CRITERION_OPERAND_RIGHT);
        if ("in".equals(operator)) {
            builder.operandRight(operandRight.asJsonArray().stream().map(this::nodeValue).collect(toList()));
        } else {
            builder.operandRight(transformGenericProperty(operandRight, context));
        }

        return builder.build();
    }

}
