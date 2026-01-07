/*
 * Copyright (c) 2026 Cofinity-X GmbH
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
 */
package org.eclipse.tractusx.edc.discovery.v4alpha.transformers;

import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryRequest.CONNECTOR_DISCOVERY_REQUEST_IDENTIFIER_ATTRIBUTE;
import static org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryRequest.CONNECTOR_DISCOVERY_REQUEST_KNOWNS_ATTRIBUTE;

public class JsonObjectToConnectorDiscoveryRequest extends AbstractJsonLdTransformer<JsonObject, ConnectorDiscoveryRequest> {
    public JsonObjectToConnectorDiscoveryRequest() {
        super(JsonObject.class, ConnectorDiscoveryRequest.class);
    }

    @Override
    public @Nullable ConnectorDiscoveryRequest transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext transformerContext) {
        var identifier = transformString(jsonObject.get(CONNECTOR_DISCOVERY_REQUEST_IDENTIFIER_ATTRIBUTE), transformerContext);
        var knowns = Optional.ofNullable(jsonObject.get(CONNECTOR_DISCOVERY_REQUEST_KNOWNS_ATTRIBUTE))
                .map(JsonValue::asJsonArray)
                .map(knownsArray -> knownsArray.stream()
                        .map(value -> ((JsonString)value).getString())
                        .collect(toList()))
                .filter(list -> !list.isEmpty())
                .orElse(null);

        if (identifier == null) {
            transformerContext.reportProblem("Missing required attribute in ConnectorDiscoveryRequest: tx:identifier");
            return null;
        }

        return new ConnectorDiscoveryRequest(identifier, knowns);
    }
}
