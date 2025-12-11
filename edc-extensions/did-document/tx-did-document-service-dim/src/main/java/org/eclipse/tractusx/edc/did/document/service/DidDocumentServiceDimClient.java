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
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.tractusx.edc.iam.dcp.sts.dim.oauth.DimOauth2Client;
import org.eclipse.tractusx.edc.spi.did.document.service.DidDocumentServiceClient;

import java.io.IOException;
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
 */
public class DidDocumentServiceDimClient implements DidDocumentServiceClient {

    public static final MediaType TYPE_JSON = MediaType.parse("application/json");
    private static final String DID_DOC_API_PATH = "/api/v2.0.0/companyIdentities";

    private final DidResolverRegistry resolverRegistry;
    private final EdcHttpClient httpClient;
    private final DimOauth2Client dimOauth2Client;
    private final ObjectMapper mapper;
    private final String ownDid;
    private final Monitor monitor;
    private final String didDocApiUrl;
    private final AtomicReference<String> companyIdentity = new AtomicReference<>();

    public DidDocumentServiceDimClient(DidResolverRegistry resolverRegistry, EdcHttpClient httpClient,
                                       DimOauth2Client dimOauth2Client, ObjectMapper mapper, String dimUrl, String ownDid, Monitor monitor) {
        this.resolverRegistry = resolverRegistry;
        this.httpClient = httpClient;
        this.dimOauth2Client = dimOauth2Client;
        this.mapper = mapper;
        this.ownDid = ownDid;
        this.monitor = monitor.withPrefix(getClass().getSimpleName());
        this.didDocApiUrl = "%s/%s".formatted(dimUrl, DID_DOC_API_PATH);
    }

    /**
     * Creates a new service entry in the DID Document.
     * It requires two API calls: one to add the service and another to update the patch status.
     *
     * @param service the service to create
     * @return a ServiceResult indicating success or failure
     */
    @Override
    public ServiceResult<Void> create(Service service) {

        var existingService = this.getById(service.getId());
        if (existingService.succeeded()) {
            return ServiceResult.conflict("%s already exists".formatted(asString(existingService.getContent())));
        }
        return createServiceEntry(service)
                .compose(v -> updatePatchStatus())
                .onSuccess(v -> monitor.info("Created service entry %s in DID Document".formatted(asString(service))))
                .onFailure(f -> monitor.warning("Failed to create service entry %s with failure %s".formatted(asString(service), f.getFailureDetail())));
    }

    @Override
    public ServiceResult<Void> update(Service service) {
        var existingService = this.getById(service.getId());
        if (existingService.failed()) {
            return create(service);
        }

        if (isEquals(service, existingService.getContent())) {
            // no need to update, same entry already exists
            return ServiceResult.success();
        } else {
            return deleteById(service.getId())
                    .compose(v -> createServiceEntry(service))
                    .compose(v -> updatePatchStatus())
                    .onSuccess(v -> monitor.info("Updated service entry %s in DID Document".formatted(asString(service))))
                    .onFailure(f -> monitor.warning("Failed to update service entry %s with failure %s".formatted(asString(service), f.getFailureDetail())));
        }
    }

    @Override
    public ServiceResult<Service> getById(String id) {
        return findAll()
                .compose(services -> services.stream()
                        .filter(service -> service.getId().equals(id))
                        .findFirst()
                        .map(ServiceResult::success)
                        .orElse(ServiceResult.notFound("Service with id %s not found".formatted(id))));
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
        var existingService = this.getById(id);
        if (existingService.failed()) {
            return ServiceResult.notFound("Service with id %s not found".formatted(id));
        }
        return deleteServiceEntry(id)
                .compose(v -> updatePatchStatus())
                .onSuccess(v -> monitor.info("Deleted service entry %s in DID Document".formatted(id)))
                .onFailure(f -> monitor.warning("Failed to delete service entry %s with failure %s".formatted(id, f.getFailureDetail())));
    }

    @Override
    public ServiceResult<List<Service>> findAll() {
        return ServiceResult.from(resolverRegistry.resolve(ownDid).map(DidDocument::getService));
    }

    private ServiceResult<Void> createServiceEntry(Service service) {

        return resolveCompanyIdentity()
                .map(this::companyIdentityUrl)
                .compose(url -> patchRequest(didDocCreateServicePayload(service), url))
                .map(Request.Builder::build)
                .compose(request -> this.executeRequest(request, this::handleDidUpdateResponse))
                .flatMap(res -> ServiceResult.success());
    }

    private ServiceResult<Void> deleteServiceEntry(String id) {
        return resolveCompanyIdentity()
                .map(this::companyIdentityUrl)
                .compose(url -> patchRequest(didDocDeleteServicePayload(id), url))
                .map(Request.Builder::build)
                .compose(request -> this.executeRequest(request, this::handleDidUpdateResponse))
                .flatMap(res -> ServiceResult.success());
    }

    private ServiceResult<Void> updatePatchStatus() {

        return resolveCompanyIdentity()
                .map(companyId -> "%s/status".formatted(companyIdentityUrl(companyId)))
                .compose(url -> patchRequest(null, url))
                .map(Request.Builder::build)
                .compose(request -> this.executeRequest(request, this::handlePatchStatusResponse))
                .flatMap(res -> ServiceResult.success());
    }

    private Result<String> resolveCompanyIdentity() {
        if (companyIdentity.get() == null) {
            var url = HttpUrl.parse(didDocApiUrl).newBuilder().addQueryParameter("$filter", "issuerDID eq %s".formatted(ownDid)).build();
            getRequest()
                    .map(builder -> builder.url(url).build())
                    .compose(request -> this.executeRequest(request, this::handleCompanyIdentityResponse))
                    .onSuccess(companyIdentity::set)
                    .onFailure(f -> monitor.severe("Failed to resolve company identity for DID %s with failure %s".formatted(ownDid, f.getFailureDetail())));
        }
        return Result.success(companyIdentity.get());
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

    /**
     * Handles the response for a DID update request.
     * Package Private visibility for testing.
     *
     * @param response the HTTP response
     * @return a Result containing the response body as a string if successful, or a failure message
     */
    Result<String> handleDidUpdateResponse(Response response) {
        try {
            var body = Objects.requireNonNull(response.body()).string();
            var parsedBody = mapper.readTree(body);
            return Optional.ofNullable(parsedBody.get("updateDidRequest"))
                    .map(updateDidRequest -> updateDidRequest.get("success").asBoolean())
                    .filter(Boolean.TRUE::equals)
                    .map(success -> Result.success(body))
                    .orElseGet(() -> Result.failure("Failed to Update Did Document, res: %s".formatted(body)));
        } catch (IOException e) {
            monitor.severe("Failed to parse did update response from DIM");
            return Result.failure(e.getMessage());
        }
    }

    /**
     * Handles the response for a patch status request.
     * Package Private visibility for testing.
     *
     * @param response the HTTP response
     * @return a Result containing the response body as a string if successful, or a failure message
     */
    Result<String> handlePatchStatusResponse(Response response) {
        try {
            var body = Objects.requireNonNull(response.body()).string();
            var parsedBody = mapper.readTree(body);
            return Optional.ofNullable(parsedBody.get("status").asText())
                    .filter("successful"::equalsIgnoreCase)
                    .map(success -> Result.success(body))
                    .orElseGet(() -> Result.failure("Failed to Update Patch Status, res: %s".formatted(body)));
        } catch (IOException e) {
            monitor.severe("Failed to parse patch status response from DIM");
            return Result.failure(e.getMessage());
        }
    }

    /**
     * Handles the response for a company identity resolution request.
     * Package Private visibility for testing.
     *
     * @param response the HTTP response
     * @return a Result containing the company identity ID if successful, or a failure message
     */
    Result<String> handleCompanyIdentityResponse(Response response) {
        try {
            var body = Objects.requireNonNull(response.body()).string();
            var parsedBody = mapper.readTree(body);
            return Optional.of(parsedBody)
                    .filter(data -> data.get("count").asInt() == 1)
                    .map(data -> data.get("data").get(0).get("id").asText())
                    .map(Result::success)
                    .orElseGet(() -> Result.failure("Failed to Resolve Company Identity Response, res: %s".formatted(body)));
        } catch (IOException e) {
            monitor.severe("Failed to parse company identity response from DIM");
            return Result.failure(e.getMessage());
        }
    }

    private Result<Request.Builder> baseRequestWithToken() {
        return dimOauth2Client.obtainRequestToken().map(this::baseRequestWithToken);
    }

    private Request.Builder baseRequestWithToken(TokenRepresentation tokenRepresentation) {
        return new Request.Builder().addHeader("Authorization", format("Bearer %s", tokenRepresentation.getToken()));
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
        var createPayload = Map.of("type", service.getType(), "serviceEndpoint", service.getServiceEndpoint());
        return didDocUpdatePayload(Map.of("addServices", List.of(createPayload)));
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

    private Map<String, Object> didDocUpdatePayload(Map<String, Object> operationPayload) {
        return Map.of("didDocUpdates", operationPayload);
    }

    private boolean isEquals(Service service1, Service service2) {

        return Objects.equals(service1.getId(), service2.getId()) &&
               Objects.equals(service1.getType(), service2.getType()) &&
               Objects.equals(service1.getServiceEndpoint(), service2.getServiceEndpoint());
    }

    String asString(Service service) {
        return "Service [id:%s, type:%s, serviceEndpoint=%s]".formatted(service.getId(), service.getType(), service.getServiceEndpoint());
    }

    private String companyIdentityUrl(String companyIdentity) {
        return "%s/%s".formatted(didDocApiUrl, companyIdentity);
    }
}
