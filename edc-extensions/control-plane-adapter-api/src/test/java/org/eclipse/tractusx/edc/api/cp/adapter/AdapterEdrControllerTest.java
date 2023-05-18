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

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.eclipse.tractusx.edc.api.cp.adapter.dto.NegotiateEdrRequestDto;
import org.eclipse.tractusx.edc.spi.cp.adapter.model.NegotiateEdrRequest;
import org.eclipse.tractusx.edc.spi.cp.adapter.service.AdapterTransferProcessService;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.eclipse.edc.api.model.IdResponseDto.EDC_ID_RESPONSE_DTO_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.tractusx.edc.api.cp.adapter.TestFunctions.openRequest;
import static org.eclipse.tractusx.edc.api.cp.adapter.TestFunctions.requestDto;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ApiTest
public class AdapterEdrControllerTest extends RestControllerTestBase {

    public static final String ADAPTER_EDR_PATH = "/adapter/edrs";
    private final JsonLd jsonLdService = new TitaniumJsonLd(monitor);
    AdapterTransferProcessService adapterTransferProcessService = mock(AdapterTransferProcessService.class);
    TypeTransformerRegistry transformerRegistry = mock(TypeTransformerRegistry.class);

    @Test
    void initEdrNegotiation_shouldWork_whenValidRequest() {

        var openRequest = openRequest();
        var contractNegotiation = getContractNegotiation();
        var responseBody = Json.createObjectBuilder().add(TYPE, EDC_ID_RESPONSE_DTO_TYPE).add(ID, contractNegotiation.getId()).build();

        when(transformerRegistry.transform(any(JsonObject.class), eq(NegotiateEdrRequestDto.class))).thenReturn(Result.success(NegotiateEdrRequestDto.Builder.newInstance().build()));
        when(transformerRegistry.transform(any(), eq(NegotiateEdrRequest.class))).thenReturn(Result.success(openRequest));
        when(adapterTransferProcessService.initiateEdrNegotiation(openRequest)).thenReturn(ServiceResult.success(contractNegotiation));
        when(transformerRegistry.transform(any(IdResponseDto.class), eq(JsonObject.class))).thenReturn(Result.success(responseBody));
        var request = requestDto();

        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(ADAPTER_EDR_PATH)
                .then()
                .statusCode(200)
                .body(ID, is(contractNegotiation.getId()));

    }

    @Test
    void initEdrNegotiation_shouldReturnBadRequest_whenValidInvalidRequest() {

        var request = NegotiateEdrRequestDto.Builder.newInstance().build();
        when(transformerRegistry.transform(any(JsonObject.class), eq(NegotiateEdrRequestDto.class))).thenReturn(Result.failure("fail"));

        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(ADAPTER_EDR_PATH)
                .then()
                .statusCode(400);

    }

    @Override
    protected Object controller() {
        return new AdapterEdrController(adapterTransferProcessService, jsonLdService, transformerRegistry);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/")
                .when();
    }

    private ContractNegotiation getContractNegotiation() {
        return ContractNegotiation.Builder.newInstance()
                .id("id")
                .counterPartyAddress("http://test")
                .counterPartyId("provider")
                .protocol("protocol")
                .build();
    }
}
