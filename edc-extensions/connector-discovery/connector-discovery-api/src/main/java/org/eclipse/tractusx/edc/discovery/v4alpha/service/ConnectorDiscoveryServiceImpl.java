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

package org.eclipse.tractusx.edc.discovery.v4alpha.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import okhttp3.Request;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol;
import org.eclipse.edc.protocol.dsp.spi.type.Dsp08Constants;
import org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.eclipse.edc.protocol.spi.ProtocolVersions;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryRequest;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryService;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;

import java.io.IOException;


public class ConnectorDiscoveryServiceImpl implements ConnectorDiscoveryService {

    private static final String DSP_DISCOVERY_PATH = "/.well-known/dspace-version";

    private final BdrsClient bdrsClient;
    private final EdcHttpClient httpClient;
    private final ObjectMapper mapper;

    public ConnectorDiscoveryServiceImpl(BdrsClient bdrsClient, EdcHttpClient httpClient, ObjectMapper mapper) {
        this.bdrsClient = bdrsClient;
        this.httpClient = httpClient;
        this.mapper = mapper;

    }

    @Override
    public ServiceResult<JsonObject> discoverVersionParams(ConnectorParamsDiscoveryRequest request) {

        var discoveredParameters = Json.createObjectBuilder();

        var wellKnownRequest = new Request.Builder()
                .url(request.counterPartyAddress() + DSP_DISCOVERY_PATH)
                .get()
                .build();

        try (var response = httpClient.execute(wellKnownRequest)) {

            if (!response.isSuccessful()) {
                var failureMessage = response.message();
                return ServiceResult.unexpected("Counter party well-known endpoint has failed with status %s and message: %s".formatted(response.code(), failureMessage));
            } else {
                var bytesBody = response.body().bytes();

                var protocolVersions = mapper.readValue(bytesBody, ProtocolVersions.class);
                var did = bdrsClient.resolveDid(request.bpnl());
                var version20251 = findProtocolVersion(Dsp2025Constants.V_2025_1_VERSION, protocolVersions);
                if (version20251 != null && did != null) {
                    addDiscoveredParameters(Dsp2025Constants.V_2025_1_VERSION, did,
                            request.counterPartyAddress() + removeTrailingSlash(version20251.path()),
                            discoveredParameters);

                    return ServiceResult.success(discoveredParameters.build());
                } else {
                    var version08 = findProtocolVersion(Dsp08Constants.V_08_VERSION, protocolVersions);
                    if (version08 != null) {
                        addDiscoveredParameters(Dsp08Constants.V_08_VERSION, request.bpnl(),
                                request.counterPartyAddress() + removeTrailingSlash(version08.path()),
                                discoveredParameters);

                        return ServiceResult.success(discoveredParameters.build());
                    }
                }
            }

        } catch (IOException e) {
            return ServiceResult.unexpected("An exception with the following message occurred while executing dsp version request: %s".formatted(e.getMessage()));
        }

        return ServiceResult.unexpected("No valid protocol version found for the counter party. " +
                "The provided BPNL couldn't be resolved to a DID or the counter party does " +
                "not support any of the expected protocol versions (" + Dsp08Constants.V_08_VERSION + ", " + Dsp2025Constants.V_2025_1_VERSION + ")");
    }

    @Override
    public ServiceResult<JsonArray> discoverConnectors(ConnectorDiscoveryRequest request) {
        return ServiceResult.unexpected("Call not expected");
    }

    private String removeTrailingSlash(String path) {
        if (path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    private ProtocolVersion findProtocolVersion(String targetVersion, ProtocolVersions protocolVersions) {
        return protocolVersions.protocolVersions().stream()
                .filter(v -> targetVersion.equals(v.version()))
                .filter(v -> v.path() != null)
                .findFirst()
                .orElse(null);
    }

    private void addDiscoveredParameters(String version, String counterPartyId, String address, JsonObjectBuilder versionParameters) {
        var protocol = Dsp2025Constants.V_2025_1_VERSION.equals(version) ? Dsp2025Constants.DATASPACE_PROTOCOL_HTTP_V_2025_1 : HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
        versionParameters.add(CatalogRequest.CATALOG_REQUEST_PROTOCOL, protocol);
        versionParameters.add(CatalogRequest.CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, address);
        versionParameters.add(CatalogRequest.CATALOG_REQUEST_COUNTER_PARTY_ID, counterPartyId);
    }
}
