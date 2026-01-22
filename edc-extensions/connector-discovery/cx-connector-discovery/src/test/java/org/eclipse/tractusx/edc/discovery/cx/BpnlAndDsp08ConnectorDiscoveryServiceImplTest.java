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

package org.eclipse.tractusx.edc.discovery.cx;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.tractusx.edc.discovery.cx.service.BpnlAndDsp08ConnectorDiscoveryServiceImpl;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.CacheConfig;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryRequest;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_COUNTER_PARTY_ADDRESS;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_COUNTER_PARTY_ID;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_PROTOCOL;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * This class only tests the delta to the default service implementation, basically all error cases are mainly handled
 * there, so this concentrates on positive test cases.
 */
class BpnlAndDsp08ConnectorDiscoveryServiceImplTest {

    private final BdrsClient bdrsClient = mock();
    private final DidResolverRegistry didResolver = mock();
    private final ObjectMapper mapper = new ObjectMapper().configure(
            com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final EdcHttpClient httpClient = mock();
    private final Clock clock = Clock.systemUTC();
    private final Monitor monitor = mock();

    private final BpnlAndDsp08ConnectorDiscoveryServiceImpl testee = new BpnlAndDsp08ConnectorDiscoveryServiceImpl(
            bdrsClient, httpClient, didResolver, mapper, new CacheConfig(1000, clock), monitor);

    private static final String TEST_DID = "did:web:providerdid";
    private static final String TEST_BPNL = "BPNL1234567890AB";
    private static final String TEST_UNKNOWN_IDENTIFIER = "unknown";
    private static final String TEST_ADDRESS = "http://example.org/api/dsp";
    private static final String TEST_ADDRESS_2 = "http://example.org/c3/api/v1/dsp";

    private static final String VERSION_PROTOCOL_NEW = "dataspace-protocol-http:2025-1";
    private static final String VERSION_PROTOCOL_OLD = "dataspace-protocol-http";

    private static final DidDocument RETURNED_DOCUMENT = DidDocument.Builder.newInstance()
            .id(TEST_DID)
            .service(List.of(
                    new Service(TEST_DID + "c1", "DataService", TEST_ADDRESS),
                    new Service(TEST_DID + "c2", "CatalogService", "http://younameit"),
                    new Service(TEST_DID + "c3", "DataService", TEST_ADDRESS_2),
                    new Service(TEST_DID + "cs", "CredentialService", "http://dontcare")))
            .build();

    private static final JsonObject STANDARD_VERSION_METADATA = Json.createObjectBuilder()
            .add("protocolVersions", Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("version", "v0.8")
                            .add("path", "/"))
                    .add(Json.createObjectBuilder()
                            .add("version", "2025-1")
                            .add("path", "/somePath"))).build();

    private static final JsonObject ONLY_OLD_VERSION_METADATA = Json.createObjectBuilder()
            .add("protocolVersions", Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("version", "v0.8")
                            .add("path", "/"))).build();

    @ParameterizedTest
    @ValueSource(strings = { TEST_DID, TEST_BPNL })
    void discoverVersionParams_shouldReturnDsp2025_whenDsp2025Available(String counterPartyId) {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest(counterPartyId, TEST_ADDRESS);

        var expectedJson = Json.createObjectBuilder()
                .add(CATALOG_REQUEST_COUNTER_PARTY_ID, TEST_DID)
                .add(CATALOG_REQUEST_PROTOCOL, VERSION_PROTOCOL_NEW)
                .add(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, TEST_ADDRESS + "/somePath")
                .build();

        when(bdrsClient.resolveDid(paramsDiscoveryRequest.counterPartyId()))
                .thenReturn(TEST_DID);
        when(httpClient.executeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(STANDARD_VERSION_METADATA.toString()).build()));

        var response = testee.discoverVersionParams(paramsDiscoveryRequest).join();

        assertThat(response).isEqualTo(expectedJson);
    }

    @ParameterizedTest
    @ValueSource(strings = { TEST_DID, TEST_BPNL })
    void discoverVersionParams_shouldReturnDsp08_whenDidDsp2025NotAvailable(String counterPartyId) {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest(counterPartyId, TEST_ADDRESS);

        when(bdrsClient.resolveBpn(counterPartyId))
                .thenReturn(TEST_BPNL);
        when(httpClient.executeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(ONLY_OLD_VERSION_METADATA.toString()).build()));

        var expectedJson = Json.createObjectBuilder()
                .add(CATALOG_REQUEST_COUNTER_PARTY_ID, TEST_BPNL)
                .add(CATALOG_REQUEST_PROTOCOL, VERSION_PROTOCOL_OLD)
                .add(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, TEST_ADDRESS)
                .build();

        var response = testee.discoverVersionParams(paramsDiscoveryRequest).join();

        assertThat(response).isEqualTo(expectedJson);
    }

    @Test
    void discoverVersionParams_shouldReturnFailure_whenDidNotResolvable() {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest(TEST_BPNL, TEST_ADDRESS);

        when(bdrsClient.resolveDid(paramsDiscoveryRequest.counterPartyId()))
                .thenReturn(null);
        when(httpClient.executeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(STANDARD_VERSION_METADATA.toString()).build()));

        assertThatThrownBy(() -> testee.discoverVersionParams(paramsDiscoveryRequest).join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("cannot be mapped to a");
    }


    @Test
    void discoverVersionParams_shouldReturnFailure_whenBpnlNotResolvable() {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest(TEST_DID, TEST_ADDRESS);

        when(bdrsClient.resolveDid(paramsDiscoveryRequest.counterPartyId()))
                .thenReturn(null);
        when(httpClient.executeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(ONLY_OLD_VERSION_METADATA.toString()).build()));

        assertThatThrownBy(() -> testee.discoverVersionParams(paramsDiscoveryRequest).join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("cannot be mapped to a");
    }

    @ParameterizedTest
    @ArgumentsSource(VersionMetadataProvider.class)
    void discoverVersionParams_shouldReturnFailure_whenUnknownIdentifierUsed(JsonObject versionMetadata) {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest(TEST_UNKNOWN_IDENTIFIER, TEST_ADDRESS);

        when(bdrsClient.resolveDid(paramsDiscoveryRequest.counterPartyId()))
                .thenReturn(null);
        when(httpClient.executeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(versionMetadata.toString()).build()));

        assertThatThrownBy(() -> testee.discoverVersionParams(paramsDiscoveryRequest).join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("is of unknown type");
    }

    private static class VersionMetadataProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    of(STANDARD_VERSION_METADATA),
                    of(ONLY_OLD_VERSION_METADATA)
            );
        }
    }

    @Test
    void discoverVersionParams_shouldReturnFailure_whenNoVersionSupported() {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest(TEST_DID, TEST_ADDRESS);

        var versionMetadata = Json.createObjectBuilder()
                .add("protocolVersions", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("version", "2024/1")
                                .add("path", "/somePath"))).build();

        when(httpClient.executeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(versionMetadata.toString()).build()));
        assertThatThrownBy(() -> testee.discoverVersionParams(paramsDiscoveryRequest).join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("The counterparty does not support any of the expected protocol versions");
    }

    @ParameterizedTest
    @ValueSource(strings = { TEST_DID, TEST_BPNL })
    void discoverConnectors_shouldReturnExpectedValues_StandardCall(String counterPartyId) {
        var connectorDiscoveryRequest = new ConnectorDiscoveryRequest(counterPartyId, emptyList());

        var expectedJson1 = Json.createObjectBuilder()
                .add(CATALOG_REQUEST_COUNTER_PARTY_ID, TEST_DID)
                .add(CATALOG_REQUEST_PROTOCOL, VERSION_PROTOCOL_NEW)
                .add(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, TEST_ADDRESS + "/somePath")
                .build();

        var expectedJson2 = Json.createObjectBuilder()
                .add(CATALOG_REQUEST_COUNTER_PARTY_ID, TEST_BPNL)
                .add(CATALOG_REQUEST_PROTOCOL, VERSION_PROTOCOL_OLD)
                .add(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, TEST_ADDRESS_2)
                .build();

        when(didResolver.resolve(any())).thenReturn(Result.success(RETURNED_DOCUMENT));
        when(httpClient.executeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(STANDARD_VERSION_METADATA.toString()).build()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(ONLY_OLD_VERSION_METADATA.toString()).build()));
        when(bdrsClient.resolveDid(TEST_BPNL))
                .thenReturn(TEST_DID);
        when(bdrsClient.resolveBpn(TEST_DID))
                .thenReturn(TEST_BPNL);

        var response = testee.discoverConnectors(connectorDiscoveryRequest).join();

        var returnedData = response.getValuesAs(JsonObject.class);

        assertThat(returnedData.size()).isEqualTo(2);
        assertThat(returnedData).containsExactly(expectedJson1, expectedJson2);

        verify(didResolver, times(1)).resolve(TEST_DID);
        verify(httpClient, times(2)).executeAsync(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = { TEST_DID, TEST_BPNL })
    void discoverConnectors_shouldReturnExpectedValues_WithKnownConnectorsProvided(String counterPartId) {
        var additionalOne = "http://example.com/connector_additional/api/dsp";
        var additionalTwo = "http://example.com/connector_extra/api/v1/dsp";

        var connectorDiscoveryRequest = new ConnectorDiscoveryRequest(counterPartId, List.of(additionalOne, additionalTwo));

        var expectedJson1 = Json.createObjectBuilder()
                .add(CATALOG_REQUEST_COUNTER_PARTY_ID, TEST_DID)
                .add(CATALOG_REQUEST_PROTOCOL, VERSION_PROTOCOL_NEW)
                .add(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, TEST_ADDRESS + "/somePath")
                .build();

        var expectedJson2 = Json.createObjectBuilder()
                .add(CATALOG_REQUEST_COUNTER_PARTY_ID, TEST_BPNL)
                .add(CATALOG_REQUEST_PROTOCOL, VERSION_PROTOCOL_OLD)
                .add(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, TEST_ADDRESS_2)
                .build();

        var expectedJson3 = Json.createObjectBuilder()
                .add(CATALOG_REQUEST_COUNTER_PARTY_ID, TEST_DID)
                .add(CATALOG_REQUEST_PROTOCOL, VERSION_PROTOCOL_NEW)
                .add(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, additionalOne + "/somePath")
                .build();

        var expectedJson4 = Json.createObjectBuilder()
                .add(CATALOG_REQUEST_COUNTER_PARTY_ID, TEST_BPNL)
                .add(CATALOG_REQUEST_PROTOCOL, VERSION_PROTOCOL_OLD)
                .add(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, additionalTwo)
                .build();

        when(didResolver.resolve(any())).thenReturn(Result.success(RETURNED_DOCUMENT));
        when(httpClient.executeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(STANDARD_VERSION_METADATA.toString()).build()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(ONLY_OLD_VERSION_METADATA.toString()).build()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(STANDARD_VERSION_METADATA.toString()).build()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(ONLY_OLD_VERSION_METADATA.toString()).build()));
        when(bdrsClient.resolveDid(TEST_BPNL))
                .thenReturn(TEST_DID);
        when(bdrsClient.resolveBpn(TEST_DID))
                .thenReturn(TEST_BPNL)
                .thenReturn(TEST_BPNL);

        var response = testee.discoverConnectors(connectorDiscoveryRequest).join();

        var returnedData = response.getValuesAs(JsonObject.class);

        assertThat(returnedData.size()).isEqualTo(4);
        assertThat(returnedData).containsExactly(expectedJson1, expectedJson2, expectedJson3, expectedJson4);

        verify(didResolver, times(1)).resolve(TEST_DID);
        verify(httpClient, times(4)).executeAsync(any(), any());
    }

    static okhttp3.Response.Builder dummyResponseBuilder(String body) {
        return new okhttp3.Response.Builder()
                .code(200)
                .message("any")
                .body(ResponseBody.create(body, MediaType.get("application/json")))
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url(TEST_ADDRESS).build());
    }
}