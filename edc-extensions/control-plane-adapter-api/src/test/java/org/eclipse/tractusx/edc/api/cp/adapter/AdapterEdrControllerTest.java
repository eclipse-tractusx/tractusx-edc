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
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.eclipse.tractusx.edc.api.cp.adapter.dto.NegotiateEdrRequestDto;
import org.eclipse.tractusx.edc.edr.spi.EndpointDataReferenceEntry;
import org.eclipse.tractusx.edc.spi.cp.adapter.model.NegotiateEdrRequest;
import org.eclipse.tractusx.edc.spi.cp.adapter.service.AdapterTransferProcessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static org.eclipse.edc.api.model.IdResponseDto.EDC_ID_RESPONSE_DTO_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.api.cp.adapter.TestFunctions.openRequest;
import static org.eclipse.tractusx.edc.api.cp.adapter.TestFunctions.requestDto;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.EndpointDataReferenceEntry.EDR_ENTRY_AGREEMENT_ID;
import static org.eclipse.tractusx.edc.edr.spi.EndpointDataReferenceEntry.EDR_ENTRY_ASSET_ID;
import static org.eclipse.tractusx.edc.edr.spi.EndpointDataReferenceEntry.EDR_ENTRY_TRANSFER_PROCESS_ID;
import static org.eclipse.tractusx.edc.edr.spi.EndpointDataReferenceEntry.EDR_ENTRY_TYPE;
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

    @BeforeEach
    void setup() {
        jsonLdService.registerNamespace("edc", EDC_NAMESPACE);
        jsonLdService.registerNamespace("tx", TX_NAMESPACE);
    }

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

    @Test
    void initEdrNegotiation_shouldReturnError_whenNotFound() {
        var transferProcessId = "id";

        when(adapterTransferProcessService.findByTransferProcessId(transferProcessId)).thenReturn(ServiceResult.notFound(""));

        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .get(ADAPTER_EDR_PATH + "/" + transferProcessId)
                .then()
                .statusCode(404);
    }

    @Test
    void getEdr_shouldReturnDataAddress_whenFound() {
        var transferProcessId = "id";
        var edr = EndpointDataReference.Builder.newInstance().endpoint("test").id(transferProcessId).build();
        var response = Json.createObjectBuilder()
                .add(DataAddress.TYPE, EndpointDataReference.EDR_SIMPLE_TYPE)
                .add(EndpointDataReference.ENDPOINT, edr.getEndpoint())
                .add(EndpointDataReference.ID, edr.getId())
                .build();

        when(adapterTransferProcessService.findByTransferProcessId(transferProcessId)).thenReturn(ServiceResult.success(edr));
        when(transformerRegistry.transform(any(DataAddress.class), eq(JsonObject.class))).thenReturn(Result.success(response));

        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .get(ADAPTER_EDR_PATH + "/" + transferProcessId)
                .then()
                .statusCode(200)
                .body("'edc:endpoint'", is(edr.getEndpoint()))
                .body("'edc:id'", is(edr.getId()))
                .body("'edc:type'", is(EndpointDataReference.EDR_SIMPLE_TYPE));

    }

    @Test
    void queryEdrs_shouldReturnCachedEntries_whenAssetIdIsProvided() {
        var assetId = "id";
        var transferProcessId = "id";
        var agreementId = "id";

        var entry = EndpointDataReferenceEntry.Builder.newInstance()
                .transferProcessId(transferProcessId)
                .agreementId(agreementId)
                .assetId(assetId)
                .build();

        var response = Json.createObjectBuilder()
                .add(TYPE, EDR_ENTRY_TYPE)
                .add(EDR_ENTRY_ASSET_ID, entry.getAssetId())
                .add(EDR_ENTRY_TRANSFER_PROCESS_ID, entry.getTransferProcessId())
                .add(EDR_ENTRY_AGREEMENT_ID, entry.getAgreementId())
                .build();
        
        when(adapterTransferProcessService.findByAssetAndAgreement(assetId, null)).thenReturn(ServiceResult.success(List.of(entry)));
        when(transformerRegistry.transform(any(EndpointDataReferenceEntry.class), eq(JsonObject.class))).thenReturn(Result.success(response));

        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .get(ADAPTER_EDR_PATH + format("?=assetId=%s", assetId))
                .then()
                .statusCode(200)
                .body("[0].'edc:transferProcessId'", is(entry.getTransferProcessId()))
                .body("[0].'edc:agreementId'", is(entry.getAgreementId()))
                .body("[0].'edc:assetId'", is(entry.getAssetId()));

    }

    @Test
    void queryEdrs_shouldReturnCachedEntries_whenAgreementIdIsProvided() {
        var assetId = "id";
        var transferProcessId = "id";
        var agreementId = "id";

        var entry = EndpointDataReferenceEntry.Builder.newInstance()
                .transferProcessId(transferProcessId)
                .agreementId(agreementId)
                .assetId(assetId)
                .build();


        var response = Json.createObjectBuilder()
                .add(TYPE, EDR_ENTRY_TYPE)
                .add(EDR_ENTRY_ASSET_ID, entry.getAssetId())
                .add(EDR_ENTRY_TRANSFER_PROCESS_ID, entry.getTransferProcessId())
                .add(EDR_ENTRY_AGREEMENT_ID, entry.getAgreementId())
                .build();


        when(adapterTransferProcessService.findByAssetAndAgreement(null, agreementId)).thenReturn(ServiceResult.success(List.of(entry)));
        when(transformerRegistry.transform(any(EndpointDataReferenceEntry.class), eq(JsonObject.class))).thenReturn(Result.success(response));

        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .get(ADAPTER_EDR_PATH + format("?=agreementId=%s", entry.getAgreementId()))
                .then()
                .statusCode(200)
                .body("[0].'edc:transferProcessId'", is(entry.getTransferProcessId()))
                .body("[0].'edc:agreementId'", is(entry.getAgreementId()))
                .body("[0].'edc:assetId'", is(entry.getAssetId()));
    }

    @Test
    void queryEdrs_shouldFail_whenNoQueryParameter() {
        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .get(ADAPTER_EDR_PATH)
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
