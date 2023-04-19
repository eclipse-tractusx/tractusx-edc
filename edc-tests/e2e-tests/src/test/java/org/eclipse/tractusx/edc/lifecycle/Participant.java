/*
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.lifecycle;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.api.management.catalog.model.CatalogRequestDto;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractNegotiationDto;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationInitiateRequestDto;
import org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto;
import org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.InjectionContainer;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.token.MockDapsService;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;

public class Participant extends EdcRuntimeExtension implements BeforeAllCallback, AfterAllCallback {

    private final String managementUrl;
    private final String apiKey;
    private final String idsEndpoint;
    private final TypeManager typeManager = new TypeManager();
    private final String idsId;
    private final String bpn;
    private final String backend;
    private DataWiper wiper;

    public Participant(String moduleName, String runtimeName, Map<String, String> properties) {
        super(moduleName, runtimeName, properties);
        this.managementUrl = URI.create(format("http://localhost:%s%s", properties.get("web.http.management.port"), properties.get("web.http.management.path"))).toString();
        this.idsEndpoint = URI.create(format("http://localhost:%s%s", properties.get("web.http.ids.port"), properties.get("web.http.ids.path"))).toString();
        this.apiKey = properties.get("edc.api.auth.key");
        this.idsId = properties.get("edc.ids.id");
        this.bpn = runtimeName + "-BPN";
        this.backend = properties.get("edc.receiver.http.dynamic.endpoint");
        this.registerServiceMock(IdentityService.class, new MockDapsService(getBpn()));

        typeManager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));

    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) {
        //do nothing - we only want to start the runtime once
        wiper.clearPersistence();
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        //only run this once
        super.beforeTestExecution(context);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        super.afterTestExecution(context);
    }

    /**
     * Creates an asset with the given ID and props using the participant's Data Management API
     */
    public void createAsset(String id, Map<String, String> properties) {
        properties = new HashMap<>(properties);
        properties.put("asset:prop:id", id);
        properties.put("asset:prop:description", "test description");

        var asset = Map.of(
                "asset", Map.of(
                        "id", id,
                        "properties", properties
                ),
                "dataAddress", Map.of(
                        "properties", Map.of("type", "test-type")

                )
        );

        baseRequest()
                .body(asset)
                .when()
                .post("/assets")
                .then()
                .statusCode(200)
                .contentType(JSON);

    }

    /**
     * Creates an asset with the given ID and props using the participant's Data Management API
     */
    public void createAsset(String id, Map<String, String> asserProperties, HttpDataAddress address) {
        asserProperties = new HashMap<>(asserProperties);
        asserProperties.put("asset:prop:id", id);
        asserProperties.put("asset:prop:description", "test description");

        var asset = Map.of(
                "asset", Map.of(
                        "id", id,
                        "properties", asserProperties
                ),
                "dataAddress", address
        );

        baseRequest()
                .body(asset)
                .when()
                .post("/assets")
                .then()
                .statusCode(200)
                .contentType(JSON);
    }

    /**
     * Creates a {@link org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition} using the participant's Data Management API
     */
    public void createContractDefinition(String assetId, String definitionId, String accessPolicyId, String contractPolicyId, long contractValidityDurationSeconds) {
        var contractDefinition = Map.of(
                "id", definitionId,
                "accessPolicyId", accessPolicyId,
                "validity", String.valueOf(contractValidityDurationSeconds),
                "contractPolicyId", contractPolicyId,
                "criteria", AssetSelectorExpression.Builder.newInstance().constraint("asset:prop:id", "=", assetId).build().getCriteria()
        );

        baseRequest()
                .body(contractDefinition)
                .when()
                .post("/contractdefinitions")
                .then()
                .statusCode(200)
                .contentType(JSON).contentType(JSON);
    }

    /**
     * Creates a {@link PolicyDefinition} using the participant's Data Management API
     */
    public void createPolicy(PolicyDefinition policyDefinition) {
        baseRequest()
                .body(policyDefinition)
                .when()
                .post("/policydefinitions")
                .then()
                .statusCode(200)
                .contentType(JSON).contentType(JSON);
    }

    /**
     * Requests the {@link Catalog} from another participant using this participant's Data Management API
     */
    public Catalog requestCatalog(Participant other) {
        return requestCatalog(other, QuerySpecDto.Builder.newInstance().build());
    }

    /**
     * Requests the {@link Catalog} from another participant using this participant's Data Management API
     */
    public Catalog requestCatalog(Participant other, QuerySpecDto query) {
        var response = baseRequest()
                .when()
                .body(CatalogRequestDto.Builder.newInstance()
                        .providerUrl(other.idsEndpoint + "/data")
                        .querySpec(query)
                        .build())
                .post("/catalog/request")
                .then();

        var code = response.extract().statusCode();
        var body = response.extract().body().asString();

        // doing an assertJ style assertion will allow us to use the body as fail message if the return code != 200
        assertThat(code).withFailMessage(body).isEqualTo(200);
        return typeManager.readValue(body, Catalog.class);
    }

    public String negotiateContract(Participant other, String assetId) {
        var catalog = requestCatalog(other);
        assertThat(catalog.getContractOffers()).withFailMessage("Catalog received from " + other.idsId + " was empty!").isNotEmpty();
        var response = baseRequest()
                .when()
                .body(NegotiationInitiateRequestDto.Builder.newInstance()
                        .connectorAddress(other.idsEndpoint + "/data")
                        .connectorId(getBpn())
                        .offer(catalog.getContractOffers().stream().filter(o -> o.getAsset().getId().equals(assetId))
                                .findFirst().map(co -> ContractOfferDescription.Builder.newInstance()
                                        .assetId(assetId)
                                        .offerId(co.getId())
                                        .policy(co.getPolicy())
                                        .validity(ChronoUnit.SECONDS.between(co.getContractStart(), co.getContractEnd().plus(Duration.ofMillis(500)))) // the plus 1 is required due to https://github.com/eclipse-edc/Connector/issues/2650
                                        .build())
                                .orElseThrow((() -> new RuntimeException("A contract for assetId " + assetId + " could not be negotiated"))))
                        .build()
                )
                .post("/contractnegotiations")
                .then();

        var body = response.extract().body().asString();
        assertThat(response.extract().statusCode()).withFailMessage(body).isBetween(200, 299);

        return typeManager.readValue(body, IdResponseDto.class).getId();
    }

    public ContractNegotiationDto getNegotiation(String negotiationId) {
        var response = baseRequest()
                .when()
                .get("/contractnegotiations/" + negotiationId)
                .then();

        var body = response.extract().body().asString();
        assertThat(response.extract().statusCode()).withFailMessage(body).isBetween(200, 299);
        return typeManager.readValue(body, ContractNegotiationDto.class);
    }

    /**
     * Returns this participant's IDS ID
     */
    public String idsId() {
        return idsId;
    }

    /**
     * Returns this participant's BusinessPartnerNumber (=BPN). This is constructed of the runtime name plus "-BPN"
     */
    public String getBpn() {
        return bpn;
    }

    public String requestTransfer(String contractId, String assetId, Participant other, DataAddress destination, String dataRequestId) {
        var response = baseRequest()
                .when()
                .body(TransferRequestDto.Builder.newInstance()
                        .assetId(assetId)
                        .id(dataRequestId)
                        .connectorAddress(other.idsEndpoint + "/data")
                        .managedResources(false)
                        .contractId(contractId)
                        .connectorId(bpn)
                        .protocol("ids-multipart")
                        .dataDestination(destination)
                        .build())
                .post("/transferprocess")
                .then();

        var body = response.extract().body().asString();
        assertThat(response.extract().statusCode()).withFailMessage(body).isBetween(200, 299);

        return typeManager.readValue(body, IdResponseDto.class).getId();
    }

    public TransferProcessDto getTransferProcess(String transferProcessId) {
        var json = baseRequest()
                .when()
                .get("/transferprocess/" + transferProcessId)
                .then()
                .statusCode(allOf(greaterThanOrEqualTo(200), lessThan(300)))
                .extract().body().asString();

        return typeManager.readValue(json, TransferProcessDto.class);

    }

    public EndpointDataReference getDataReference(String dataRequestId) {
        var dataReference = new AtomicReference<EndpointDataReference>();

        var result = given()
                .when()
                .get(backend + "/{id}", dataRequestId)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(EndpointDataReference.class);
        dataReference.set(result);

        return dataReference.get();
    }

    public String pullData(EndpointDataReference edr, Map<String, String> queryParams) {
        var response = given()
                .baseUri(edr.getEndpoint())
                .header(edr.getAuthKey(), edr.getAuthCode())
                .queryParams(queryParams)
                .when()
                .get();
        assertThat(response.statusCode()).isBetween(200, 300);
        return response.body().asString();
    }

    @Override
    protected void bootExtensions(ServiceExtensionContext context, List<InjectionContainer<ServiceExtension>> serviceExtensions) {
        super.bootExtensions(context, serviceExtensions);
        wiper = new DataWiper(context);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri(managementUrl)
                .header("x-api-key", apiKey)
                .contentType(JSON);
    }
}
