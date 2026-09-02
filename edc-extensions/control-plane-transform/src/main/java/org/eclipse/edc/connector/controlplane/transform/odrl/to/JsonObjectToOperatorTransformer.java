/********************************************************************************
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
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

package org.eclipse.edc.connector.controlplane.transform.odrl.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OPERATOR_TYPE;

/**
 * Converts from an ODRL operator as a {@link JsonObject} in JSON-LD expanded form to an {@link Operator}.
 */
public class JsonObjectToOperatorTransformer extends AbstractJsonLdTransformer<JsonObject, Operator> {

    public JsonObjectToOperatorTransformer() {
        super(JsonObject.class, Operator.class);
    }

    @Override
    public @Nullable Operator transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var value = nodeId(object);

        if (value == null) {
            context.problem().missingProperty().property(ID).type(ODRL_OPERATOR_TYPE).report();
            return null;
        }

        return Arrays.stream(Operator.values())
                .filter(it -> it.getOdrlRepresentation().equals(value))
                .findFirst()
                .orElseGet(() -> {
                    context.problem().invalidProperty().property(ID).type(ODRL_OPERATOR_TYPE).value(value).report();
                    return null;
                });

    }

}
