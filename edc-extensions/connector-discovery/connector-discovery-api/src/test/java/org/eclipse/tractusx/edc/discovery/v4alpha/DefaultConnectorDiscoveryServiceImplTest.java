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
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.tractusx.edc.discovery.v4alpha.service.BaseConnectorDiscoveryServiceImpl;
import org.eclipse.tractusx.edc.discovery.v4alpha.service.DefaultConnectorDiscoveryServiceImpl;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

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

public class DefaultConnectorDiscoveryServiceImplTest {

    private final DidResolverRegistry didResolver = mock();
    private final ObjectMapper mapper = new ObjectMapper();
    private final EdcHttpClient httpClient = mock();
    private final Clock clock = Clock.systemUTC();
    private final Monitor monitor = mock();

    private final ConnectorDiscoveryService testee = new DefaultConnectorDiscoveryServiceImpl(httpClient, didResolver, mapper, new BaseConnectorDiscoveryServiceImpl.CacheConfig(1000, clock), monitor);

    private static final String TEST_DID = "did:web:providerdid";
    private static final String TEST_ADDRESS = "http://example.org/api/dsp";

    private static final String VERSION_PROTOCOL = "dataspace-protocol-http:2025-1";


    @Test
    void discoverVersionParams_shouldReturnDsp2025_whenDsp2025Available() throws IOException {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest(TEST_DID, TEST_ADDRESS);

        var mockVersionResponseMock = Json.createObjectBuilder()
                .add("protocolVersions", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("version", "v0.8")
                                .add("path", "/"))
                        .add(Json.createObjectBuilder()
                                .add("version", "2025-1")
                                .add("path", "/somePath"))).build();

        var expectedJson = Json.createObjectBuilder()
                .add(CATALOG_REQUEST_COUNTER_PARTY_ID, TEST_DID)
                .add(CATALOG_REQUEST_PROTOCOL, VERSION_PROTOCOL)
                .add(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, TEST_ADDRESS + "/somePath")
                .build();

        when(httpClient.executeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(dummyResponseBuilder(200, mockVersionResponseMock.toString()).build()));

        var response = testee.discoverVersionParams(paramsDiscoveryRequest).join();

        assertThat(response).isEqualTo(expectedJson);
    }

    @Test
    void discoverVersionParams_shouldReturnUseCacheDuringSucccess() throws IOException, InterruptedException {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest(TEST_DID, TEST_ADDRESS);

        var mockVersionResponseMock = Json.createObjectBuilder()
                .add("protocolVersions", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("version", "v0.8")
                                .add("path", "/"))
                        .add(Json.createObjectBuilder()
                                .add("version", "2025-1")
                                .add("path", "/somePath"))).build();

        var expectedJson = Json.createObjectBuilder()
                .add(CATALOG_REQUEST_COUNTER_PARTY_ID, TEST_DID)
                .add(CATALOG_REQUEST_PROTOCOL, VERSION_PROTOCOL)
                .add(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, TEST_ADDRESS + "/somePath")
                .build();

        when(httpClient.executeAsync(any(), any()))
                .thenReturn(
                        CompletableFuture.completedFuture(dummyResponseBuilder(200, mockVersionResponseMock.toString()).build()),
                        CompletableFuture.completedFuture(dummyResponseBuilder(200, mockVersionResponseMock.toString()).build()));

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
                        .add(Json.createObjectBuilder()
                                .add("version", "v0.8")
                                .add("path", "/"))).build();

        when(httpClient.executeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(dummyResponseBuilder(200, mockVersionResponseMock.toString()).build()));

        assertThatThrownBy(() -> {
            testee.discoverVersionParams(paramsDiscoveryRequest).join();
        }).isInstanceOf(CompletionException.class)
                .hasMessageContaining("InvalidRequestException")
                .hasMessageContaining("The counterparty does not support any of the expected protocol versions");
    }

    @Test
    void discoverVersionParams_shouldReturnException_whenMetadataEndpointHasError() throws IOException {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest(TEST_DID, TEST_ADDRESS);

        when(httpClient.executeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(dummyResponseBuilder(404, "Not Found", "Not Found").build()));

        assertThatThrownBy(() -> {
            testee.discoverVersionParams(paramsDiscoveryRequest).join();
        }).isInstanceOf(CompletionException.class)
                .hasMessageContaining("BadGatewayException")
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
                .thenReturn(CompletableFuture.completedFuture(dummyResponseBuilder(200, mockVersionResponseMock.toString()).build()));

        assertThatThrownBy(() -> {
            testee.discoverVersionParams(paramsDiscoveryRequest).join();
        }).isInstanceOf(CompletionException.class)
                .hasMessageContaining("InvalidRequestException")
                .hasMessageContaining("The counterparty does not support any of the expected protocol versions");
    }

    @ParameterizedTest
    @ArgumentsSource(ResponseDataProvider.class)
    void discoverVersionParams_shouldReturnException_whenVersionRequestHasWrongProps(String overall, String detailed) throws IOException {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest(TEST_DID, TEST_ADDRESS);

        var mockVersionResponseMock = Json.createObjectBuilder()
                        .add(overall, Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add(detailed, "v0.8")
                                        .add("path", "/"))).build();

        when(httpClient.executeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(dummyResponseBuilder(200, mockVersionResponseMock.toString()).build()));

        assertThatThrownBy(() -> {
            testee.discoverVersionParams(paramsDiscoveryRequest).join();
        }).isInstanceOf(CompletionException.class)
                .hasMessageContaining("BadGatewayException")
                .hasMessageContaining("An exception with the following message occurred while executing dsp version request");
    }

    private static class ResponseDataProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    of("urgs", "version"),
                    of("protocolVersions", "urgs")
            );
        }
    }

    @ParameterizedTest
    @ArgumentsSource(RequestDataProvider.class)
    void discoverVersionParams_shouldReturnException_whenNoDidIsUsed(String id, String address, Class exceptionType, String exceptionName, String message) throws IOException {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest(id, address);

        var mockVersionResponseMock = Json.createObjectBuilder()
                .add("protocolVersions", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("version", "v0.8")
                                .add("path", "/"))
                        .add(Json.createObjectBuilder()
                                .add("version", "2025-1")
                                .add("path", "/somePath"))).build();

        when(httpClient.executeAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(dummyResponseBuilder(200, mockVersionResponseMock.toString()).build()));

        assertThatThrownBy(() -> {
            testee.discoverVersionParams(paramsDiscoveryRequest).join();
        }).isInstanceOf(exceptionType)
                .hasMessageContaining(exceptionName != null ? exceptionName : "")
                .hasMessageContaining(message);
    }

    private static class RequestDataProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    of("BPNL1234567890AB", TEST_ADDRESS, CompletionException.class, "InvalidRequestException", "CounterPartyId used not supported, must be a did"),
                    of(TEST_DID, "urgs/:123", InvalidRequestException.class, null, "Provided endpoint url of connector cannot be parsed as URL"),
                    of(null, TEST_ADDRESS, InvalidRequestException.class, null, "Input data must not be empty"),
                    of(TEST_DID, null, InvalidRequestException.class, null, "Input data must not be empty"),
                    of("   ", TEST_ADDRESS, InvalidRequestException.class, null, "Input data must not be empty"),
                    of(TEST_DID, " \n", InvalidRequestException.class, null, "Input data must not be empty")
            );
        }
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
