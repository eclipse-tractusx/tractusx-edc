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
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol;
import org.eclipse.edc.protocol.dsp.spi.type.Dsp08Constants;
import org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.eclipse.edc.protocol.spi.ProtocolVersions;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.web.spi.exception.BadGatewayException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryRequest;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryService;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.DspVersionToIdentifierMapper;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.IdentifierToDidMapper;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static jakarta.json.Json.createArrayBuilder;
import static java.lang.String.format;
import static java.util.Collections.emptyList;


public class ConnectorDiscoveryServiceImpl implements ConnectorDiscoveryService {

    private static final String DSP_DISCOVERY_PATH = "/.well-known/dspace-version";
    private static final String DATA_SERVICE = "DataService";

    private final DidResolverRegistry didResolver;
    private final EdcHttpClient httpClient;
    private final ObjectMapper mapper;
    private final IdentifierToDidMapper identifierMapper;
    private final DspVersionToIdentifierMapper dspVersionMapper;
    private final Monitor monitor;

    public ConnectorDiscoveryServiceImpl(DidResolverRegistry didResolver,
                                         EdcHttpClient httpClient, ObjectMapper mapper,
                                         IdentifierToDidMapper identifierMapper, DspVersionToIdentifierMapper dspVersionMapper,
                                         Monitor monitor) {
        this.didResolver = didResolver;
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.identifierMapper = identifierMapper;
        this.dspVersionMapper = dspVersionMapper;
        this.monitor = monitor;
    }

    @Override
    public CompletableFuture<JsonObject> discoverVersionParams(ConnectorParamsDiscoveryRequest request) {

        var wellKnownRequest = new Request.Builder()
                .url(request.counterPartyAddress() + DSP_DISCOVERY_PATH)
                .get()
                .build();

        var resolveDid = identifierMapper.mapToDid(request.identifier());
        return httpClient.executeAsync(wellKnownRequest, emptyList())
                .thenCombine(resolveDid, (response, did) -> {
                    try {
                        if (!response.isSuccessful()) {
                            var msg = "Counter party well-known endpoint has failed with status %s and message: %s"
                                    .formatted(response.code(), response.message());
                            monitor.warning(msg);
                            throw new BadGatewayException(msg);

                        } else {
                            var discoveredParameters = Json.createObjectBuilder();
                            var bytesBody = response.body().bytes();
                            var protocolVersions = mapper.readValue(bytesBody, ProtocolVersions.class);

                            var version20251 = findProtocolVersion(Dsp2025Constants.V_2025_1_VERSION, protocolVersions);
                            if (version20251 != null && did != null) {
                                addDiscoveredParameters(Dsp2025Constants.V_2025_1_VERSION,
                                        dspVersionMapper.identifierForDspVersion(did, Dsp2025Constants.V_2025_1_VERSION).join(),
                                        request.counterPartyAddress() + removeTrailingSlash(version20251.path()),
                                        discoveredParameters);

                                return discoveredParameters.build();
                            } else {
                                var version08 = findProtocolVersion(Dsp08Constants.V_08_VERSION, protocolVersions);
                                if (version08 != null) {
                                    addDiscoveredParameters(Dsp08Constants.V_08_VERSION,
                                            dspVersionMapper.identifierForDspVersion(did, Dsp08Constants.V_08_VERSION).join(),
                                            request.counterPartyAddress() + removeTrailingSlash(version08.path()),
                                            discoveredParameters);

                                    return discoveredParameters.build();
                                }
                            }
                            throw new InvalidRequestException(
                                    "The counter party does not support any of the expected protocol versions (%s, %s)"
                                            .formatted(Dsp08Constants.V_08_VERSION, Dsp2025Constants.V_2025_1_VERSION));
                        }
                    } catch (IOException e) {
                        var msg = "An exception with the following message occurred while executing dsp version request: %s"
                                .formatted(e.getMessage());
                        monitor.warning(msg, e);
                        throw new BadGatewayException(msg);
                    }
                });
    }

    @Override
    public CompletableFuture<JsonArray> discoverConnectors(ConnectorDiscoveryRequest request) {
        identifierMapper.mapToDid(request.counterPartyId()).thenApply(didResolver::resolve)
                .thenApply(result -> {
                    if (result.failed()) {
                        var msg = format("Error, downloading the DID" + ": %s", result.getFailureDetail());
                        monitor.warning(msg);
                        throw new InvalidRequestException(msg);
                    }
                    var didDocument = result.getContent();
                    var connectorRequests = didDocument.getService().stream()
                            .filter(entry -> DATA_SERVICE.equals(entry.getType()))
                            .map(Service::getServiceEndpoint)
                            .map(endpoint -> discoverVersionParams(
                                    new ConnectorParamsDiscoveryRequest(request.counterPartyId(), endpoint)))
                            .toList();

                    var returnArrayBuilder = createArrayBuilder();

                    for (CompletableFuture<JsonObject> future : connectorRequests) {
                        try {
                            var paramsResult = future.join();
                            returnArrayBuilder.add(paramsResult);
                        } catch (Exception e) {
                            monitor.severe("Exception during connector discovery, omit endpoint result", e);
                        }
                    }

                    var returnArray = returnArrayBuilder.build();
                    if (returnArray.isEmpty()) {
                        throw new InvalidRequestException(
                                "No connector endpoints found for did %s".formatted(request.counterPartyId()));
                    }
                    return returnArrayBuilder;
                });
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

    private void addDiscoveredParameters(String version, String counterPartyId, String address, JsonObjectBuilder
            versionParameters) {
        var protocol = Dsp2025Constants.V_2025_1_VERSION.equals(version)
                ? Dsp2025Constants.DATASPACE_PROTOCOL_HTTP_V_2025_1
                : HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
        versionParameters.add(CatalogRequest.CATALOG_REQUEST_PROTOCOL, protocol);
        versionParameters.add(CatalogRequest.CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, address);
        versionParameters.add(CatalogRequest.CATALOG_REQUEST_COUNTER_PARTY_ID, counterPartyId);
    }
}
