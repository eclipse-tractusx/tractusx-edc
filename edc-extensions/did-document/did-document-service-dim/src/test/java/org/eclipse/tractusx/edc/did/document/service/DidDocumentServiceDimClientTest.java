/********************************************************************************
 * Copyright (c) 2025 SAP SE
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
 ********************************************************************************/

package org.eclipse.tractusx.edc.did.document.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.tractusx.edc.iam.dcp.sts.dim.oauth.DimOauth2Client;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DidDocumentServiceDimClientTest {

    private static final String COMPANY_ID = "ddfdcbad-44b2-43b5-b49f-6347ec2e586a";

    private static final String DATA_SERVICE_ID = "did:web:example.com:123#DataService";
    private static final String DATA_SERVICE_TYPE = "DataService";
    private static final String DATA_SERVICE_ENDPOINT = "https://edc.com/edc/.well-known/dspace-version";

    private final EdcHttpClient httpClient = mock(EdcHttpClient.class);
    private final DimOauth2Client dimOauth2Client = mock(DimOauth2Client.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final Monitor monitor = mock(Monitor.class);
    private final String dimUrl = "https://div.example.com";
    private final String didDocApiUrl = String.join("", dimUrl, "/api/v2.0.0/companyIdentities");
    private final String tenantBaseUrl = String.join("/", didDocApiUrl, COMPANY_ID);
    private final String didUpdateStatusUrl = String.join("", tenantBaseUrl + "/status");
    private final String ownDid = "did:web:example.com:123";

    private DidDocumentServiceDimClient client;

    @BeforeEach
    void setUp() {
        when(monitor.withPrefix(anyString())).thenReturn(monitor);
        client = new DidDocumentServiceDimClient(httpClient, dimOauth2Client, mapper, dimUrl, ownDid, monitor);
    }

    @Test
    void update_service_success() {

        when(dimOauth2Client.obtainRequestToken()).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token").build()));
        when(httpClient.execute(any(Request.class), anyList(), any()))
                .thenReturn(Result.success(COMPANY_ID)) // resolve company id
                .thenReturn(Result.success("")) // delete service
                .thenReturn(Result.success("")) // // update patch status
                .thenReturn(Result.success("")) //  create service
                .thenReturn(Result.success("")); // update patch status

        var dataService = new Service(DATA_SERVICE_ID, DATA_SERVICE_TYPE, DATA_SERVICE_ENDPOINT);
        var result = client.update(dataService);

        assertThat(result).isSucceeded();
        var requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(httpClient, times(5)).execute(requestCaptor.capture(), anyList(), any());

        var requests = requestCaptor.getAllValues();

        // assert resolve company id
        var expectedCompanyIdUrl = HttpUrl.parse(didDocApiUrl).newBuilder().addQueryParameter("$filter", "issuerDID eq \"%s\"".formatted(ownDid)).build().toString();
        assertRequest(requests.get(0), "GET", expectedCompanyIdUrl, null);

        // assert delete service
        var expectedDeleteServiceBody = """
                {
                   "didDocUpdates": {
                    "removeServices": [
                      "%s"
                    ]
                  }
                }
                """.formatted(dataService.getId());
        assertRequest(requests.get(1), "PATCH", tenantBaseUrl, expectedDeleteServiceBody);

        // assert delete service patch status
        assertRequest(requests.get(2), "PATCH", didUpdateStatusUrl, "{}");

        // assert create service
        var expectedCreateServiceBody = """
                {
                  "didDocUpdates": {
                    "addServices": [
                      {
                        "id": "%s",
                        "serviceEndpoint": "%s",
                        "type": "%s"
                      }
                    ]
                  }
                }
                """.formatted(dataService.getId(), dataService.getServiceEndpoint(), dataService.getType());
        assertRequest(requests.get(3), "PATCH", tenantBaseUrl, expectedCreateServiceBody);

        // assert create service patch status
        assertRequest(requests.get(4), "PATCH", didUpdateStatusUrl, "{}");
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidServiceProvider.class)
    void update_service_failure(Service service) {

        var result = client.update(service);
        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.getReason()).isEqualTo(ServiceFailure.Reason.UNEXPECTED);
            assertThat(failure.getFailureDetail()).contains("Validation Failure");
        });
    }

    private static class InvalidServiceProvider implements ArgumentsProvider {
        @Override
        public @NotNull Stream<? extends Arguments> provideArguments(@NotNull ParameterDeclarations parameters, @NotNull ExtensionContext extensionContext) {
            return Stream.of(
                    Arguments.of(new Service(null, null, null)),
                    Arguments.of(new Service(DATA_SERVICE_ID, null, null)),
                    Arguments.of(new Service(null, DATA_SERVICE_ID, null)),
                    Arguments.of(new Service(null, null, DATA_SERVICE_ENDPOINT)),
                    Arguments.of(new Service(DATA_SERVICE_ID, DATA_SERVICE_TYPE, null)),
                    Arguments.of(new Service(DATA_SERVICE_ID, null, DATA_SERVICE_ENDPOINT)),
                    Arguments.of(new Service(null, DATA_SERVICE_ID, DATA_SERVICE_ENDPOINT)),
                    Arguments.of(new Service("invalid uri", DATA_SERVICE_TYPE, DATA_SERVICE_ENDPOINT))
            );
        }
    }

    @Test
    void deleteById_success() {
        when(dimOauth2Client.obtainRequestToken()).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token").build()));
        when(httpClient.execute(any(Request.class), anyList(), any()))
                .thenReturn(Result.success(COMPANY_ID)) // resolve company id
                .thenReturn(Result.success("")) // delete service
                .thenReturn(Result.success("")); // // update patch status

        var result = client.deleteById(DATA_SERVICE_ID);

        assertThat(result).isSucceeded();
        var requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(httpClient, times(3)).execute(requestCaptor.capture(), anyList(), any());

        var requests = requestCaptor.getAllValues();

        // assert resolve company id
        var expectedCompanyIdUrl = HttpUrl.parse(didDocApiUrl).newBuilder().addQueryParameter("$filter", "issuerDID eq \"%s\"".formatted(ownDid)).build().toString();
        assertRequest(requests.get(0), "GET", expectedCompanyIdUrl, null);

        // assert delete service
        var expectedDeleteServiceBody = """
                {
                   "didDocUpdates": {
                    "removeServices": [
                      "%s"
                    ]
                  }
                }
                """.formatted(DATA_SERVICE_ID);
        assertRequest(requests.get(1), "PATCH", tenantBaseUrl, expectedDeleteServiceBody);

        // assert delete service patch status
        assertRequest(requests.get(2), "PATCH", didUpdateStatusUrl, "{}");
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @ValueSource(strings = {" ", "invalid uri"})
    void deleteById_failure(String serviceId) {

        var result = client.deleteById(serviceId);
        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.getReason()).isEqualTo(ServiceFailure.Reason.UNEXPECTED);
            assertThat(failure.getFailureDetail()).contains("Validation Failure");
        });
    }

    @Test
    void handleDidUpdateResponse_addService_success() {

        String didUpdateResponse = """
                {
                  "updateDidRequest": {
                    "didDocUpdates": {
                      "addServices": [
                        {
                         "id": "did:web:example.com:123#DataService",
                          "serviceEndpoint": "https://edc.com/edc/.well-known/dspace-version",
                          "type": "DataService"
                        }
                      ]
                    },
                    "success": true
                  }
                }
                """;
        var responseBody = ResponseBody.create(didUpdateResponse, MediaType.parse("application/json"));
        var response = mock(Response.class);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);

        var result = client.handleDidUpdateResponse(response);
        assertThat(result).isSucceeded();
    }

    @Test
    void handleDidUpdateResponse_addService_failure() {

        String didUpdateResponse = """
                {
                  "updateDidRequest": {
                    "didDocUpdates": {
                      "addServices": [
                        {
                          "id": "did:web:example.com:123#DataService",
                          "serviceEndpoint": "https://edc.com/edc/.well-known/dspace-version",
                          "type": "DataService"
                        }
                      ]
                    },
                    "success": false
                  }
                }
                """;
        var responseBody = ResponseBody.create(didUpdateResponse, MediaType.parse("application/json"));
        var response = mock(Response.class);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);

        var result = client.handleDidUpdateResponse(response);
        assertThat(result).isFailed();
    }

    @Test
    void handleDidUpdateResponse_addService_failureStatusCode() {
        var response = mock(Response.class);
        when(response.isSuccessful()).thenReturn(false);

        var result = client.handleDidUpdateResponse(response);
        assertThat(result).isFailed();
    }

    @Test
    void handleDidUpdateResponse_addService_resBodyParseFailure() {
        String didUpdateResponse = "<Invalid Response Body>";
        var responseBody = ResponseBody.create(didUpdateResponse, MediaType.parse("application/json"));
        var response = mock(Response.class);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);

        var result = client.handleDidUpdateResponse(response);
        assertThat(result).isFailed();
    }

    @Test
    void handleDidUpdateResponse_deleteService_success() {

        String didUpdateResponse = """
                {
                   "updateDidRequest": {
                     "didDocUpdates": {
                       "removeServices": [
                          "did:web:example.com:123#DataService"
                       ]
                     },
                     "success": true
                   }
                 }
                """;
        var responseBody = ResponseBody.create(didUpdateResponse, MediaType.parse("application/json"));
        var response = mock(Response.class);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);

        var result = client.handleDidUpdateResponse(response);
        assertThat(result).isSucceeded();
    }

    @Test
    void handleDidUpdateResponse_deleteService_failure() throws IOException {

        String didUpdateResponse = """
                {
                   "updateDidRequest": {
                     "didDocUpdates": {
                       "removeServices": [
                          "did:web:example.com:123#DataService"
                       ]
                     },
                     "success": false
                   }
                 }
                """;
        var responseBody = ResponseBody.create(didUpdateResponse, MediaType.parse("application/json"));
        var response = mock(Response.class);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);

        var result = client.handleDidUpdateResponse(response);
        assertThat(result).isFailed();
    }

    @Test
    void handlePatchStatusResponse_success() throws IOException {

        String didUpdateResponse = """
                {
                  "operation": "update",
                  "status": "successful"
                }
                """;
        var responseBody = ResponseBody.create(didUpdateResponse, MediaType.parse("application/json"));
        var response = mock(Response.class);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);

        var result = client.handlePatchStatusResponse(response);
        assertThat(result).isSucceeded();
    }

    @Test
    void handlePatchStatusResponse_failure() throws IOException {

        String didUpdateResponse = """
                {
                  "operation": "update",
                  "status": "failed"
                }
                """;
        var responseBody = ResponseBody.create(didUpdateResponse, MediaType.parse("application/json"));
        var response = mock(Response.class);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);

        var result = client.handlePatchStatusResponse(response);
        assertThat(result).isFailed();
    }

    @Test
    void handlePatchStatusResponse_failureStatusCode() throws IOException {

        var response = mock(Response.class);
        when(response.isSuccessful()).thenReturn(false);

        var result = client.handlePatchStatusResponse(response);
        assertThat(result).isFailed();
    }

    @Test
    void handlePatchStatusResponse_resBodyParseFailure() {
        String didUpdateResponse = "<Invalid Response Body>";
        var responseBody = ResponseBody.create(didUpdateResponse, MediaType.parse("application/json"));
        var response = mock(Response.class);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);

        var result = client.handlePatchStatusResponse(response);
        assertThat(result).isFailed();
    }

    @Test
    void handleCompanyIdentityResponse_success() throws IOException {

        String didUpdateResponse = """
                {
                  "count": 1,
                  "data": [
                    {
                      "id": "ddfdcbad-44b2-43b5-b49f-6347ec2e586a",
                      "issuerDID": "did:web:example.com:ABC123",
                      "isPrivate": false,
                      "name": "ABC123",
                      "lastOperationStatus": {
                        "lastChanged": "2025-12-09T10:28:27.828Z",
                        "operation": "update",
                        "status": "successful"
                      },
                      "allOperationStatuses": [],
                      "downloadURL": "https://div.example.com/did-document/91f6954d-b3c8-474a-ad97-59b52cff1f60/did-web/bf618a73df14b6da49c41215fcd920516ad2dbab6922568dc78c59100ec98d9b",
                      "application": [
                        "provider"
                      ],
                      "isSelfHosted": true
                    }
                  ]
                }
                """;
        var responseBody = ResponseBody.create(didUpdateResponse, MediaType.parse("application/json"));
        var response = mock(Response.class);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);

        var result = client.handleCompanyIdentityResponse(response);
        assertThat(result).isSucceeded().satisfies(companyId -> assertThat(companyId).isEqualTo(COMPANY_ID));
    }

    @Test
    void handleCompanyIdentityResponse_NoData() throws IOException {

        String didUpdateResponse = """
                {
                   "count": 0,
                   "data": []
                 }
                """;
        var responseBody = ResponseBody.create(didUpdateResponse, MediaType.parse("application/json"));
        var response = mock(Response.class);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);

        var result = client.handleCompanyIdentityResponse(response);
        assertThat(result).isFailed();
    }

    @Test
    void handleCompanyIdentityResponse_failureStatusCode() throws IOException {

        var response = mock(Response.class);
        when(response.isSuccessful()).thenReturn(false);

        var result = client.handleCompanyIdentityResponse(response);
        assertThat(result).isFailed();
    }

    @Test
    void handleCompanyIdentityResponse_MultipleData() throws IOException {

        String didUpdateResponse = """
                {
                  "count": 2,
                  "data": [
                    {
                      "id": "ddfdcbad-44b2-43b5-b49f-6347ec2e586a",
                      "issuerDID": "did:web:example.com:ABC123",
                      "isPrivate": false,
                      "name": "ABC123",
                      "lastOperationStatus": {
                        "lastChanged": "2025-12-09T10:28:27.828Z",
                        "operation": "update",
                        "status": "successful"
                      },
                      "allOperationStatuses": [],
                      "downloadURL": "https://div.example.com/did-document/91f6954d-b3c8-474a-ad97-59b52cff1f60/did-web/bf618a73df14b6da49c41215fcd920516ad2dbab6922568dc78c59100ec98d9b",
                      "application": [
                        "provider"
                      ],
                      "isSelfHosted": true
                    },
                    {
                      "id": "ddfdcbad-44b2-43b5-b49f-6347ec2e586b",
                      "issuerDID": "did:web:example.com:DEF456",
                      "isPrivate": false,
                      "name": "DEF456",
                      "lastOperationStatus": {
                        "lastChanged": "2025-12-09T10:28:27.828Z",
                        "operation": "update",
                        "status": "successful"
                      },
                      "allOperationStatuses": [],
                      "downloadURL": "https://div.example.com/did-document/91f6954d-b3c8-474a-ad97-59b52cff1f60/did-web/bf618a73df14b6da49c41215fcd920516ad2dbab6922568dc78c59100ec98d9b",
                      "application": [
                        "provider"
                      ],
                      "isSelfHosted": true
                    }
                  ]
                }
                """;
        var responseBody = ResponseBody.create(didUpdateResponse, MediaType.parse("application/json"));
        var response = mock(Response.class);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);

        var result = client.handleCompanyIdentityResponse(response);
        assertThat(result).isFailed();
    }

    @Test
    void handleCompanyIdentityResponse_resBodyParseFailure() {
        String didUpdateResponse = "<Invalid Response Body>";
        var responseBody = ResponseBody.create(didUpdateResponse, MediaType.parse("application/json"));
        var response = mock(Response.class);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);

        var result = client.handleCompanyIdentityResponse(response);
        assertThat(result).isFailed();
    }

    private void assertRequest(Request request, String expectedMethod, String expectedUrl, String expectedJsonBody) {

        assertThat(request.url().toString()).isEqualTo(expectedUrl);
        assertThat(request.method()).isEqualTo(expectedMethod);

        try {
            JsonNode expectedJsonNode = expectedJsonBody != null ? mapper.readTree(expectedJsonBody) : null;
            var actualJsonNode = request.body() != null ? mapper.readTree(stringifyRequestBody(request)) : null;
            assertThat(actualJsonNode).isEqualTo(expectedJsonNode);
        } catch (IOException e) {
            throw new RuntimeException("Invalid expectedJsonBody provided", e);
        }
    }

    private String stringifyRequestBody(Request request) {
        try {
            final Request copy = request.newBuilder().build();
            final Buffer buffer = new Buffer();
            if (copy.body() != null) {
                copy.body().writeTo(buffer);
            } else {
                return null;
            }
            return buffer.readUtf8();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
