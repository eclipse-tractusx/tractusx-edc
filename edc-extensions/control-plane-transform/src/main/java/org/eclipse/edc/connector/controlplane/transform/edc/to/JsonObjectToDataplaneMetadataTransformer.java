/********************************************************************************
 * Copyright (c) 2025 Think-it GmbH
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

package org.eclipse.edc.connector.controlplane.transform.edc.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsonObjectToDataplaneMetadataTransformer extends AbstractJsonLdTransformer<JsonObject, DataplaneMetadata> {

    public JsonObjectToDataplaneMetadataTransformer() {
        super(JsonObject.class, DataplaneMetadata.class);
    }

    @Override
    public @Nullable DataplaneMetadata transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = DataplaneMetadata.Builder.newInstance();

        var labels = jsonObject.getJsonArray(DataplaneMetadata.EDC_DATAPLANE_METADATA_LABELS);
        if (labels != null) {
            transformArray(labels, Object.class, context).forEach(label -> builder.label(label.toString()));
        }

        var properties = jsonObject.get(DataplaneMetadata.EDC_DATAPLANE_METADATA_PROPERTIES);
        if (properties != null) {
            var jsonValue = nodeJsonValue(properties);
            if (jsonValue instanceof JsonObject json) {
                visitProperties(json, (key, value) -> builder.property(key, transformGenericProperty(value, context)));
            } else {
                context.reportProblem("Expected properties to be a JsonObject");
                return null;

            }
        }

        return builder.build();
    }
}
