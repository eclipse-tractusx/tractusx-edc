/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.api.edr;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.eclipse.tractusx.edc.edr.spi.service.EdrService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static java.util.UUID.randomUUID;
import static org.eclipse.edc.api.model.IdResponse.ID_RESPONSE_TYPE;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_AGREEMENT_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_ASSET_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_CONTRACT_NEGOTIATION_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_PROVIDER_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_TRANSFER_PROCESS_ID;
import static org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.eclipse.tractusx.edc.api.edr.TestFunctions.createContractNegotiation;
import static org.eclipse.tractusx.edc.api.edr.TestFunctions.negotiationRequest;
import static org.eclipse.tractusx.edc.edr.spi.types.RefreshMode.AUTO_REFRESH;
import static org.eclipse.tractusx.edc.edr.spi.types.RefreshMode.NO_REFRESH;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ApiTest
public abstract class BaseEdrCacheApiControllerTest extends RestControllerTestBase {

    private static final String TEST_TRANSFER_PROCESS_ID = "test-transfer-process-id";
    private static final String TEST_TRANSFER_NEGOTIATION_ID = "test-cn-id";
    private static final String TEST_AGREEMENT_ID = "test-agreement-id";
    private static final String TEST_PROVIDER_ID = "test-provider-id";
    private static final String TEST_ASSET_ID = "test-asset-id";

    protected final TypeTransformerRegistry transformerRegistry = mock();
    protected final JsonObjectValidatorRegistry validator = mock();
    protected final EndpointDataReferenceStore edrStore = mock();
    protected final EdrService edrService = mock();
    protected final ContractNegotiationService contractNegotiationService = mock();


    @Test
    void initEdrNegotiation_shouldWork_whenValidRequest() {
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

        var contractNegotiation = createContractNegotiation();
        var responseBody = Json.createObjectBuilder().add(TYPE, ID_RESPONSE_TYPE).add(ID, contractNegotiation.getId()).build();

        when(transformerRegistry.transform(any(JsonObject.class), eq(ContractRequest.class))).thenReturn(Result.success(createContractRequest()));
        when(contractNegotiationService.initiateNegotiation(any())).thenReturn(contractNegotiation);
        when(transformerRegistry.transform(any(IdResponse.class), eq(JsonObject.class))).thenReturn(Result.success(responseBody));

        var request = negotiationRequest();

        baseRequest()
                .contentType(JSON)
                .body(request)
                .post("/edrs")
                .then()
                .statusCode(200)
                .body(ID, is(contractNegotiation.getId()));

    }

    @Test
    void initEdrNegotiation_shouldReturnBadRequest_whenValidInvalidRequest() {
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

        when(transformerRegistry.transform(any(JsonObject.class), eq(ContractRequest.class))).thenReturn(Result.failure("fail"));

        baseRequest()
                .contentType(JSON)
                .body(Json.createObjectBuilder().build())
                .post("/edrs")
                .then()
                .statusCode(400);

    }

    @Test
    void requestEdrEntries() {
        when(edrStore.query(any()))
                .thenReturn(StoreResult.success(List.of(createEdrEntry())));
        when(transformerRegistry.transform(isA(EndpointDataReferenceEntry.class), eq(JsonObject.class)))
                .thenReturn(Result.success(createEdrEntryJson().build()));
        when(transformerRegistry.transform(isA(JsonObject.class), eq(QuerySpec.class)))
                .thenReturn(Result.success(QuerySpec.Builder.newInstance().offset(10).build()));
        when(validator.validate(any(), any())).thenReturn(ValidationResult.success());

        baseRequest()
                .contentType(JSON)
                .body("{}")
                .post("/edrs/request")
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));

        verify(edrStore).query(argThat(s -> s.getOffset() == 10));
        verify(transformerRegistry).transform(isA(EndpointDataReferenceEntry.class), eq(JsonObject.class));
        verify(transformerRegistry).transform(isA(JsonObject.class), eq(QuerySpec.class));
    }

    @Test
    void getEdrEntryDataAddress() {

        var dataAddressType = "type";
        var dataAddress = DataAddress.Builder.newInstance().type(dataAddressType).build();
        when(edrService.resolveByTransferProcess("transferProcessId", AUTO_REFRESH))
                .thenReturn(ServiceResult.success(dataAddress));

        when(transformerRegistry.transform(isA(DataAddress.class), eq(JsonObject.class)))
                .thenReturn(Result.success(createDataAddress(dataAddressType).build()));

        baseRequest()
                .contentType(JSON)
                .get("/edrs/transferProcessId/dataaddress")
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(JSON)
                .body("'%s'".formatted(DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY), equalTo(dataAddressType));

        verify(edrService).resolveByTransferProcess("transferProcessId", AUTO_REFRESH);
        verify(transformerRegistry).transform(isA(DataAddress.class), eq(JsonObject.class));
        verifyNoMoreInteractions(transformerRegistry);
    }

    @Test
    void getEdrEntryDataAddress_withExplicitAutoRefreshTrue() {

        var dataAddressType = "type";
        var dataAddress = DataAddress.Builder.newInstance().type(dataAddressType).build();
        when(edrService.resolveByTransferProcess("transferProcessId", AUTO_REFRESH))
                .thenReturn(ServiceResult.success(dataAddress));

        when(transformerRegistry.transform(isA(DataAddress.class), eq(JsonObject.class)))
                .thenReturn(Result.success(createDataAddress(dataAddressType).build()));

        baseRequest()
                .contentType(JSON)
                .get("/edrs/transferProcessId/dataaddress?auto_refresh=true")
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(JSON)
                .body("'%s'".formatted(DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY), equalTo(dataAddressType));

        verify(edrService).resolveByTransferProcess("transferProcessId", AUTO_REFRESH);
        verify(transformerRegistry).transform(isA(DataAddress.class), eq(JsonObject.class));
        verifyNoMoreInteractions(transformerRegistry);
    }

    @Test
    void getEdrEntryDataAddress_withExplicitAutoRefreshFalse() {

        var dataAddressType = "type";
        var dataAddress = DataAddress.Builder.newInstance().type(dataAddressType).build();
        when(edrService.resolveByTransferProcess("transferProcessId", NO_REFRESH))
                .thenReturn(ServiceResult.success(dataAddress));

        when(transformerRegistry.transform(isA(DataAddress.class), eq(JsonObject.class)))
                .thenReturn(Result.success(createDataAddress(dataAddressType).build()));

        baseRequest()
                .contentType(JSON)
                .get("/edrs/transferProcessId/dataaddress?auto_refresh=false")
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(JSON)
                .body("'%s'".formatted(DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY), equalTo(dataAddressType));

        verify(edrService).resolveByTransferProcess("transferProcessId", NO_REFRESH);
        verify(transformerRegistry).transform(isA(DataAddress.class), eq(JsonObject.class));
        verifyNoMoreInteractions(transformerRegistry);
    }


    @Test
    void getEdrEntryDataAddress_whenNotFound() {

        when(edrService.resolveByTransferProcess("transferProcessId", AUTO_REFRESH))
                .thenReturn(ServiceResult.notFound("notFound"));


        baseRequest()
                .contentType(JSON)
                .get("/edrs/transferProcessId/dataaddress")
                .then()
                .log().ifError()
                .statusCode(404)
                .contentType(JSON);

        verify(edrService).resolveByTransferProcess("transferProcessId", AUTO_REFRESH);
        verifyNoMoreInteractions(transformerRegistry);
    }

    @Test
    void removeEdrEntry() {
        when(edrStore.delete("transferProcessId"))
                .thenReturn(StoreResult.success(createEdrEntry()));

        baseRequest()
                .contentType(JSON)
                .delete("/edrs/transferProcessId")
                .then()
                .statusCode(204);
        verify(edrStore).delete("transferProcessId");
    }

    @Test
    void removeEdrEntry_whenNotFound() {
        when(edrStore.delete("transferProcessId"))
                .thenReturn(StoreResult.notFound("not found"));

        baseRequest()
                .contentType(JSON)
                .delete("/edrs/transferProcessId")
                .then()
                .statusCode(404);

        verify(edrStore).delete("transferProcessId");
    }

    protected abstract RequestSpecification baseRequest();

    private JsonObjectBuilder createEdrEntryJson() {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, EDR_ENTRY_TYPE)
                .add(ID, TEST_TRANSFER_PROCESS_ID)
                .add(EDR_ENTRY_TRANSFER_PROCESS_ID, TEST_TRANSFER_PROCESS_ID)
                .add(EDR_ENTRY_PROVIDER_ID, TEST_PROVIDER_ID)
                .add(EDR_ENTRY_CONTRACT_NEGOTIATION_ID, TEST_TRANSFER_NEGOTIATION_ID)
                .add(EDR_ENTRY_ASSET_ID, TEST_ASSET_ID)
                .add(EDR_ENTRY_AGREEMENT_ID, TEST_AGREEMENT_ID);
    }

    private JsonObjectBuilder createDataAddress(String type) {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, DataAddress.EDC_DATA_ADDRESS_TYPE)
                .add(DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY, type);
    }

    private EndpointDataReferenceEntry createEdrEntry() {
        return EndpointDataReferenceEntry.Builder.newInstance()
                .agreementId(TEST_AGREEMENT_ID)
                .assetId(TEST_ASSET_ID)
                .providerId(TEST_PROVIDER_ID)
                .transferProcessId(TEST_TRANSFER_PROCESS_ID)
                .contractNegotiationId(TEST_TRANSFER_NEGOTIATION_ID)
                .build();

    }

    private JsonObjectBuilder createContextBuilder() {
        return createObjectBuilder()
                .add(VOCAB, EDC_NAMESPACE)
                .add(EDC_PREFIX, EDC_NAMESPACE);
    }

    private ContractRequest createContractRequest() {
        return ContractRequest.Builder.newInstance()
                .protocol("test-protocol")
                .counterPartyAddress("test-cb")
                .contractOffer(ContractOffer.Builder.newInstance()
                        .id("test-offer-id")
                        .assetId(randomUUID().toString())
                        .policy(Policy.Builder.newInstance().build())
                        .build())
                .build();
    }
}
