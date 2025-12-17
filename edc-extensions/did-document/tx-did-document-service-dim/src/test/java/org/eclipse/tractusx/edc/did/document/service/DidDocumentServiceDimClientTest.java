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

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.tractusx.edc.iam.dcp.sts.dim.oauth.DimOauth2Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DidDocumentServiceDimClientTest {

    private static final String COMPANY_ID = "ddfdcbad-44b2-43b5-b49f-6347ec2e586a";

    private static final String DATA_SERVICE_ID = "did:web:example.com:123#DataService";
    private static final String DATA_SERVICE_TYPE = "DataService";
    private static final String DATA_SERVICE_ENDPOINT = "https://edc.com/edc/.well-known/dspace-version";

    private static final String CREDENTIAL_SERVICE_ID = "did:web:example.com:123#CredentialService";
    private static final String CREDENTIAL_SERVICE_TYPE = "CredentialService";
    private static final String CREDENTIAL_SERVICE_ENDPOINT = "https://div.example.com/api/holder";

    private final DidResolverRegistry resolverRegistry = mock(DidResolverRegistry.class);
    private final EdcHttpClient httpClient = mock(EdcHttpClient.class);
    private final DimOauth2Client dimOauth2Client = mock(DimOauth2Client.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final Monitor monitor = mock(Monitor.class);
    private final String dimUrl = "https://div.example.com";
    private final String ownDid = "did:web:example.com:123";

    private DidDocumentServiceDimClient client;

    @BeforeEach
    void setUp() {
        when(monitor.withPrefix(anyString())).thenReturn(monitor);
        client = new DidDocumentServiceDimClient(resolverRegistry, httpClient, dimOauth2Client, mapper, dimUrl, ownDid, monitor);
    }

    @Test
    void create_serviceAlreadyExists() {
        var service = new Service(DATA_SERVICE_ID, DATA_SERVICE_TYPE, DATA_SERVICE_ENDPOINT);
        DidDocument didDocument = DidDocument.Builder.newInstance().service(List.of(service)).build();

        when(resolverRegistry.resolve(ownDid)).thenReturn(Result.success(didDocument));

        var result = client.create(service);

        assertThat(result).isFailed().satisfies(failure -> {
                    assertThat(failure.getReason()).isEqualTo(ServiceFailure.Reason.CONFLICT);
                    assertThat(failure.getMessages().stream().anyMatch(msg -> msg.contains("already exists"))).isTrue();
                }
        );
    }

    @Test
    void create_service_success() {
        var credentialService = new Service(CREDENTIAL_SERVICE_ID, CREDENTIAL_SERVICE_TYPE, CREDENTIAL_SERVICE_ENDPOINT);
        DidDocument didDocument = DidDocument.Builder.newInstance().service(List.of(credentialService)).build();

        when(resolverRegistry.resolve(ownDid)).thenReturn(Result.success(didDocument));
        when(dimOauth2Client.obtainRequestToken()).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token").build()));
        when(httpClient.execute(any(Request.class), anyList(), any()))
                .thenReturn(Result.success(COMPANY_ID)) // resolve company id
                .thenReturn(Result.success("")) // create service
                .thenReturn(Result.success("")); // update patch status

        var dataService = new Service(DATA_SERVICE_ID, DATA_SERVICE_TYPE, DATA_SERVICE_ENDPOINT);
        var result = client.create(dataService);

        assertThat(result).isSucceeded();
        verify(httpClient, times(3)).execute(any(Request.class), anyList(), any());
    }

    @Test
    void update_serviceDoesNotExist() {
        var credentialService = new Service(CREDENTIAL_SERVICE_ID, CREDENTIAL_SERVICE_TYPE, CREDENTIAL_SERVICE_ENDPOINT);
        DidDocument didDocument = DidDocument.Builder.newInstance().service(List.of(credentialService)).build();

        when(resolverRegistry.resolve(ownDid)).thenReturn(Result.success(didDocument));
        when(dimOauth2Client.obtainRequestToken()).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token").build()));
        when(httpClient.execute(any(Request.class), anyList(), any()))
                .thenReturn(Result.success(COMPANY_ID)) // resolve company id
                .thenReturn(Result.success("")) // create service
                .thenReturn(Result.success("")); // update patch status

        var dataService = new Service(DATA_SERVICE_ID, DATA_SERVICE_TYPE, DATA_SERVICE_ENDPOINT);
        var result = client.update(dataService);

        assertThat(result).isSucceeded();
        verify(httpClient, times(3)).execute(any(Request.class), anyList(), any());
    }

    @Test
    void update_serviceAlreadyExists() {
        var dataService = new Service(DATA_SERVICE_ID, DATA_SERVICE_TYPE, DATA_SERVICE_ENDPOINT);
        DidDocument didDocument = DidDocument.Builder.newInstance().service(List.of(dataService)).build();

        when(resolverRegistry.resolve(ownDid)).thenReturn(Result.success(didDocument));
        when(dimOauth2Client.obtainRequestToken()).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token").build()));

        var result = client.update(dataService);

        assertThat(result).isSucceeded();
        verify(httpClient, never()).execute(any(Request.class), anyList(), any());
    }

    @Test
    void update_serviceAlreadyExistsWithDifferentUrl() {
        var oldDataService = new Service(DATA_SERVICE_ID, DATA_SERVICE_TYPE, "https://edc.com/edc/.well-known/dspace-version-old");
        DidDocument didDocument = DidDocument.Builder.newInstance().service(List.of(oldDataService)).build();

        when(resolverRegistry.resolve(ownDid)).thenReturn(Result.success(didDocument));
        when(dimOauth2Client.obtainRequestToken()).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token").build()));
        when(httpClient.execute(any(Request.class), anyList(), any()))
                .thenReturn(Result.success(COMPANY_ID)) // resolve company id
                .thenReturn(Result.success("")) // delete service
                .thenReturn(Result.success("")) // // update patch status
                .thenReturn(Result.success("")) //  create service
                .thenReturn(Result.success("")); // update patch status

        var newDataService = new Service(DATA_SERVICE_ID, DATA_SERVICE_TYPE, DATA_SERVICE_ENDPOINT);
        var result = client.update(newDataService);

        assertThat(result).isSucceeded();
        verify(httpClient, times(5)).execute(any(Request.class), anyList(), any());
    }

    @Test
    void getById_ServiceExists() {
        var dataService = new Service(DATA_SERVICE_ID, DATA_SERVICE_TYPE, DATA_SERVICE_ENDPOINT);
        DidDocument didDocument = DidDocument.Builder.newInstance().service(List.of(dataService)).build();

        when(resolverRegistry.resolve(ownDid)).thenReturn(Result.success(didDocument));

        var result = client.getById(DATA_SERVICE_ID);

        assertThat(result).isSucceeded().satisfies(service -> assertThat(service).isEqualTo(dataService));
    }

    @Test
    void getById_ServiceNotFound() {
        DidDocument didDocument = DidDocument.Builder.newInstance().build();

        when(resolverRegistry.resolve(ownDid)).thenReturn(Result.success(didDocument));

        var result = client.getById(DATA_SERVICE_ID);

        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.getReason()).isEqualTo(ServiceFailure.Reason.NOT_FOUND);
            assertThat(failure.getMessages().stream().anyMatch(msg -> msg.contains("not found"))).isTrue();
        });
    }

    @Test
    void deleteById_ServiceNotFound() {
        DidDocument didDocument = DidDocument.Builder.newInstance().build();

        when(resolverRegistry.resolve(ownDid)).thenReturn(Result.success(didDocument));

        var result = client.deleteById(DATA_SERVICE_ID);

        assertThat(result).isFailed().satisfies(failure -> {
            assertThat(failure.getReason()).isEqualTo(ServiceFailure.Reason.NOT_FOUND);
            assertThat(failure.getMessages().stream().anyMatch(msg -> msg.contains("not found"))).isTrue();
        });
    }

    @Test
    void deleteById_success() {
        var dataService = new Service(DATA_SERVICE_ID, DATA_SERVICE_TYPE, DATA_SERVICE_ENDPOINT);
        DidDocument didDocument = DidDocument.Builder.newInstance().service(List.of(dataService)).build();

        when(resolverRegistry.resolve(ownDid)).thenReturn(Result.success(didDocument));
        when(dimOauth2Client.obtainRequestToken()).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token").build()));
        when(httpClient.execute(any(Request.class), anyList(), any()))
                .thenReturn(Result.success(COMPANY_ID)) // resolve company id
                .thenReturn(Result.success("")) // delete service
                .thenReturn(Result.success("")); // // update patch status

        var result = client.deleteById(DATA_SERVICE_ID);

        assertThat(result).isSucceeded();
        verify(httpClient, times(3)).execute(any(Request.class), anyList(), any());

    }

    @Test
    void findAll_shouldReturnServices_whenDidDocumentResolved() {
        var service = new Service(DATA_SERVICE_ID, DATA_SERVICE_TYPE, DATA_SERVICE_ENDPOINT);
        DidDocument didDocument = DidDocument.Builder.newInstance().service(List.of(service)).build();

        when(resolverRegistry.resolve(ownDid)).thenReturn(Result.success(didDocument));

        var result = client.findAll();

        assertThat(result).isSucceeded().satisfies(services -> assertThat(services).containsExactly(service));
    }

    @Test
    void findAll_shouldReturnEmpty_whenNoServices() {
        DidDocument didDocument = DidDocument.Builder.newInstance().build();

        when(resolverRegistry.resolve(ownDid)).thenReturn(Result.success(didDocument));

        var result = client.findAll();

        assertThat(result).isSucceeded().satisfies(services -> assertThat(services).isEmpty());
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
}
