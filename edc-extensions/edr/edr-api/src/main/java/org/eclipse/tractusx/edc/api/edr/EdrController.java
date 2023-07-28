/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.api.edr;

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
import org.eclipse.edc.connector.api.management.configuration.transform.ManagementApiTypeTransformerRegistry;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.tractusx.edc.api.edr.dto.NegotiateEdrRequestDto;
import org.eclipse.tractusx.edc.edr.spi.service.EdrService;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.tractusx.edc.edr.spi.types.NegotiateEdrRequest;

import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.AGREEMENT_ID;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.ASSET_ID;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.PROVIDER_ID;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/edrs")
public class EdrController implements EdrApi {

    private final EdrService edrService;
    private final ManagementApiTypeTransformerRegistry transformerRegistry;
    private final JsonLd jsonLdService;

    private Monitor monitor;

    public EdrController(EdrService edrService, JsonLd jsonLdService, ManagementApiTypeTransformerRegistry transformerRegistry) {
        this.edrService = edrService;
        this.jsonLdService = jsonLdService;
        this.transformerRegistry = transformerRegistry;
    }

    @POST
    @Override
    public JsonObject initiateEdrNegotiation(JsonObject requestObject) {
        var edrNegotiationRequest = jsonLdService.expand(requestObject)
                .compose(expanded -> transformerRegistry.transform(expanded, NegotiateEdrRequestDto.class))
                .compose(dto -> transformerRegistry.transform(dto, NegotiateEdrRequest.class))
                .orElseThrow(InvalidRequestException::new);

        var contractNegotiation = edrService.initiateEdrNegotiation(edrNegotiationRequest).orElseThrow(exceptionMapper(NegotiateEdrRequest.class));

        var idResponse = IdResponse.Builder.newInstance()
                .id(contractNegotiation.getId())
                .createdAt(contractNegotiation.getCreatedAt())
                .build();

        return transformerRegistry.transform(idResponse, JsonObject.class)
                .compose(jsonLdService::compact)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @GET
    @Override
    public List<JsonObject> queryEdrs(@QueryParam("assetId") String assetId, @QueryParam("agreementId") String agreementId, @QueryParam("providerId") String providerId) {
        if (assetId == null && agreementId == null) {
            throw new InvalidRequestException("At least one of this query parameter is required [assetId,agreementId]");
        }
        return edrService.findBy(querySpec(assetId, agreementId, providerId))
                .orElseThrow(exceptionMapper(EndpointDataReferenceEntry.class))
                .stream()
                .map(edrCached -> transformerRegistry.transform(edrCached, JsonObject.class)
                        .compose(jsonLdService::compact))
                .peek(this::logIfError)
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    @Override
    public JsonObject getEdr(@PathParam("id") String transferProcessId) {
        var edr = edrService.findByTransferProcessId(transferProcessId).orElseThrow(exceptionMapper(EndpointDataReference.class, transferProcessId));
        return transformerRegistry.transform(edr, DataAddress.class)
                .compose(dataAddress -> transformerRegistry.transform(dataAddress, JsonObject.class))
                .compose(jsonLdService::compact)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @DELETE
    @Path("/{id}")
    @Override
    public void deleteEdr(@PathParam("id") String transferProcessId) {
        edrService.deleteByTransferProcessId(transferProcessId).orElseThrow(exceptionMapper(EndpointDataReference.class, transferProcessId));
    }

    private void logIfError(Result<?> result) {
        result.onFailure(f -> monitor.warning(f.getFailureDetail()));
    }

    private QuerySpec querySpec(String assetId, String agreementId, String providerId) {
        var queryBuilder = QuerySpec.Builder.newInstance();
        if (assetId != null) {
            queryBuilder.filter(fieldFilter(ASSET_ID, assetId));
        }
        if (agreementId != null) {
            queryBuilder.filter(fieldFilter(AGREEMENT_ID, agreementId));
        }
        if (providerId != null) {
            queryBuilder.filter(fieldFilter(PROVIDER_ID, agreementId));
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
