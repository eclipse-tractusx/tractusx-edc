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

package org.eclipse.tractusx.edc.discovery.v4alpha;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
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
import org.eclipse.edc.web.spi.exception.BadGatewayException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.tractusx.edc.discovery.v4alpha.service.BaseConnectorDiscoveryServiceImpl;
import org.eclipse.tractusx.edc.discovery.v4alpha.service.DefaultConnectorDiscoveryServiceImpl;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryRequest;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryService;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultConnectorDiscoveryServiceImplTest {

    private final DidResolverRegistry didResolver = mock();
    private final ObjectMapper mapper = new ObjectMapper().configure(
            com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final EdcHttpClient httpClient = mock();
    private final Clock clock = Clock.systemUTC();
    private final Monitor monitor = mock();

    private final ConnectorDiscoveryService testee = new DefaultConnectorDiscoveryServiceImpl(
            httpClient, didResolver, mapper,
            new BaseConnectorDiscoveryServiceImpl.CacheConfig(1000, clock), monitor);

    private static final String TEST_DID = "did:web:providerdid";
    private static final String TEST_NON_DID_IDENTIFIER = "BPNL1234567890AB";
    private static final String TEST_ADDRESS = "http://example.org/api/dsp";
    private static final String TEST_ADDRESS_2 = "http://example.org/c3/api/v1/dsp";

    private static final String VERSION_PROTOCOL = "dataspace-protocol-http:2025-1";

    private static final DidDocument RETURNED_DOCUMENT = DidDocument.Builder.newInstance()
            .id(TEST_DID)
            .service(List.of(
                    new Service(TEST_DID + "c1", "DataService", TEST_ADDRESS),
                    new Service(TEST_DID + "c2", "CatalogService", "http://younameit"),
                    new Service(TEST_DID + "c3", "DataService", TEST_ADDRESS_2),
                    new Service(TEST_DID + "cs", "CredentialService", "http://dontcare")))
            .build();

    private static final JsonObjectBuilder STANDARD_VERSION_08_DATA = Json.createObjectBuilder()
            .add("version", "v0.8")
            .add("path", "/");

    private static final JsonObjectBuilder STANDARD_VERSION_20251_DATA = Json.createObjectBuilder()
            .add("version", "2025-1")
            .add("path", "/somePath");

    private static final JsonObject STANDARD_VERSION_METADATA = Json.createObjectBuilder()
            .add("protocolVersions", Json.createArrayBuilder()
                    .add(STANDARD_VERSION_08_DATA)
                    .add(STANDARD_VERSION_20251_DATA)).build();

    @Test
    void discoverVersionParams_shouldReturnDsp2025_whenDsp2025Available() throws IOException {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest(TEST_DID, TEST_ADDRESS);

        var expectedJson = Json.createObjectBuilder()
                .add(CATALOG_REQUEST_COUNTER_PARTY_ID, TEST_DID)
                .add(CATALOG_REQUEST_PROTOCOL, VERSION_PROTOCOL)
                .add(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, TEST_ADDRESS + "/somePath")
                .build();

        when(httpClient.executeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(200, STANDARD_VERSION_METADATA.toString()).build()));

        var response = testee.discoverVersionParams(paramsDiscoveryRequest).join();

        assertThat(response).isEqualTo(expectedJson);
    }

    @Test
    void discoverVersionParams_shouldReturnUseCacheDuringSucccess() throws IOException, InterruptedException {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest(TEST_DID, TEST_ADDRESS);

        var expectedJson = Json.createObjectBuilder()
                .add(CATALOG_REQUEST_COUNTER_PARTY_ID, TEST_DID)
                .add(CATALOG_REQUEST_PROTOCOL, VERSION_PROTOCOL)
                .add(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, TEST_ADDRESS + "/somePath")
                .build();

        when(httpClient.executeAsync(any(), any()))
                .thenReturn(
                        CompletableFuture.completedFuture(
                                dummyResponseBuilder(200, STANDARD_VERSION_METADATA.toString()).build()),
                        CompletableFuture.completedFuture(
                                dummyResponseBuilder(200, STANDARD_VERSION_METADATA.toString()).build()));

        var response = testee.discoverVersionParams(paramsDiscoveryRequest).join();
        assertThat(response).isEqualTo(expectedJson);

        response = testee.discoverVersionParams(paramsDiscoveryRequest).join();
        assertThat(response).isEqualTo(expectedJson);

        Thread.sleep(1500);

        response = testee.discoverVersionParams(paramsDiscoveryRequest).join();
        assertThat(response).isEqualTo(expectedJson);

        verify(httpClient, times(2)).executeAsync(any(), any());
    }

    @Test
    void discoverVersionParams_shouldReturnException_whenOnlyDsp08Available() throws IOException {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest(TEST_DID, TEST_ADDRESS);

        var mockVersionResponseMock = Json.createObjectBuilder()
                .add("protocolVersions", Json.createArrayBuilder()
                        .add(STANDARD_VERSION_08_DATA)).build();

        when(httpClient.executeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(200, mockVersionResponseMock.toString()).build()));

        assertThatThrownBy(() -> {
            testee.discoverVersionParams(paramsDiscoveryRequest).join();
        }).isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("The counterparty does not support any of the expected protocol versions");
    }

    @Test
    void discoverVersionParams_shouldReturnException_whenMetadataEndpointHasError() throws IOException {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest(TEST_DID, TEST_ADDRESS);

        when(httpClient.executeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(404, "Not Found", "Not Found").build()));

        assertThatThrownBy(() -> {
            testee.discoverVersionParams(paramsDiscoveryRequest).join();
        }).isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(BadGatewayException.class)
                .hasMessageContaining("counterparty well-known endpoint has failed with status")
                .hasMessageContaining("404");
    }

    @Test
    void discoverVersionParams_shouldReturnException_whenVersionRequestHasMissingProps() throws IOException {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest(TEST_DID, TEST_ADDRESS);

        var mockVersionResponseMock = Json.createObjectBuilder()
                .add("protocolVersions", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("version", "2025-1"))).build();

        when(httpClient.executeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(200, mockVersionResponseMock.toString()).build()));

        assertThatThrownBy(() -> {
            testee.discoverVersionParams(paramsDiscoveryRequest).join();
        }).isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("The counterparty does not support any of the expected protocol versions");
    }

    @Test
    void discoverVersionParams_shouldReturnException_whenVersionResponseIsOfWrongType() throws IOException {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest(TEST_DID, TEST_ADDRESS);

        var mockVersionResponseMock = Json.createObjectBuilder()
                        .add("urgs", Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add("version", "v0.8")
                                        .add("path", "/"))).build();

        when(httpClient.executeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(200, mockVersionResponseMock.toString()).build()));

        assertThatThrownBy(() -> {
            testee.discoverVersionParams(paramsDiscoveryRequest).join();
        }).isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(BadGatewayException.class)
                .hasMessageContaining("No protocol versions found");
    }

    @Test
    void discoverVersionParams_shouldReturnException_whenVersionResponseHasWrongProps() throws IOException {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest(TEST_DID, TEST_ADDRESS);

        var mockVersionResponseMock = Json.createObjectBuilder()
                .add("protocolVersions", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("urgs", "v0.8")
                                .add("path", "/"))).build();

        when(httpClient.executeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(200, mockVersionResponseMock.toString()).build()));

        assertThatThrownBy(() -> {
            testee.discoverVersionParams(paramsDiscoveryRequest).join();
        }).isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("The counterparty does not support any of the expected protocol versions");
    }

    @ParameterizedTest
    @ArgumentsSource(RequestDataProvider.class)
    void discoverVersionParams_shouldReturnException_whenNoDidIsUsed(
            String id, String address, Class exceptionType, String message) throws IOException {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest(id, address);

        when(httpClient.executeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(200, STANDARD_VERSION_METADATA.toString()).build()));

        assertThatThrownBy(() -> {
            testee.discoverVersionParams(paramsDiscoveryRequest).join();
        }).isInstanceOf(exceptionType)
                .hasMessageContaining(message);
    }

    private static class RequestDataProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    of(TEST_NON_DID_IDENTIFIER, TEST_ADDRESS, CompletionException.class,
                            "CounterPartyId used not supported, must be a did"),
                    of(TEST_DID, "urgs/:123", InvalidRequestException.class,
                            "Provided endpoint url of connector cannot be parsed as URL"),
                    of(null, TEST_ADDRESS, InvalidRequestException.class, "Input data must not be empty"),
                    of(TEST_DID, null, InvalidRequestException.class, "Input data must not be empty"),
                    of("   ", TEST_ADDRESS, InvalidRequestException.class, "Input data must not be empty"),
                    of(TEST_DID, " \n", InvalidRequestException.class, "Input data must not be empty")
            );
        }
    }

    @Test
    void discoverConnectors_shouldReturnExpectedValues_StandardCall()  throws IOException {
        var connectorDiscoveryRequest = new ConnectorDiscoveryRequest(TEST_DID, emptyList());

        var expectedJson1 = Json.createObjectBuilder()
                .add(CATALOG_REQUEST_COUNTER_PARTY_ID, TEST_DID)
                .add(CATALOG_REQUEST_PROTOCOL, VERSION_PROTOCOL)
                .add(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, TEST_ADDRESS + "/somePath")
                .build();

        var expectedJson2 = Json.createObjectBuilder()
                .add(CATALOG_REQUEST_COUNTER_PARTY_ID, TEST_DID)
                .add(CATALOG_REQUEST_PROTOCOL, VERSION_PROTOCOL)
                .add(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, TEST_ADDRESS_2 + "/somePath")
                .build();

        when(didResolver.resolve(any())).thenReturn(Result.success(RETURNED_DOCUMENT));
        when(httpClient.executeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(200, STANDARD_VERSION_METADATA.toString()).build()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(200, STANDARD_VERSION_METADATA.toString()).build()));

        var response = testee.discoverConnectors(connectorDiscoveryRequest).join();

        var returnedData = response.getValuesAs(JsonObject.class);

        assertThat(returnedData.size()).isEqualTo(2);
        assertThat(returnedData).containsExactly(expectedJson1, expectedJson2);

        verify(didResolver, times(1)).resolve(TEST_DID);
        verify(httpClient, times(2)).executeAsync(any(), any());
    }

    @Test
    void discoverConnectors_shouldReturnExpectedValues_WithKnownConnectorsAndFailingVersionMetadataCall()  throws IOException {
        var additionalOne = "http://example.com/connector_additional/api/dsp";
        var additionalTwo = "http://example.com/connector_additional_broken/api/dsp";

        var connectorDiscoveryRequest = new ConnectorDiscoveryRequest(TEST_DID, List.of(additionalOne, additionalTwo));

        var expectedJson1 = Json.createObjectBuilder()
                .add(CATALOG_REQUEST_COUNTER_PARTY_ID, TEST_DID)
                .add(CATALOG_REQUEST_PROTOCOL, VERSION_PROTOCOL)
                .add(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, TEST_ADDRESS + "/somePath")
                .build();

        var expectedJson2 = Json.createObjectBuilder()
                .add(CATALOG_REQUEST_COUNTER_PARTY_ID, TEST_DID)
                .add(CATALOG_REQUEST_PROTOCOL, VERSION_PROTOCOL)
                .add(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, TEST_ADDRESS_2 + "/somePath")
                .build();

        var expectedJson3 = Json.createObjectBuilder()
                .add(CATALOG_REQUEST_COUNTER_PARTY_ID, TEST_DID)
                .add(CATALOG_REQUEST_PROTOCOL, VERSION_PROTOCOL)
                .add(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, additionalOne + "/somePath")
                .build();

        when(didResolver.resolve(any())).thenReturn(Result.success(RETURNED_DOCUMENT));
        when(httpClient.executeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(200, STANDARD_VERSION_METADATA.toString()).build()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(200, STANDARD_VERSION_METADATA.toString()).build()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(200, STANDARD_VERSION_METADATA.toString()).build()))
                .thenReturn(CompletableFuture.completedFuture(
                        dummyResponseBuilder(500, "", "Server Error").build()));

        var response = testee.discoverConnectors(connectorDiscoveryRequest).join();

        var returnedData = response.getValuesAs(JsonObject.class);

        assertThat(returnedData.size()).isEqualTo(3);
        assertThat(returnedData).containsExactly(expectedJson1, expectedJson2, expectedJson3);

        verify(didResolver, times(1)).resolve(TEST_DID);
        verify(httpClient, times(4)).executeAsync(any(), any());
        verify(monitor, times(1)).severe(eq("Exception during connector discovery, omit endpoint result"), any());
    }

    @ParameterizedTest
    @ArgumentsSource(ConnectorRequestDataProvider.class)
    void discoverConnectors_shouldFail_whenCounterPartyIdIsNotAsExpected(String counterPartyId, String expectedMessage) throws IOException {
        var connectorDiscoveryRequest = new ConnectorDiscoveryRequest(counterPartyId, emptyList());

        assertThatThrownBy(() -> {
            testee.discoverConnectors(connectorDiscoveryRequest).join();
        }).isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(expectedMessage);
    }

    private static class ConnectorRequestDataProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    of(TEST_NON_DID_IDENTIFIER, "The counterparty id has to be a did"),
                    of(null, "Input data must not be empty"),
                    of("  \n", "Input data must not be empty")
            );
        }
    }

    @Test
    void discoverConnectors_shouldFail_whenDidDocumentCannotBeRetrieved() throws IOException {
        var connectorDiscoveryRequest = new ConnectorDiscoveryRequest(TEST_DID, emptyList());

        when(didResolver.resolve(any())).thenReturn(Result.failure("Did document could not be found"));

        assertThatThrownBy(() -> {
            testee.discoverConnectors(connectorDiscoveryRequest).join();
        }).isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Error, downloading the did");
    }

    @Test
    void discoverConnectors_shouldFail_whenDidDocumentContainsEmptyServiceSection() throws IOException {
        var connectorDiscoveryRequest = new ConnectorDiscoveryRequest(TEST_DID, emptyList());

        DidDocument testDidDocument = DidDocument.Builder.newInstance()
                .id(TEST_DID)
                .service(List.of(
                        new Service(TEST_DID + "c2", "CatalogService", "http://younameit"),
                        new Service(TEST_DID + "cs", "CredentialService", "http://dontcare")))
                .build();

        when(didResolver.resolve(any())).thenReturn(Result.success(testDidDocument));

        assertThatThrownBy(() -> {
            testee.discoverConnectors(connectorDiscoveryRequest).join();
        }).isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("No connector endpoints found for counterPartyId");

        verify(didResolver, times(1)).resolve(TEST_DID);
        verify(httpClient, never()).executeAsync(any(), any());
    }

    static okhttp3.Response.Builder dummyResponseBuilder(int code, String body) {
        return dummyResponseBuilder(code, body, "any");
    }

    static okhttp3.Response.Builder dummyResponseBuilder(int code, String body, String message) {
        return new okhttp3.Response.Builder()
                .code(code)
                .message(message)
                .body(ResponseBody.create(body, MediaType.get("application/json")))
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url(TEST_ADDRESS).build());
    }
}
