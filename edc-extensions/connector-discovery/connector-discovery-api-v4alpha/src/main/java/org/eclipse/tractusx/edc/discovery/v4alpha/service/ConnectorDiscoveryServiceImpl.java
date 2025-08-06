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
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest;
import org.eclipse.edc.connector.controlplane.protocolversion.spi.ProtocolVersionRequest;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionService;
import org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol;
import org.eclipse.edc.protocol.dsp.spi.type.Dsp08Constants;
import org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.eclipse.edc.protocol.spi.ProtocolVersions;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.spi.exception.BadGatewayException;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryService;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class ConnectorDiscoveryServiceImpl implements ConnectorDiscoveryService {

    private final BdrsClient bdrsClient;
    private final VersionService versionService;
    private final ObjectMapper mapper;

    public ConnectorDiscoveryServiceImpl(BdrsClient bdrsClient, VersionService versionService, ObjectMapper mapper) {
        this.bdrsClient = bdrsClient;
        this.versionService = versionService;
        this.mapper = mapper;

    }

    public ServiceResult<JsonObject> discoverVersionParams(ConnectorParamsDiscoveryRequest request) {

        var discoveredParameters = Json.createObjectBuilder();

        try {
            var result = requestVersions(request);

            if (result.failed()) {
                throw new BadGatewayException("Counter party well-known endpoint has failed: " + result.getFailureMessages());
            } else {
                var protocolVersions = mapper.readValue(result.getContent(), ProtocolVersions.class);
                var did = bdrsClient.resolve(request.bpnl());
                var version20251 = findProtocolVersion(Dsp2025Constants.V_2025_1_VERSION, protocolVersions);
                if (version20251 != null && did != null) {
                    addDiscoveredParameters(Dsp2025Constants.V_2025_1_VERSION, did, request.counterPartyAddress() + version20251.path(), discoveredParameters);
                    return ServiceResult.success(discoveredParameters.build());
                } else {
                    var version08 = findProtocolVersion(Dsp08Constants.V_08_VERSION, protocolVersions);
                    if (version08 != null) {
                        addDiscoveredParameters(Dsp08Constants.V_08_VERSION, request.bpnl(), request.counterPartyAddress() + version08.path(), discoveredParameters);
                        return ServiceResult.success(discoveredParameters.build());
                    }
                }
            }

        } catch (TimeoutException e) {
            throw new BadGatewayException("Timeout while waiting for the counter party to respond.");
        } catch (BadGatewayException e) {
            throw new BadGatewayException(e.getMessage());
        } catch (Exception e) {
            return ServiceResult.unexpected("Error while discovering dsp parameters: " + e.getMessage());
        }

        return ServiceResult.unexpected("No valid protocol version found for the counter party. " +
                "The provided BPNL couldn't be resolved to a DID or the counter party does " +
                "not support any of the expected protocol versions (" + Dsp08Constants.V_08_VERSION + ", " + Dsp2025Constants.V_2025_1_VERSION + ")");
    }

    private StatusResult<byte[]> requestVersions(ConnectorParamsDiscoveryRequest request) throws Exception {

        var protocolVersionRequest = ProtocolVersionRequest.Builder.newInstance()
                .protocol(HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP)
                .counterPartyId(request.bpnl())
                .counterPartyAddress(request.counterPartyAddress())
                .build();

        return versionService.requestVersions(protocolVersionRequest).get(10, TimeUnit.SECONDS);
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
