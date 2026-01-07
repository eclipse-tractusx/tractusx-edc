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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.tractusx.edc.iam.dcp.sts.dim.oauth.DimOauth2Client;
import org.eclipse.tractusx.edc.spi.did.document.service.DidDocumentServiceClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static java.lang.String.format;
import static org.eclipse.edc.http.spi.FallbackFactories.retryWhenStatusIsNotIn;

/**
 * Implementation of {@link DidDocumentServiceClient} that interacts with a DIM (Decentralized Identity
 * Verification) Service to manage services in a DID Document.
 *
 * Did Document is tied to a company identity in DIM, which is resolved using the own DID. Company identity is needed
 * to perform any updates to the DID Document.
 *
 * <p>
 * Did document update is a two-step process in DIM:
 * 1. A PATCH request is sent to add or remove services.
 * Sample payload to add a service:
 * <pre>
 *  {@code
 * {
 *   "didDocUpdates": {
 *     "addServices": [
 *       {
 *         "id": "did:web:example.com:123#DataService",
 *         "serviceEndpoint": "https://edc.com/edc/.well-known/dspace-version",
 *         "type": "DataService"
 *       }
 *     ]
 *   }
 * }
 * }
 * </pre>
 * 2. A subsequent PATCH request is sent to the /status endpoint to finalize the update.
 *     PATCH {dimUrl}/companyIdentities/{companyIdentityId}/status
 */
public class DidDocumentServiceDimClient implements DidDocumentServiceClient {

    public static final MediaType TYPE_JSON = MediaType.parse("application/json");
    private static final String DID_DOC_API_PATH = "/api/v2.0.0/companyIdentities";

    private final EdcHttpClient httpClient;
    private final DimOauth2Client dimOauth2Client;
    private final ObjectMapper mapper;
    private final String ownDid;
    private final Monitor monitor;
    private final String didDocApiUrl;
    private final AtomicReference<String> companyIdentity = new AtomicReference<>();

    public DidDocumentServiceDimClient(EdcHttpClient httpClient,
                                       DimOauth2Client dimOauth2Client, ObjectMapper mapper, String dimUrl, String ownDid, Monitor monitor) {
        this.httpClient = httpClient;
        this.dimOauth2Client = dimOauth2Client;
        this.mapper = mapper;
        this.ownDid = ownDid;
        this.monitor = monitor.withPrefix(getClass().getSimpleName());
        this.didDocApiUrl = String.join("", dimUrl, DID_DOC_API_PATH);
    }

    @Override
    public ServiceResult<Void> update(Service service) {
        return validateService(service)
                .compose(v -> deleteById(service.getId()))
                .compose(v -> createServiceEntry(service))
                .compose(v -> updatePatchStatus())
                .onSuccess(v -> monitor.info("Updated service entry %s in DID Document".formatted(asString(service))))
                .onFailure(f -> monitor.warning("Failed to update service entry %s with failure %s".formatted(asString(service), f.getFailureDetail())));
    }

    private ServiceResult<Void> validateService(Service service) {
        if (isBlank(service.getServiceEndpoint()) || isBlank(service.getType())) {
            return ServiceResult.unexpected("Validation Failure: Service id, type and serviceEndpoint must be provided and non-blank");
        }
        return validateServiceId(service.getId());
    }

    private ServiceResult<Void> validateServiceId(String serviceId) {

        if (isBlank(serviceId)) {
            return ServiceResult.unexpected("Validation Failure: Service ID must be provided and non-blank");
        }
        try {
            new URI(serviceId);
        } catch (URISyntaxException ex) {
            return ServiceResult.unexpected("Validation Failure: Service ID must be a valid URI: %s'".formatted(serviceId));
        }
        return ServiceResult.success();
    }

    private ServiceResult<Void> createServiceEntry(Service service) {

        return createTenantBaseUrl()
                .compose(url -> patchRequest(didDocCreateServicePayload(service), url))
                .map(Request.Builder::build)
                .compose(request -> this.executeRequest(request, this::handleDidUpdateResponse))
                .flatMap(res -> ServiceResult.success());
    }

    /**
     * Constructs the payload for adding a service to the DID Document.
     * <p>
     * The resulting JSON structure is:
     * <pre>
     *  {@code
     * {
     *   "didDocUpdates": {
     *     "addServices": [
     *       {
     *         "id": "did:web:example.com:123#DataService",
     *         "serviceEndpoint": "https://edc.com/edc/.well-known/dspace-version",
     *         "type": "DataService"
     *       }
     *     ]
     *   }
     * }
     * }
     * </pre>
     *
     * @param service the service to add to the DID Document
     * @return a map representing the payload for the add service operation
     */
    private Map<String, Object> didDocCreateServicePayload(Service service) {
        var createPayload = Map.of("id", service.getId(), "type", service.getType(), "serviceEndpoint", service.getServiceEndpoint());
        return didDocUpdatePayload(Map.of("addServices", List.of(createPayload)));
    }

    /**
     * Handles the response for a DID update request.
     * Package Private visibility for testing.
     * <p>
     * The expected successful response structure is:
     * <pre>
     *  {@code
     *  {
     *   "updateDidRequest": {
     *     "didDocUpdates": {
     *       "removeServices": [
     *         "did:web:example.com:123#DataService"
     *       ]
     *     },
     *     "success": true
     *   }
     * }
     * }
     * </pre>
     *
     * @param response the HTTP response
     * @return a Result containing the response body as a string if successful, or a failure message
     */
    Result<String> handleDidUpdateResponse(Response response) {
        if (!response.isSuccessful()) {
            return Result.failure("DID update request failed with status code: %d, message: %s".formatted(response.code(), response.message()));
        }
        try {
            var body = Objects.requireNonNull(response.body()).string();
            var parsedBody = mapper.readTree(body);
            return Optional.ofNullable(parsedBody.get("updateDidRequest"))
                    .map(updateDidRequest -> updateDidRequest.path("success").asBoolean(false))
                    .filter(Boolean.TRUE::equals)
                    .map(success -> Result.success(body))
                    .orElseGet(() -> Result.failure("Failed to Update Did Document, res: %s".formatted(body)));
        } catch (IOException e) {
            monitor.severe("Failed to parse did update response from DIM", e);
            return Result.failure(e.getMessage());
        }
    }

    /**
     * Deletes a service entry from the DID Document.
     * It requires two API calls: one to remove the service and another to update the patch status.
     *
     * @param id the ID of the service to delete
     * @return a ServiceResult indicating success or failure
     */
    @Override
    public ServiceResult<Void> deleteById(String id) {
        return validateServiceId(id)
                .compose(v -> deleteServiceEntry(id))
                .compose(v -> updatePatchStatus())
                .onSuccess(v -> monitor.info("Deletion of service entry %s in DID Document successful".formatted(id)))
                .onFailure(f -> monitor.severe("Failed to delete service entry %s with failure %s".formatted(id, f.getFailureDetail())));
    }

    private ServiceResult<Void> deleteServiceEntry(String id) {
        return createTenantBaseUrl()
                .compose(url -> patchRequest(didDocDeleteServicePayload(id), url))
                .map(Request.Builder::build)
                .compose(request -> this.executeRequest(request, this::handleDidUpdateResponse))
                .flatMap(res -> ServiceResult.success());
    }

    /**
     * Constructs the payload for removing a service from the DID Document.
     * <p>
     * The resulting JSON structure is:
     * <pre>
     *  {@code
     * {
     *   "didDocUpdates": {
     *     "removeServices": [
     *       "did:web:example.com:123#DataService"
     *     ]
     *   }
     * }
     * }
     * </pre>
     *
     * @param id the ID of the service to remove from the DID Document
     * @return a map representing the payload for the remove service operation
     */
    private Map<String, Object> didDocDeleteServicePayload(String id) {
        return didDocUpdatePayload(Map.of("removeServices", List.of(id)));
    }

    private ServiceResult<Void> updatePatchStatus() {

        return createTenantBaseUrl()
                .map("%s/status"::formatted)
                .compose(url -> patchRequest(Map.of(), url))
                .map(Request.Builder::build)
                .compose(request -> this.executeRequest(request, this::handlePatchStatusResponse))
                .flatMap(res -> ServiceResult.success());
    }

    /**
     * Handles the response for a patch status request.
     * Package Private visibility for testing.
     * <p>
     * The expected successful response structure is:
     * <pre>
     *  {@code
     *  {
     *   "operation": "update",
     *   "status": "successful"
     * }
     * }
     *
     * @param response the HTTP response
     * @return a Result containing the response body as a string if successful, or a failure message
     */
    Result<String> handlePatchStatusResponse(Response response) {
        if (!response.isSuccessful()) {
            return Result.failure("Failed to update patch status request with status code: %d, message %s".formatted(response.code(), response.message()));
        }
        try {
            var body = Objects.requireNonNull(response.body()).string();
            var parsedBody = mapper.readTree(body);
            return Optional.ofNullable(parsedBody.path("status").asText(null))
                    .filter("successful"::equalsIgnoreCase)
                    .map(success -> Result.success(body))
                    .orElseGet(() -> Result.failure("Failed to Update Patch Status, res: %s".formatted(body)));
        } catch (IOException e) {
            monitor.severe("Failed to parse patch status response from DIM", e);
            return Result.failure(e.getMessage());
        }
    }

    private Result<String> createTenantBaseUrl() {
        if (companyIdentity.get() == null) {
            var url = HttpUrl.parse(didDocApiUrl).newBuilder().addQueryParameter("$filter", "issuerDID eq \"%s\"".formatted(ownDid)).build();
            return getRequest()
                    .map(builder -> builder.url(url).build())
                    .compose(request -> this.executeRequest(request, this::handleCompanyIdentityResponse))
                    .onSuccess(companyIdentity::set)
                    .compose(companyId -> Result.success(companyIdentityUrl(companyIdentity.get())))
                    .onFailure(f -> monitor.severe("Failed to resolve company identity for DID %s with failure %s".formatted(ownDid, f.getFailureDetail())));
        }
        return Result.success(companyIdentityUrl(companyIdentity.get()));
    }

    /**
     * Handles the response for a company identity resolution request.
     * Package Private visibility for testing.
     * <p>
     * It is expected that DIM returns exactly one company identity for the given DID.
     * If none or multiple are returned, it is considered a failure.
     * <p>
     * The expected successful response structure is:
     * <pre>
     *  {@code
     *  {
     *   "count": 1,
     *   "data": [
     *     {
     *       "id": "ddfdcbad-44b2-43b5-b49f-6347ec2e586a",
     *       "issuerDID": "did:web:example.com:ABC123",
     *       "isPrivate": false,
     *       "name": "ABC123",
     *       "lastOperationStatus": {
     *         "lastChanged": "2025-12-09T10:28:27.828Z",
     *         "operation": "update",
     *         "status": "successful"
     *       },
     *       "allOperationStatuses": [],
     *       "downloadURL": "https://div.example.com/did-document/91f6954d-b3c8-474a-ad97-59b52cff1f60/did-web/bf618a73df14b6da49c41215fcd920516ad2dbab6922568dc78c59100ec98d9b",
     *       "application": [
     *         "provider"
     *       ],
     *       "isSelfHosted": true
     *     }
     *   ]
     * }
     * }
     *
     * @param response the HTTP response
     * @return a Result containing the company identity ID if successful, or a failure message
     */
    Result<String> handleCompanyIdentityResponse(Response response) {
        if (!response.isSuccessful()) {
            return Result.failure("Company identity resolution request failed with status code: %d, message: %s".formatted(response.code(), response.message()));
        }
        try {
            var body = Objects.requireNonNull(response.body()).string();
            var parsedBody = mapper.readTree(body);
            return Optional.of(parsedBody)
                    .filter(data -> data.path("count").asInt(0) == 1)
                    .map(data -> data.path("data").path(0).path("id").asText(null))
                    .map(Result::success)
                    .orElseGet(() -> Result.failure("Failed to Resolve Company Identity Response, res: %s".formatted(body)));
        } catch (IOException e) {
            monitor.severe("Failed to parse company identity response from DIM", e);
            return Result.failure(e.getMessage());
        }
    }

    private Result<Request.Builder> patchRequest(Map<String, Object> body, String url) {
        try {
            var requestBody = RequestBody.create(mapper.writeValueAsString(body), TYPE_JSON);
            return baseRequestWithToken().map(builder -> builder.patch(requestBody).url(url));
        } catch (JsonProcessingException e) {
            return Result.failure(e.getMessage());
        }
    }

    private Result<Request.Builder> getRequest() {
        return baseRequestWithToken().map(Request.Builder::get);
    }

    private Result<String> executeRequest(Request request, Function<Response, Result<String>> responseMapping) {
        return httpClient.execute(request, List.of(retryWhenStatusIsNotIn(200, 201)), responseMapping);
    }

    private Result<Request.Builder> baseRequestWithToken() {
        return dimOauth2Client.obtainRequestToken().map(this::baseRequestWithToken);
    }

    private Request.Builder baseRequestWithToken(TokenRepresentation tokenRepresentation) {
        return new Request.Builder().addHeader("Authorization", format("Bearer %s", tokenRepresentation.getToken()));
    }

    private Map<String, Object> didDocUpdatePayload(Map<String, Object> operationPayload) {
        return Map.of("didDocUpdates", operationPayload);
    }

    String asString(Service service) {
        return "Service [id:%s, type:%s, serviceEndpoint=%s]".formatted(service.getId(), service.getType(), service.getServiceEndpoint());
    }

    private String companyIdentityUrl(String companyIdentity) {
        return "%s/%s".formatted(didDocApiUrl, companyIdentity);
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
