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
import org.eclipse.tractusx.edc.discovery.v4alpha.service.AggregatedIdentifierMapper;
import org.eclipse.tractusx.edc.discovery.v4alpha.service.BpnMapper;
import org.eclipse.tractusx.edc.discovery.v4alpha.service.ConnectorDiscoveryServiceImpl;
import org.eclipse.tractusx.edc.discovery.v4alpha.service.DidMapper;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.DspVersionToIdentifierMapper;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;

import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_COUNTER_PARTY_ADDRESS;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_COUNTER_PARTY_ID;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_PROTOCOL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class ConnectorDiscoveryServiceImplTest {

    private final BdrsClient bdrsClient = mock();
    private final DidResolverRegistry didResolver = mock();
    private final ObjectMapper mapper = new ObjectMapper().configure(
            com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final EdcHttpClient httpClient = mock();
    private final ConnectorDiscoveryServiceImpl service = new ConnectorDiscoveryServiceImpl(
            didResolver,
            httpClient,
            mapper,
            new AggregatedIdentifierMapper(new DidMapper(), new BpnMapper(bdrsClient)),
            new DspVersionToIdentifierMapper() {},
            Clock.systemDefaultZone(),
            1000 * 60 * 12,
            mock());

    @Test
    void discoverVersionParams_shouldReturnDsp2025_whenDsp2025AvailableAndDidResolvable() throws IOException {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest("someBpnl", "http://any");

        var expectedDid = "did:web:providerdid";

        var mockVersionResponseMock = Json.createObjectBuilder()
                .add("protocolVersions", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("version", "0.8")
                                .add("path", "/")
                                .add("binding", "someBinding"))
                        .add(Json.createObjectBuilder()
                                .add("version", "2025-1")
                                .add("path", "/somePath")
                                .add("binding", "someBinding"))).build();

        var expectedJson = Json.createObjectBuilder()
                .add(CATALOG_REQUEST_COUNTER_PARTY_ID, expectedDid)
                .add(CATALOG_REQUEST_PROTOCOL, "dataspace-protocol-http:2025-1")
                .add(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, "http://any/somePath")
                .build();

        when(bdrsClient.resolveDid(paramsDiscoveryRequest.identifier()))
                .thenReturn(expectedDid);
        when(httpClient.execute(any()))
                .thenReturn(dummyResponseBuilder(200, mockVersionResponseMock.toString()).build());

        var response = service.discoverVersionParams(paramsDiscoveryRequest);

        //        assertThat(response).isSucceeded();
        //        assertThat(response.getContent()).isEqualTo(expectedJson);

    }

    @Test
    void discoverVersionParams_shoudReturnDsp08_whenDidCantBeResolvedAndDsp08Available() throws IOException {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest("someBpnl", "http://any");

        var mockVersionResponseMock = Json.createObjectBuilder()
                .add("protocolVersions", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("version", "v0.8")
                                .add("path", "/")
                                .add("binding", "someBinding"))
                        .add(Json.createObjectBuilder()
                                .add("version", "2025-1")
                                .add("path", "/somePath")
                                .add("binding", "someBinding"))).build();

        when(bdrsClient.resolveDid(paramsDiscoveryRequest.identifier()))
                .thenReturn(null);
        when(httpClient.execute(any()))
                .thenReturn(dummyResponseBuilder(200, mockVersionResponseMock.toString()).build());

        var expectedJsonArray = Json.createObjectBuilder()
                .add(CATALOG_REQUEST_COUNTER_PARTY_ID, "someBpnl")
                .add(CATALOG_REQUEST_PROTOCOL, "dataspace-protocol-http")
                .add(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, "http://any")
                .build();

        var response = service.discoverVersionParams(paramsDiscoveryRequest);
        //        assertThat(response).isSucceeded();
        //        assertThat(response.getContent()).isEqualTo(expectedJsonArray);
    }

    @Test
    void discoverVersionParams_shouldReturnFailure_whenDidNotResolvableAndDsp08NotAvailable() throws IOException {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest("someBpnl", "http://any");

        var mockVersionResponseMock = Json.createObjectBuilder()
                .add("protocolVersions", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("version", "2025-1")
                                .add("path", "/2025-1")
                                .add("binding", "someBinding"))).build();

        when(bdrsClient.resolveDid(paramsDiscoveryRequest.identifier()))
                .thenReturn(null);
        when(httpClient.execute(any()))
                .thenReturn(dummyResponseBuilder(200, mockVersionResponseMock.toString()).build());

        var response = service.discoverVersionParams(paramsDiscoveryRequest);

        //        assertThat(response).isFailed();

    }

    @Test
    void discoverVersionParams_shouldReturnDsp08_whenDsp2025NotAvailableAndDsp08Available() throws IOException {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest("someBpnl", "http://any");

        var expectedDid = "did:web:providerdid";

        var mockVersionResponseMock = Json.createObjectBuilder()
                .add("protocolVersions", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("version", "v0.8")
                                .add("path", "/")
                                .add("binding", "someBinding"))).build();

        when(bdrsClient.resolveDid(paramsDiscoveryRequest.identifier()))
                .thenReturn(expectedDid);
        when(httpClient.execute(any()))
                .thenReturn(dummyResponseBuilder(200, mockVersionResponseMock.toString()).build());

        var expectedJsonArray = Json.createObjectBuilder()
                .add(CATALOG_REQUEST_COUNTER_PARTY_ID, "someBpnl")
                .add(CATALOG_REQUEST_PROTOCOL, "dataspace-protocol-http")
                .add(CATALOG_REQUEST_COUNTER_PARTY_ADDRESS, "http://any")
                .build();

        var response = service.discoverVersionParams(paramsDiscoveryRequest);

        //        assertThat(response).isSucceeded();
        //        assertThat(response.getContent()).isEqualTo(expectedJsonArray);
    }

    @Test
    void discoverVersionParams_shouldReturnFailure_whenMetadataEndpointHasError() throws IOException {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest("someBpnl", "http://any");

        var expectedDid = "did:web:providerdid";

        when(bdrsClient.resolveDid(paramsDiscoveryRequest.identifier()))
                .thenReturn(expectedDid);
        when(httpClient.execute(any()))
                .thenReturn(dummyResponseBuilder(404, "Not Found", "Not Found").build());

        var response = service.discoverVersionParams(paramsDiscoveryRequest);

        //        assertThat(response).isFailed();
        //        assertThat(response.getFailureDetail()).contains("Not Found");
    }

    @Test
    void discoverVersionParams_shouldReturnFailure_whenVersionRequestHasMissingProps() throws IOException {
        var paramsDiscoveryRequest = new ConnectorParamsDiscoveryRequest("someBpnl", "http://any");

        var expectedDid = "did:web:providerdid";

        var mockVersionResponseMock = Json.createObjectBuilder()
                .add("protocolVersions", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("version", "2025-1")
                                .add("binding", "someBinding"))).build();

        when(bdrsClient.resolveDid(paramsDiscoveryRequest.identifier()))
                .thenReturn(expectedDid);
        when(httpClient.execute(any()))
                .thenReturn(dummyResponseBuilder(200, mockVersionResponseMock.toString()).build());

        var response = service.discoverVersionParams(paramsDiscoveryRequest);

        //        assertThat(response).isFailed();
        //        assertThat(response.getFailureDetail()).contains("No valid protocol version found for the counter party.");
    }

    private static okhttp3.Response.Builder dummyResponseBuilder(int code, String body) {
        return dummyResponseBuilder(code, body, "any");
    }

    private static okhttp3.Response.Builder dummyResponseBuilder(int code, String body, String message) {
        return new okhttp3.Response.Builder()
                .code(code)
                .message(message)
                .body(ResponseBody.create(body, MediaType.get("application/json")))
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://any").build());
    }
}