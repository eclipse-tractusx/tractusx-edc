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
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ACTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_REMEDY_ATTRIBUTE;

/**
 * Converts from an ODRL prohibition as a {@link JsonObject} in JSON-LD expanded form to a {@link Prohibition}.
 */
public class JsonObjectToProhibitionTransformer extends AbstractJsonLdTransformer<JsonObject, Prohibition> {

    public JsonObjectToProhibitionTransformer() {
        super(JsonObject.class, Prohibition.class);
    }

    @Override
    public @Nullable Prohibition transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = Prohibition.Builder.newInstance();

        visitProperties(object, key -> switch (key) {
            case ODRL_ACTION_ATTRIBUTE -> value -> builder.action(transformObject(value, Action.class, context));
            case ODRL_CONSTRAINT_ATTRIBUTE -> value -> builder.constraints(transformArray(value, Constraint.class, context));
            case ODRL_REMEDY_ATTRIBUTE -> value -> builder.remedies(transformArray(value, Duty.class, context));
            default -> doNothing();
        });

        return builderResult(builder::build, context);
    }

}
