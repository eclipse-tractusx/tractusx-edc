/*
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest.DISCOVERY_PARAMS_REQUEST_BPNL_ATTRIBUTE;
import static org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest.DISCOVERY_PARAMS_REQUEST_COUNTER_PARTY_ADDRESS_ATTRIBUTE;

public class JsonObjectToConnectorDiscoveryRequest extends AbstractJsonLdTransformer<JsonObject, ConnectorParamsDiscoveryRequest> {


    public JsonObjectToConnectorDiscoveryRequest() {
        super(JsonObject.class, ConnectorParamsDiscoveryRequest.class);

    }

    @Override
    public @Nullable ConnectorParamsDiscoveryRequest transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext transformerContext) {

        var bpnl = transformString(jsonObject.get(DISCOVERY_PARAMS_REQUEST_BPNL_ATTRIBUTE), transformerContext);
        var counterPartyAddress = transformString(jsonObject.get(DISCOVERY_PARAMS_REQUEST_COUNTER_PARTY_ADDRESS_ATTRIBUTE), transformerContext);

        if (bpnl == null || counterPartyAddress == null) {
            transformerContext.reportProblem("Missing required attributes in ConnectorParamsDiscoveryRequest: tx:bpnl or edc:counterPartyAddress");
            return null;
        }

        return new ConnectorParamsDiscoveryRequest(bpnl, counterPartyAddress);

    }
}

