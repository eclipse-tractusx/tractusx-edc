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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Violation;
import org.eclipse.edc.web.jersey.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.eclipse.tractusx.edc.api.edr.legacy.dto.NegotiateEdrRequestDto;
import org.eclipse.tractusx.edc.edr.spi.service.EdrService;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates;
import org.eclipse.tractusx.edc.edr.spi.types.NegotiateEdrRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static org.eclipse.edc.api.model.IdResponse.ID_RESPONSE_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.api.edr.legacy.TestFunctions.negotiationRequest;
import static org.eclipse.tractusx.edc.api.edr.legacy.TestFunctions.openRequest;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.AGREEMENT_ID;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.ASSET_ID;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.CONTRACT_NEGOTIATION_ID;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_AGREEMENT_ID;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_ASSET_ID;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_CONTRACT_NEGOTIATION_ID;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_PROVIDER_ID;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_STATE;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_TRANSFER_PROCESS_ID;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_TYPE;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.PROVIDER_ID;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiTest
public class EdrControllerTest extends RestControllerTestBase {

    public static final String EDR_PATH = "/edrs";
    private final JsonLd jsonLdService = new TitaniumJsonLd(monitor);
    EdrService edrService = mock(EdrService.class);
    TypeTransformerRegistry transformerRegistry = mock();
    JsonObjectValidatorRegistry validatorRegistry = mock();

    @BeforeEach
    void setup() {
        jsonLdService.registerNamespace("edc", EDC_NAMESPACE);
        jsonLdService.registerNamespace("tx", TX_NAMESPACE);
    }

    @Test
    void initEdrNegotiation_shouldWork_whenValidRequest() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());

        var openRequest = openRequest();
        var contractNegotiation = getContractNegotiation();
        var responseBody = Json.createObjectBuilder().add(TYPE, ID_RESPONSE_TYPE).add(ID, contractNegotiation.getId()).build();

        when(transformerRegistry.transform(any(JsonObject.class), eq(NegotiateEdrRequestDto.class))).thenReturn(Result.success(NegotiateEdrRequestDto.Builder.newInstance().build()));
        when(transformerRegistry.transform(any(), eq(NegotiateEdrRequest.class))).thenReturn(Result.success(openRequest));
        when(edrService.initiateEdrNegotiation(openRequest)).thenReturn(ServiceResult.success(contractNegotiation));
        when(transformerRegistry.transform(any(IdResponse.class), eq(JsonObject.class))).thenReturn(Result.success(responseBody));

        var request = negotiationRequest();

        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(EDR_PATH)
                .then()
                .statusCode(200)
                .body(ID, is(contractNegotiation.getId()));

    }

    @Test
    void initEdrNegotiation_shouldReturnBadRequest_whenValidInvalidRequest() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());

        var request = NegotiateEdrRequestDto.Builder.newInstance().build();
        when(transformerRegistry.transform(any(JsonObject.class), eq(NegotiateEdrRequestDto.class))).thenReturn(Result.failure("fail"));

        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .post(EDR_PATH)
                .then()
                .statusCode(400);

    }

    @Test
    void initEdrNegotiation_shouldReturnBadRequest_whenValidationFails() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.failure(Violation.violation("failure", "failure path")));
        var request = negotiationRequest();

        given()
                .port(port)
                .body(request)
                .contentType(MediaType.APPLICATION_JSON)
                .post(EDR_PATH)
                .then()
                .statusCode(400)
                .contentType(MediaType.APPLICATION_JSON);

        verifyNoInteractions(transformerRegistry);
    }

    @Test
    void initEdrNegotiation_shouldReturnError_whenNotFound() {
        var transferProcessId = "id";

        when(edrService.findByTransferProcessId(transferProcessId)).thenReturn(ServiceResult.notFound(""));

        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .get(EDR_PATH + "/" + transferProcessId)
                .then()
                .statusCode(404);
    }

    @Test
    void getEdr_shouldReturnDataAddress_whenFound() {
        var transferProcessId = "id";
        var edr = EndpointDataReference.Builder.newInstance().endpoint("test")
                .contractId("test-contract-id")
                .id(transferProcessId).build();
        var response = Json.createObjectBuilder()
                .add(DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY, EndpointDataReference.EDR_SIMPLE_TYPE)
                .add(EndpointDataReference.ENDPOINT, edr.getEndpoint())
                .add(EndpointDataReference.ID, edr.getId())
                .build();

        var dataAddress = DataAddress.Builder.newInstance().type("HttpData").build();
        when(edrService.findByTransferProcessId(transferProcessId)).thenReturn(ServiceResult.success(edr));
        when(transformerRegistry.transform(any(EndpointDataReference.class), eq(DataAddress.class))).thenReturn(Result.success(dataAddress));
        when(transformerRegistry.transform(any(DataAddress.class), eq(JsonObject.class))).thenReturn(Result.success(response));

        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .get(EDR_PATH + "/" + transferProcessId)
                .then()
                .statusCode(200)
                .body("'edc:endpoint'", is(edr.getEndpoint()))
                .body("'edc:id'", is(edr.getId()))
                .body("'edc:type'", is(EndpointDataReference.EDR_SIMPLE_TYPE));

    }

    @Test
    void queryEdrs_shouldReturnCachedEntries_whenAssetIdIsProvided() {
        var assetId = "assetId";
        var transferProcessId = "transferProcessId";
        var agreementId = "agreementId";
        var providerId = "providerId";


        var entry = EndpointDataReferenceEntry.Builder.newInstance()
                .transferProcessId(transferProcessId)
                .agreementId(agreementId)
                .assetId(assetId)
                .providerId(providerId)
                .state(EndpointDataReferenceEntryStates.NEGOTIATED.code())
                .build();

        var response = Json.createObjectBuilder()
                .add(TYPE, EDR_ENTRY_TYPE)
                .add(EDR_ENTRY_ASSET_ID, entry.getAssetId())
                .add(EDR_ENTRY_TRANSFER_PROCESS_ID, entry.getTransferProcessId())
                .add(EDR_ENTRY_AGREEMENT_ID, entry.getAgreementId())
                .add(EDR_ENTRY_PROVIDER_ID, entry.getProviderId())
                .add(EDR_ENTRY_STATE, entry.getEdrState())
                .build();

        var filter = QuerySpec.Builder.newInstance().filter(fieldFilter(ASSET_ID, assetId)).build();

        when(edrService.findBy(eq(filter))).thenReturn(ServiceResult.success(List.of(entry)));
        when(transformerRegistry.transform(any(EndpointDataReferenceEntry.class), eq(JsonObject.class))).thenReturn(Result.success(response));

        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .get(EDR_PATH + format("?=assetId=%s", assetId))
                .then()
                .statusCode(200)
                .body("[0].'edc:transferProcessId'", is(entry.getTransferProcessId()))
                .body("[0].'edc:agreementId'", is(entry.getAgreementId()))
                .body("[0].'edc:assetId'", is(entry.getAssetId()))
                .body("[0].'edc:providerId'", is(entry.getProviderId()))
                .body("[0].'tx:edrState'", is(entry.getEdrState()));

    }

    @Test
    void queryEdrs_shouldReturnCachedEntries_whenAgreementIdIsProvided() {
        var assetId = "assetId";
        var transferProcessId = "transferProcessId";
        var agreementId = "agreementId";
        var providerId = "providerId";

        var entry = EndpointDataReferenceEntry.Builder.newInstance()
                .transferProcessId(transferProcessId)
                .agreementId(agreementId)
                .assetId(assetId)
                .providerId(providerId)
                .build();


        var response = Json.createObjectBuilder()
                .add(TYPE, EDR_ENTRY_TYPE)
                .add(EDR_ENTRY_ASSET_ID, entry.getAssetId())
                .add(EDR_ENTRY_TRANSFER_PROCESS_ID, entry.getTransferProcessId())
                .add(EDR_ENTRY_AGREEMENT_ID, entry.getAgreementId())
                .add(EDR_ENTRY_PROVIDER_ID, entry.getProviderId())
                .build();

        var filter = QuerySpec.Builder.newInstance()
                .filter(fieldFilter(AGREEMENT_ID, agreementId))
                .filter(fieldFilter(PROVIDER_ID, entry.getProviderId()))
                .build();

        when(edrService.findBy(eq(filter))).thenReturn(ServiceResult.success(List.of(entry)));
        when(transformerRegistry.transform(any(EndpointDataReferenceEntry.class), eq(JsonObject.class))).thenReturn(Result.success(response));

        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .get(EDR_PATH + format("?=agreementId=%s&providerId=%s", entry.getAgreementId(), entry.getProviderId()))
                .then()
                .statusCode(200)
                .body("[0].'edc:transferProcessId'", is(entry.getTransferProcessId()))
                .body("[0].'edc:agreementId'", is(entry.getAgreementId()))
                .body("[0].'edc:assetId'", is(entry.getAssetId()))
                .body("[0].'edc:providerId'", is(entry.getProviderId()));
    }

    @Test
    void queryEdrs_shouldReturnCachedEntries_whenContractNegotiationIdIsProvided() {
        var assetId = "assetId";
        var transferProcessId = "transferProcessId";
        var agreementId = "agreementId";
        var providerId = "providerId";
        var contractNegotiationId = "contractNegotiationId";

        var entry = EndpointDataReferenceEntry.Builder.newInstance()
                .transferProcessId(transferProcessId)
                .agreementId(agreementId)
                .assetId(assetId)
                .providerId(providerId)
                .contractNegotiationId(contractNegotiationId)
                .build();


        var response = Json.createObjectBuilder()
                .add(TYPE, EDR_ENTRY_TYPE)
                .add(EDR_ENTRY_ASSET_ID, entry.getAssetId())
                .add(EDR_ENTRY_TRANSFER_PROCESS_ID, entry.getTransferProcessId())
                .add(EDR_ENTRY_AGREEMENT_ID, entry.getAgreementId())
                .add(EDR_ENTRY_CONTRACT_NEGOTIATION_ID, entry.getContractNegotiationId())
                .add(EDR_ENTRY_PROVIDER_ID, entry.getProviderId())
                .build();

        var filter = QuerySpec.Builder.newInstance()
                .filter(fieldFilter(CONTRACT_NEGOTIATION_ID, contractNegotiationId))
                .filter(fieldFilter(PROVIDER_ID, entry.getProviderId()))
                .build();

        when(edrService.findBy(eq(filter))).thenReturn(ServiceResult.success(List.of(entry)));
        when(transformerRegistry.transform(any(EndpointDataReferenceEntry.class), eq(JsonObject.class))).thenReturn(Result.success(response));

        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .get(EDR_PATH + format("?=contractNegotiationId=%s&providerId=%s", entry.getContractNegotiationId(), entry.getProviderId()))
                .then()
                .log().all(true)
                .statusCode(200)
                .body("[0].'edc:transferProcessId'", is(entry.getTransferProcessId()))
                .body("[0].'edc:agreementId'", is(entry.getAgreementId()))
                .body("[0].'edc:contractNegotiationId'", is(entry.getContractNegotiationId()))
                .body("[0].'edc:assetId'", is(entry.getAssetId()))
                .body("[0].'edc:providerId'", is(entry.getProviderId()));
    }

    @Test
    void deleteEdr() {
        var transferProcessId = "id";

        when(edrService.deleteByTransferProcessId(transferProcessId)).thenReturn(ServiceResult.success(null));

        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .delete(EDR_PATH + "/" + transferProcessId)
                .then()
                .statusCode(204);
    }

    @Test
    void deleteEdr_shouldReturnNotFound_whenNotInCache() {
        var transferProcessId = "id";

        when(edrService.deleteByTransferProcessId(transferProcessId)).thenReturn(ServiceResult.notFound(""));

        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .delete(EDR_PATH + "/" + transferProcessId)
                .then()
                .statusCode(404);
    }

    @Test
    void queryEdrs_shouldFail_whenNoQueryParameter() {
        baseRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .get(EDR_PATH)
                .then()
                .statusCode(400);
    }

    @Override
    protected Object controller() {
        return new EdrController(edrService, transformerRegistry, validatorRegistry, monitor);
    }

    @Override
    protected Object additionalResource() {
        final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
        return new JerseyJsonLdInterceptor(this.jsonLdService, objectMapper, "edr");
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

    private Criterion fieldFilter(String field, String value) {
        return Criterion.Builder.newInstance()
                .operandLeft(field)
                .operator("=")
                .operandRight(value)
                .build();
    }
}
