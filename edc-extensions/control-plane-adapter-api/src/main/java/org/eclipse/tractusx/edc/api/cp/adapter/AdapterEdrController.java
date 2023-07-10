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

package org.eclipse.tractusx.edc.api.cp.adapter;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.tractusx.edc.api.cp.adapter.dto.NegotiateEdrRequestDto;
import org.eclipse.tractusx.edc.edr.spi.EndpointDataReferenceEntry;
import org.eclipse.tractusx.edc.spi.cp.adapter.model.NegotiateEdrRequest;
import org.eclipse.tractusx.edc.spi.cp.adapter.service.AdapterTransferProcessService;

import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/edrs")
public class AdapterEdrController implements AdapterEdrApi {

    private final AdapterTransferProcessService adapterTransferProcessService;
    private final TypeTransformerRegistry transformerRegistry;
    private final JsonLd jsonLdService;

    private Monitor monitor;

    public AdapterEdrController(AdapterTransferProcessService adapterTransferProcessService, JsonLd jsonLdService, TypeTransformerRegistry transformerRegistry) {
        this.adapterTransferProcessService = adapterTransferProcessService;
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

        var contractNegotiation = adapterTransferProcessService.initiateEdrNegotiation(edrNegotiationRequest).orElseThrow(exceptionMapper(NegotiateEdrRequest.class));

        var responseDto = IdResponseDto.Builder.newInstance()
                .id(contractNegotiation.getId())
                .createdAt(contractNegotiation.getCreatedAt())
                .build();

        return transformerRegistry.transform(responseDto, JsonObject.class)
                .compose(jsonLdService::compact)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @GET
    @Override
    public List<JsonObject> queryEdrs(@QueryParam("assetId") String assetId, @QueryParam("agreementId") String agreementId) {
        if (assetId == null && agreementId == null) {
            throw new InvalidRequestException("At least one of this query parameter is required [assetId,agreementId]");
        }
        return adapterTransferProcessService.findByAssetAndAgreement(assetId, agreementId)
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
        var edr = adapterTransferProcessService.findByTransferProcessId(transferProcessId).orElseThrow(exceptionMapper(EndpointDataReference.class, transferProcessId));
        return transformerRegistry.transform(edr, DataAddress.class)
                .compose(dataAddress -> transformerRegistry.transform(dataAddress, JsonObject.class))
                .compose(jsonLdService::compact)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    private void logIfError(Result<?> result) {
        result.onFailure(f -> monitor.warning(f.getFailureDetail()));
    }
}
