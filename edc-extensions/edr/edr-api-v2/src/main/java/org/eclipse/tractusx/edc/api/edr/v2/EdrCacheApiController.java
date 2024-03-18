/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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

package org.eclipse.tractusx.edc.api.edr.v2;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;
import org.eclipse.tractusx.edc.api.edr.v2.dto.NegotiateEdrRequestDto;
import org.eclipse.tractusx.edc.edr.spi.service.EdrService;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.tractusx.edc.edr.spi.types.NegotiateEdrRequest;
import org.eclipse.tractusx.edc.spi.tokenrefresh.common.TokenRefreshHandler;

import java.time.Instant;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE;
import static org.eclipse.edc.spi.result.ServiceResult.success;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_AUTH_NS;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/v2/edrs")
public class EdrCacheApiController implements EdrCacheApi {
    private final EndpointDataReferenceStore edrStore;
    private final TypeTransformerRegistry transformerRegistry;
    private final JsonObjectValidatorRegistry validator;
    private final Monitor monitor;
    private final EdrService edrService;
    private final TokenRefreshHandler tokenRefreshHandler;

    public EdrCacheApiController(EndpointDataReferenceStore edrStore,
                                 TypeTransformerRegistry transformerRegistry,
                                 JsonObjectValidatorRegistry validator,
                                 Monitor monitor,
                                 EdrService edrService,
                                 TokenRefreshHandler tokenRefreshHandler) {
        this.edrStore = edrStore;
        this.transformerRegistry = transformerRegistry;
        this.validator = validator;
        this.monitor = monitor;
        this.edrService = edrService;
        this.tokenRefreshHandler = tokenRefreshHandler;
    }

    @POST
    @Override
    public JsonObject initiateEdrNegotiation(JsonObject requestObject) {
        validator.validate(NegotiateEdrRequestDto.EDR_REQUEST_DTO_TYPE, requestObject).orElseThrow(ValidationFailureException::new);

        var edrNegotiationRequest = transformerRegistry.transform(requestObject, NegotiateEdrRequestDto.class)
                .compose(dto -> transformerRegistry.transform(dto, NegotiateEdrRequest.class))
                .orElseThrow(InvalidRequestException::new);

        var contractNegotiation = edrService.initiateEdrNegotiation(edrNegotiationRequest).orElseThrow(exceptionMapper(NegotiateEdrRequest.class));

        var idResponse = IdResponse.Builder.newInstance()
                .id(contractNegotiation.getId())
                .createdAt(contractNegotiation.getCreatedAt())
                .build();

        return transformerRegistry.transform(idResponse, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @POST
    @Path("/request")
    @Override
    public JsonArray requestEdrEntries(JsonObject querySpecJson) {
        QuerySpec querySpec;
        if (querySpecJson == null) {
            querySpec = QuerySpec.Builder.newInstance().build();
        } else {
            validator.validate(EDC_QUERY_SPEC_TYPE, querySpecJson).orElseThrow(ValidationFailureException::new);

            querySpec = transformerRegistry.transform(querySpecJson, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);
        }

        return edrStore.query(querySpec)
                .flatMap(ServiceResult::from)
                .orElseThrow(exceptionMapper(QuerySpec.class, null)).stream()
                .map(it -> transformerRegistry.transform(it, JsonObject.class))
                .peek(r -> r.onFailure(f -> monitor.warning(f.getFailureDetail())))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }

    @GET
    @Path("{transferProcessId}/dataaddress")
    @Override
    public JsonObject getEdrEntryDataAddress(@PathParam("transferProcessId") String transferProcessId, @QueryParam("auto_refresh") boolean autoRefresh) {

        var dataAddress = edrStore.resolveByTransferProcess(transferProcessId)
                .flatMap(ServiceResult::from)
                .compose(edr -> autoRefresh ? refreshAndUpdateToken(edr, transferProcessId) : success(edr))
                .orElseThrow(exceptionMapper(EndpointDataReferenceEntry.class, transferProcessId));

        return transformerRegistry.transform(dataAddress, JsonObject.class)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));


    }

    @DELETE
    @Path("{transferProcessId}")
    @Override
    public void removeEdrEntry(@PathParam("transferProcessId") String transferProcessId) {
        edrStore.delete(transferProcessId)
                .flatMap(ServiceResult::from)
                .orElseThrow(exceptionMapper(EndpointDataReferenceEntry.class, transferProcessId));
    }

    @POST
    @Path("{transferProcessId}/refresh")
    @Override
    public JsonObject refreshEdr(@PathParam("transferProcessId") String transferProcessId) {
        var updatedEdr = tokenRefreshHandler.refreshToken(transferProcessId)
                .orElseThrow(exceptionMapper(DataAddress.class, transferProcessId));

        return transformerRegistry.transform(updatedEdr, JsonObject.class)
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    // todo: move this method into a service once the "old" EDR api,service,etc. is removed
    private ServiceResult<DataAddress> refreshAndUpdateToken(DataAddress edr, String id) {

        var edrEntry = edrStore.findById(id);
        if (edrEntry == null) {
            return ServiceResult.notFound("An EndpointDataReferenceEntry with ID '%s' does not exist".formatted(id));
        }

        if (isExpired(edr, edrEntry)) {
            monitor.debug("Token expired, need to refresh.");
            return tokenRefreshHandler.refreshToken(id, edr);
        }
        return ServiceResult.success(edr);
    }

    // todo: move this method into a service once the "old" EDR api,service,etc. is removed
    private boolean isExpired(DataAddress edr, org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry metadata) {
        var expiresInString = edr.getStringProperty(TX_AUTH_NS + "expiresIn");
        if (expiresInString == null) {
            return true;
        }

        var expiresIn = Long.parseLong(expiresInString);
        // createdAt is in millis, expires-in is in seconds
        var expiresAt = metadata.getCreatedAt() / 1000L + expiresIn;
        var expiresAtInstant = Instant.ofEpochSecond(expiresAt);

        return expiresAtInstant.isBefore(Instant.now());
    }

}
