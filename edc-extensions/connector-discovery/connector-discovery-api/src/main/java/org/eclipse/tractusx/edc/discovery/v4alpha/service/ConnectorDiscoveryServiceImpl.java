/*
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.discovery.v4alpha.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol;
import org.eclipse.edc.protocol.dsp.spi.type.Dsp08Constants;
import org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.eclipse.edc.protocol.spi.ProtocolVersions;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.util.collection.ConcurrentLruCache;
import org.eclipse.edc.util.collection.TimestampedValue;
import org.eclipse.edc.web.spi.exception.BadGatewayException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryRequest;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryService;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.DspVersionToIdentifierMapper;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.IdentifierToDidMapper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Clock;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static jakarta.json.Json.createArrayBuilder;
import static java.util.Collections.emptyList;

public class ConnectorDiscoveryServiceImpl implements ConnectorDiscoveryService {

    private static final String DSP_DISCOVERY_PATH = "/.well-known/dspace-version";
    private static final String DATA_SERVICE = "DataService";
    private static final int CACHE_SIZE = 200;

    private final DidResolverRegistry didResolver;
    private final EdcHttpClient httpClient;
    private final ObjectMapper mapper;
    private final IdentifierToDidMapper identifierMapper;
    private final DspVersionToIdentifierMapper dspVersionMapper;
    private final Clock clock;
    private final long cacheValidity;
    private final Monitor monitor;

    private final ConcurrentLruCache<String, TimestampedValue<ProtocolVersions>> versionsCache;

    public ConnectorDiscoveryServiceImpl(
            DidResolverRegistry didResolver,
            EdcHttpClient httpClient,
            ObjectMapper mapper,
            IdentifierToDidMapper identifierMapper,
            DspVersionToIdentifierMapper dspVersionMapper,
            Clock clock,
            long cacheValidityInMilliseconds,
            Monitor monitor) {
        this.didResolver = didResolver;
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.identifierMapper = identifierMapper;
        this.dspVersionMapper = dspVersionMapper;
        this.clock = clock;
        this.cacheValidity = cacheValidityInMilliseconds;
        this.monitor = monitor;

        versionsCache = new ConcurrentLruCache<>(CACHE_SIZE);
    }

    @Override
    public CompletableFuture<JsonObject> discoverVersionParams(ConnectorParamsDiscoveryRequest request) {
        var connectorEndpoint = createDiscoveryEndpoint(request.counterPartyAddress());
        var cacheEntry = versionsCache.get(connectorEndpoint);

        CompletableFuture<ProtocolVersions> future = null;

        if (cacheEntry != null) {
            if (cacheEntry.isExpired(clock)) { // lazy evict expired values
                versionsCache.remove(connectorEndpoint);
            } else {
                future = CompletableFuture.completedFuture(cacheEntry.value());
            }
        }

        if (future == null) {
            var wellKnownRequest = new Request.Builder()
                    .url(connectorEndpoint)
                    .get()
                    .build();

            future = httpClient.executeAsync(wellKnownRequest, emptyList())
                    .thenApply(response -> {
                        if (!response.isSuccessful()) {
                            var msg = "Counter party well-known endpoint has failed with status %s and message: %s"
                                    .formatted(response.code(), response.message());
                            monitor.warning(msg);
                            throw new BadGatewayException(msg);
                        }
                        var protocolVersions = parseResponseBody(response);
                        versionsCache.put(connectorEndpoint, new TimestampedValue<>(protocolVersions, cacheValidity));
                        return protocolVersions;
                    });
        }
        return future
                .thenApply(protocolVersions -> {
                    var did = identifierMapper.mapToDid(request.identifier());
                    var discoveredParameters = Json.createObjectBuilder();

                    var version20251 = findProtocolVersion(Dsp2025Constants.V_2025_1_VERSION, protocolVersions);
                    if (version20251 != null && did != null) {
                        addDiscoveredParameters(Dsp2025Constants.V_2025_1_VERSION,
                                dspVersionMapper.identifierForDspVersion(did, Dsp2025Constants.V_2025_1_VERSION),
                                request.counterPartyAddress() + removeTrailingSlash(version20251.path()),
                                discoveredParameters);

                        return discoveredParameters.build();
                    } else {
                        var version08 = findProtocolVersion(Dsp08Constants.V_08_VERSION, protocolVersions);
                        if (version08 != null) {
                            addDiscoveredParameters(Dsp08Constants.V_08_VERSION,
                                    dspVersionMapper.identifierForDspVersion(did, Dsp08Constants.V_08_VERSION),
                                    request.counterPartyAddress() + removeTrailingSlash(version08.path()),
                                    discoveredParameters);

                            return discoveredParameters.build();
                        }
                    }
                    throw new InvalidRequestException(
                            "The counter party does not support any of the expected protocol versions (%s, %s)"
                                    .formatted(Dsp08Constants.V_08_VERSION, Dsp2025Constants.V_2025_1_VERSION));
                });
    }

    private ProtocolVersions parseResponseBody(Response response) {
        try {
            var bytesBody = response.body().bytes();
            return mapper.readValue(bytesBody, ProtocolVersions.class);
        } catch (IOException e) {
            var msg = "An exception with the following message occurred while executing dsp version request: %s"
                    .formatted(e.getMessage());
            monitor.warning(msg, e);
            throw new BadGatewayException(msg);
        }
    }

    private String createDiscoveryEndpoint(String endpoint) {
        try {
            if (endpoint.endsWith(DSP_DISCOVERY_PATH)) {
                return new URL(endpoint).toExternalForm();
            } else {
                return new URL(removeTrailingSlash(endpoint) + DSP_DISCOVERY_PATH).toExternalForm();
            }
        } catch (MalformedURLException e) {
            throw new InvalidRequestException("Connector endpoint not an URL: %s".formatted(endpoint));
        }
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

    @Override
    public CompletableFuture<JsonArray> discoverConnectors(ConnectorDiscoveryRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            var did = identifierMapper.mapToDid(request.counterPartyId());
            var didDocument = readDidDocument(did);
            var serviceEndpoints = Stream.concat(
                    didDocument.getService().stream()
                            .filter(entry -> DATA_SERVICE.equals(entry.getType()))
                            .map(Service::getServiceEndpoint),
                    request.knownConnectors().stream()).toList();

            var returnArrayBuilder = createArrayBuilder();
            var discoveryCalls = new CompletableFuture[serviceEndpoints.size()];

            for (int i = 0; i < serviceEndpoints.size(); i++) {
                discoveryCalls[i] =
                        discoverVersionParams(new ConnectorParamsDiscoveryRequest(request.counterPartyId(), serviceEndpoints.get(i)))
                                .whenComplete((result, throwable) -> {
                                    if (throwable != null) {
                                        monitor.severe("Exception during connector discovery, omit endpoint result", throwable);
                                    } else {
                                        returnArrayBuilder.add(result);
                                    }
                                });
            }

            CompletableFuture.allOf(discoveryCalls).join();

            var returnArray = returnArrayBuilder.build();
            if (returnArray.isEmpty()) {
                throw new InvalidRequestException(
                        "No connector endpoints found for did %s".formatted(did));
            }
            return returnArray;
        });
    }

    private DidDocument readDidDocument(String did) {
        var result = didResolver.resolve(did);
        if (result.failed()) {
            var msg = "Error, downloading the DID" + ": %s".formatted(result.getFailureDetail());
            monitor.warning(msg);
            throw new InvalidRequestException(msg);
        }
        return result.getContent();
    }
}