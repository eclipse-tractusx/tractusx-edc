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
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
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
import org.eclipse.tractusx.edc.discovery.v4alpha.exceptions.UnexpectedResultApiException;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryRequest;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryService;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;

/**
 * This class implements the general functionality for connector discovery. It basically provides means to download
 * the DID document and parse it for 'DataService' entries in the service section as well as to download the version
 * metadata for a connector and to transform them into the parameters needed for the management api.
 */
public abstract class BaseConnectorDiscoveryServiceImpl implements ConnectorDiscoveryService {
    private static final int CACHE_SIZE = 200;
    private static final String DSP_DISCOVERY_PATH = ".well-known/dspace-version";
    private static final String DATA_SERVICE = "DataService";
    protected static final String DID_PREFIX = "did:";

    private final EdcHttpClient httpClient;
    private final DidResolverRegistry didResolver;
    private final ObjectMapper mapper;
    private final long cacheValidity;
    private final Clock clock;
    private final Monitor monitor;

    /*
     * An ordered list of prioritized versions supported by this connector. This is a field set by a derived service
     * implementation. The ordering is in a way, that the version at index 0 has highest priority, i.e., will be used
     * if the counterparty connector supports it as well.
     */
    private final List<String> supportedVersions;

    /*
     * A list of version mappings from official names expected in a version metadata to the name of the version
     * in the management api. Add new versions, if applicable, 2024/1 is omitted on purpose, add it, if your
     * implementation makes use of it.
     */
    private final Map<String, String> versionMapper = Map.of(
            Dsp2025Constants.V_2025_1_VERSION, Dsp2025Constants.DATASPACE_PROTOCOL_HTTP_V_2025_1,
            Dsp08Constants.V_08_VERSION, HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP);
    private final ConcurrentLruCache<String, TimestampedValue<ProtocolVersion>> versionsCache;

    public record CacheConfig(long cacheValidity, Clock clock) {
    }

    /**
     * All instantiable services will derive from this base class, so the constructor is protected.
     *
     * @param httpClient        The HTTP client service to execute requests
     * @param didResolver       The DID Document download service
     * @param mapper            The mapper to create read JSON response data into an object
     * @param supportedVersions A list of versions, this connector supports, needed to find the latest common match.
     *                          It is expected that the ordering is in a way, that the later a version was released the
     *                          lower the index in the provided list.
     * @param cacheConfig       Some cache configuration parameters
     * @param monitor           Required to log information for informing operations about events
     */
    protected BaseConnectorDiscoveryServiceImpl(
            EdcHttpClient httpClient,
            DidResolverRegistry didResolver,
            ObjectMapper mapper,
            List<String> supportedVersions,
            CacheConfig cacheConfig,
            Monitor monitor) {
        this.httpClient = httpClient;
        this.didResolver = didResolver;
        this.mapper = mapper;
        this.supportedVersions = supportedVersions;
        this.cacheValidity = cacheConfig.cacheValidity();
        this.clock = cacheConfig.clock();
        this.monitor = monitor;

        versionsCache = new ConcurrentLruCache<>(CACHE_SIZE);
    }

    /*
     * Implementation of the public service api for requesting version parameters from the counterparty connector
     *
     * It uses a cache to not call the counterparty all the time, if called, it parses the returned data into the
     * expected result body.
     */
    @Override
    public CompletableFuture<JsonObject> discoverVersionParams(ConnectorParamsDiscoveryRequest request) {
        stringNotEmpty(request.counterPartyId());
        stringNotEmpty(request.counterPartyAddress());

        var versionEndpoint = createFullPath(request.counterPartyAddress(), DSP_DISCOVERY_PATH);
        var cacheEntry = versionsCache.get(versionEndpoint);

        if (cacheEntry != null) {
            if (cacheEntry.isExpired(clock)) { // lazy evict expired values
                versionsCache.remove(versionEndpoint);
            } else {
                return CompletableFuture.completedFuture(createResultObjectFromProtocolVersionData(request, cacheEntry.value()));
            }
        }

        var wellKnownRequest = new Request.Builder()
                .url(versionEndpoint)
                .get()
                .build();

        return httpClient.executeAsync(wellKnownRequest, emptyList())
                .thenApply(response -> {
                    if (!response.isSuccessful()) {
                        var msg = "Counterparty well-known endpoint has failed with status %s and message: %s"
                                .formatted(response.code(), response.message());
                        monitor.warning(msg);
                        throw new BadGatewayException(msg);
                    }

                    var protocolVersion = extractLatestSupportedVersion(parseResponseBody(response));
                    var resultObject = createResultObjectFromProtocolVersionData(request, protocolVersion);
                    versionsCache.put(versionEndpoint, new TimestampedValue<>(protocolVersion, cacheValidity));
                    return resultObject;
                });
    }

    /**
     * The connector discovery basically deals with two potential input root patterns. First input pattern is, that
     * the base root of a connector is provided. This is the root endpoint of all dsp endpoints and is basically the root
     * to get the version metadata endpoint by adding '/.well-known/dspace-version' to the provided path. The second
     * pattern is, that the complete endpoint to the version metadata endpoint is provided.
     * <p>
     * On the other hand side, as output in several places of this service stack, an root that is based on the root
     * endpoint as described above plus an additional subpath is needed to create a meaningful root that is used or
     * returned to the caller.
     * <p>
     * This method basically provides the handling of these input output relationships by providing the means to give
     * any input pattern and create an output root that is based on the root plus the intended subpath. In addition,
     * the method ensures, that potentially unneeded slashes are removed at the edges of the provided path parts.
     *
     * @param root    Input url that points either to the root endpoint of the DSP service or the version metadata endpoint
     * @param subpath Intended subpath to be added to the input url
     * @return A joint path cleaned from unneeded slashes following the pattern 'root/subpath'
     * @throws InvalidRequestException If the created path, i.e., the root parameter is not a correct url.
     */
    protected String createFullPath(String root, String subpath) {
        var input = root;
        if (root.endsWith(DSP_DISCOVERY_PATH)) {
            input = root.substring(0, root.length() - DSP_DISCOVERY_PATH.length() - 1);
        }
        try {
            return new URL(removeSurroundingSlash(removeSurroundingSlash(input) + "/" + removeSurroundingSlash(subpath)))
                    .toExternalForm();
        } catch (MalformedURLException e) {
            throw new InvalidRequestException("Provided endpoint url of connector cannot be parsed as URL: %s".formatted(root));
        }
    }

    /*
     * Just removes all leading and prevailing slashes of a path to ensure, that no unneeded slashes are added to a path.
     */
    private String removeSurroundingSlash(String path) {
        var result = path;
        if (result.startsWith("/")) {
            result = result.substring(1);
        }
        if (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /*
     * Creates a result json object from the version metadata and the request information.
     */
    private JsonObject createResultObjectFromProtocolVersionData(
            ConnectorParamsDiscoveryRequest request, ProtocolVersion versionInformation) {
        return Optional.ofNullable(
                        createVersionParameterForProtocolVersion(
                                request.counterPartyId(),
                                createFullPath(request.counterPartyAddress(), versionInformation.path()),
                                versionInformation.version()))
                .orElseThrow(() -> {
                    var joined = supportedVersions.stream().collect(joining(", ", "", ""));
                    return new InvalidRequestException(
                            "The counterparty does not support any of the expected protocol versions (%s)"
                                    .formatted(joined));
                });
    }

    /**
     * This method is specific to an implementation of the connector discovery. It depends on the supported DSP
     * versions and the handling of the counterparty id used in a certain protocol version. Therefore this has
     * to be implemented in a derived implementation of the functionality.
     *
     * @param counterPartyId     The counterparty id used in the request, if multiple types are possible, the implementation
     *                           has to identify the identifier type and act accordingly.
     * @param versionAddress     The counterparty base connector endpoint address for the version required to be used
     *                           in DSP requests.
     * @param versionInformation The version string returned by the version metadata endpoint representing the latest
     *                           version supported by the called connector and the calling connector.
     * @return A 'JsonObject' which contains an entry for all three relevant parameters. See 'createVersionParameterRecord'
     *         for details.
     * @throws RuntimeException A meaningful exception from 'org.eclipse.edc.web.spi.exception' to allow, e.g., a correct
     *                          selection of a status code for the request.
     */
    protected abstract JsonObject createVersionParameterForProtocolVersion(
            String counterPartyId, String versionAddress, String versionInformation);

    /**
     * A helper method to be used within 'createVersionParameterForProtocolVersion' to create the actual 'JsonObject'
     * that transports the information.
     *
     * @param version        The version string of the version as announced in the version metadata. This will be translated
     *                       to the version string to be used in the management api for the protocol property that identifies
     *                       the DSP version to be used for the request.
     * @param counterPartyId The counterparty id to be used in the management api for a DSP call. In catalog requests,
     *                       this is the counterPartyId field, but it can be named different in other DSP call requests.
     * @param address        The base address for referencing the counterparty in the management api to initiate a DSP
     *                       call. The management api will typically simply add the DSP subpath to this path according to the
     *                       version specificiation.
     * @return A 'JsonObject', that represents the record of data expected from the endpoints
     */
    protected JsonObject createVersionParameterRecord(String version, String counterPartyId, String address) {
        var versionParameters = Json.createObjectBuilder();
        versionParameters.add(CatalogRequest.CATALOG_REQUEST_PROTOCOL, versionMapper.get(version));
        versionParameters.add(CatalogRequest.CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, address);
        versionParameters.add(CatalogRequest.CATALOG_REQUEST_COUNTER_PARTY_ID, counterPartyId);
        return versionParameters.build();
    }

    /*
     * Parses the response of a version metadata endpoint and translates it into a list of 'ProtocolVersion' objects
     */
    private ProtocolVersions parseResponseBody(Response response) {
        try {
            var bytesBody = response.body().bytes();
            return Optional.ofNullable(mapper.readValue(bytesBody, ProtocolVersions.class))
                    .filter(r -> r.protocolVersions() != null)
                    .orElseThrow(() -> new BadGatewayException("No protocol versions found"));
        } catch (IOException e) {
            var msg = "An exception with the following message occurred while executing dsp version request: %s"
                    .formatted(e.getMessage());
            monitor.warning(msg, e);
            throw new BadGatewayException(msg);
        }
    }

    /*
     * This method takes a list of supported version metadata objects from a connector and determines the latest version
     * supported by this connector and the counterparty connector. The prioritization is done based on the ordering of
     * the supportedVersions field. The version with index 0 has highest priority, i.e., is expected to be the latest
     * version supported.
     */
    private ProtocolVersion extractLatestSupportedVersion(ProtocolVersions versions) {
        for (String version : supportedVersions) {
            var foundVersion = versions.protocolVersions().stream()
                    .filter(pv -> version.equals(pv.version()))
                    .findFirst()
                    .filter(v -> v.path() != null);
            if (foundVersion.isPresent()) {
                return foundVersion.get();
            }
        }

        var joined = supportedVersions.stream().collect(joining(", ", "", ""));
        throw new InvalidRequestException(
                "The counterparty does not support any of the expected protocol versions (%s)"
                        .formatted(joined));
    }

    /*
     * Implementation of the public service api method.
     *
     * Reads the DID documents service entries uses the endpoint information, add the known endpoints and retrieve
     * the version metadata for each. All is done asynchronously, so that the requests for version metadata on
     * different connectors is done in parallel.
     */
    @Override
    public CompletableFuture<JsonArray> discoverConnectors(ConnectorDiscoveryRequest request) {
        stringNotEmpty(request.counterPartyId());
        if (!request.counterPartyId().startsWith(DID_PREFIX)) {
            throw new InvalidRequestException("The counterparty id has to be a did, is %s".formatted(request.counterPartyId()));
        }
        if (request.knownConnectors() == null) {
            throw new UnexpectedResultApiException("Null not allowed for knownConnector collection");
        }
        return discoverConnectorsFromDidDocument(request.counterPartyId())
                .thenApply(serviceEndpoints -> {
                    var allEndpoints = Stream.concat(
                                    serviceEndpoints.stream(),
                                    request.knownConnectors().stream())
                            .distinct()
                            .toList();
                    return resolveVersionEndpoints(request.counterPartyId(), allEndpoints);
                });
    }

    /*
     * Read the DID document and extract the 'DataService entries in the service section, extract the endpoint
     * information, as everything else is of no concern.
     */
    private CompletableFuture<Collection<String>> discoverConnectorsFromDidDocument(String did) {
        return CompletableFuture.supplyAsync(() -> {
            var didDocument = readDidDocument(did);
            return didDocument.getService().stream()
                    .filter(entry -> DATA_SERVICE.equals(entry.getType()))
                    .map(Service::getServiceEndpoint)
                    .toList();
        });
    }

    /*
     * Actually read the did document and handle issues properly
     */
    private DidDocument readDidDocument(String did) {
        var result = didResolver.resolve(did);
        if (result.failed()) {
            var msg = "Error, downloading the did" + ": %s".formatted(result.getFailureDetail());
            monitor.warning(msg);
            throw new InvalidRequestException(msg);
        }
        return result.getContent();
    }

    /*
     * Resolve version metadata for each detected connector asynchronously and add the result to the return array.
     * Failing endpoints will simply be ignored. There is only an error, if no data is found.
     */
    private JsonArray resolveVersionEndpoints(String counterPartyId, List<String> endpoints) {
        JsonArrayBuilder returnArrayBuilder = Json.createArrayBuilder();
        var discoveryCalls = new CompletableFuture[endpoints.size()];

        for (int i = 0; i < endpoints.size(); i++) {
            var endpoint = endpoints.get(i);
            discoveryCalls[i] = discoverVersionParams(new ConnectorParamsDiscoveryRequest(counterPartyId, endpoint))
                    .handle((result, throwable) -> {
                        if (throwable != null) {
                            monitor.severe("Exception during connector discovery, omit endpoint result", throwable);
                        } else {
                            returnArrayBuilder.add(result);
                        }
                        return null;
                    });
        }

        CompletableFuture.allOf(discoveryCalls).join();
        var returnArray = returnArrayBuilder.build();
        if (returnArray.isEmpty()) {
            throw new InvalidRequestException("No connector endpoints found for counterPartyId %s".formatted(counterPartyId));
        }
        return returnArray;
    }

    private void stringNotEmpty(String input) {
        if (input == null || input.isBlank()) {
            throw new InvalidRequestException("Input data must not be empty");
        }
    }
}
