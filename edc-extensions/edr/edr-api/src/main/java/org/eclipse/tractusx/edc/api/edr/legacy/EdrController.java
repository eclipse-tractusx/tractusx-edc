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

package org.eclipse.tractusx.edc.api.edr.legacy;

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
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;
import org.eclipse.tractusx.edc.api.edr.legacy.dto.NegotiateEdrRequestDto;
import org.eclipse.tractusx.edc.edr.spi.service.EdrService;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.tractusx.edc.edr.spi.types.NegotiateEdrRequest;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;
import static org.eclipse.tractusx.edc.api.edr.legacy.dto.NegotiateEdrRequestDto.EDR_REQUEST_DTO_TYPE;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.AGREEMENT_ID;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.ASSET_ID;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.CONTRACT_NEGOTIATION_ID;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.PROVIDER_ID;

@Deprecated(since = "0.6.0")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/edrs")
public class EdrController implements EdrApi {

    private final EdrService edrService;
    private final TypeTransformerRegistry transformerRegistry;
    private final JsonObjectValidatorRegistry validatorRegistry;
    private final Monitor monitor;

    public EdrController(EdrService edrService, TypeTransformerRegistry transformerRegistry,
                         JsonObjectValidatorRegistry validatorRegistry, Monitor monitor) {
        this.edrService = edrService;
        this.transformerRegistry = transformerRegistry;
        this.validatorRegistry = validatorRegistry;
        this.monitor = monitor;
    }

    @POST
    @Override
    public JsonObject initiateEdrNegotiation(JsonObject requestObject) {
        monitor.warning("The /edrs API is deprecated and will be removed from the code base with Tractus-X EDC 0.7.x. Please consider upgrading to /v2/edrs!");

        validatorRegistry.validate(EDR_REQUEST_DTO_TYPE, requestObject).orElseThrow(ValidationFailureException::new);

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

    @GET
    @Override
    public JsonArray queryEdrs(@QueryParam("assetId") String assetId,
                               @QueryParam("agreementId") String agreementId,
                               @QueryParam("contractNegotiationId") String contractNegotiationId,
                               @QueryParam("providerId") String providerId) {
        monitor.warning("The /edrs API is deprecated and will be removed from the code base with Tractus-X EDC 0.7.x. Please consider upgrading to /v2/edrs!");
        if (assetId == null && agreementId == null && contractNegotiationId == null) {
            throw new InvalidRequestException("At least one of this query parameter is required [assetId, agreementId, contractNegotiationId]");
        }
        return edrService.findBy(querySpec(assetId, agreementId, contractNegotiationId, providerId))
                .orElseThrow(exceptionMapper(EndpointDataReferenceEntry.class))
                .stream()
                .map(edrCached -> transformerRegistry.transform(edrCached, JsonObject.class))
                .peek(this::logIfError)
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }

    @GET
    @Path("/{id}")
    @Override
    public JsonObject getEdr(@PathParam("id") String transferProcessId) {
        monitor.warning("The /edrs API is deprecated and will be removed from the code base with Tractus-X EDC 0.7.x. Please consider upgrading to /v2/edrs!");
        var edr = edrService.findByTransferProcessId(transferProcessId).orElseThrow(exceptionMapper(EndpointDataReference.class, transferProcessId));
        return transformerRegistry.transform(edr, DataAddress.class)
                .compose(dataAddress -> transformerRegistry.transform(dataAddress, JsonObject.class))
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @DELETE
    @Path("/{id}")
    @Override
    public void deleteEdr(@PathParam("id") String transferProcessId) {
        monitor.warning("The /edrs API is deprecated and will be removed from the code base with Tractus-X EDC 0.7.x. Please consider upgrading to /v2/edrs!");
        edrService.deleteByTransferProcessId(transferProcessId).orElseThrow(exceptionMapper(EndpointDataReference.class, transferProcessId));
    }

    private void logIfError(Result<?> result) {
        result.onFailure(f -> monitor.warning(f.getFailureDetail()));
    }

    private QuerySpec querySpec(String assetId, String agreementId, String contractNegotiationId, String providerId) {
        var queryBuilder = QuerySpec.Builder.newInstance();
        if (assetId != null) {
            queryBuilder.filter(fieldFilter(ASSET_ID, assetId));
        }
        if (agreementId != null) {
            queryBuilder.filter(fieldFilter(AGREEMENT_ID, agreementId));
        }
        if (contractNegotiationId != null) {
            queryBuilder.filter(fieldFilter(CONTRACT_NEGOTIATION_ID, contractNegotiationId));
        }
        if (providerId != null) {
            queryBuilder.filter(fieldFilter(PROVIDER_ID, providerId));
        }
        return queryBuilder.build();
    }


    private Criterion fieldFilter(String field, String value) {
        return Criterion.Builder.newInstance()
                .operandLeft(field)
                .operator("=")
                .operandRight(value)
                .build();
    }
}
