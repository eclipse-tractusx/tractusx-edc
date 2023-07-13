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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.helpers.AssetHelperFunctions;
import org.eclipse.tractusx.edc.helpers.ContractDefinitionHelperFunctions;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATASET_ATTRIBUTE;
import static org.eclipse.tractusx.edc.helpers.AssetHelperFunctions.createDataAddressBuilder;
import static org.eclipse.tractusx.edc.helpers.CatalogHelperFunctions.createCatalogRequest;
import static org.eclipse.tractusx.edc.helpers.CatalogHelperFunctions.getDatasetAssetId;
import static org.eclipse.tractusx.edc.helpers.CatalogHelperFunctions.getDatasetContractId;
import static org.eclipse.tractusx.edc.helpers.CatalogHelperFunctions.getDatasetFirstPolicy;
import static org.eclipse.tractusx.edc.helpers.ContractNegotiationHelperFunctions.createNegotiationRequest;
import static org.eclipse.tractusx.edc.helpers.EdrNegotiationHelperFunctions.createEdrNegotiationRequest;
import static org.eclipse.tractusx.edc.helpers.TransferProcessHelperFunctions.createTransferRequest;
import static org.mockito.Mockito.mock;

public class Participant {

    private static final String PROXY_SUBPATH = "proxy/aas/request";

    private final String managementUrl;
    private final String apiKey;
    private final String dspEndpoint;

    private final String gatewayEndpoint;

    private final String runtimeName;
    private final String bpn;
    private final String backend;
    private final JsonLd jsonLd;
    private final Duration timeout = Duration.ofSeconds(30);

    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private final String proxyUrl;

    public Participant(String runtimeName, String bpn, Map<String, String> properties) {
        this.managementUrl = URI.create(format("http://localhost:%s%s", properties.get("web.http.management.port"), properties.get("web.http.management.path"))).toString();
        this.dspEndpoint = URI.create(format("http://localhost:%s%s", properties.get("web.http.protocol.port"), properties.get("web.http.protocol.path"))).toString();
        this.apiKey = properties.get("edc.api.auth.key");
        this.gatewayEndpoint = URI.create(format("http://localhost:%s/api/gateway", properties.get("web.http.port"))).toString();
        this.proxyUrl = URI.create(format("http://localhost:%s", properties.get("tx.dpf.consumer.proxy.port"))).toString();
        this.bpn = bpn;
        this.runtimeName = runtimeName;
        this.backend = properties.get("edc.receiver.http.dynamic.endpoint");
        jsonLd = new TitaniumJsonLd(mock(Monitor.class));
    }

    /**
     * Creates an asset with the given ID and props using the participant's Data Management API
     */
    public void createAsset(String id, JsonObject properties) {
        createAsset(id, properties, createDataAddressBuilder("test-type").build());
    }

    /**
     * Creates an asset with the given ID and props using the participant's Data Management API
     */
    public void createAsset(String id) {
        createAsset(id, Json.createObjectBuilder().build(), createDataAddressBuilder("test-type").build());
    }

    /**
     * Creates an asset with the given ID and props using the participant's Data Management API
     */
    public void createAsset(String id, JsonObject assetProperties, JsonObject dataAddress) {
        var asset = AssetHelperFunctions.createAsset(id, assetProperties, dataAddress);

        baseRequest()
                .body(asset)
                .when()
                .post("/v2/assets")
                .then()
                .statusCode(200)
                .contentType(JSON);
    }

    /**
     * Creates a {@link org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition} using the participant's Data Management API
     */
    public void createContractDefinition(String assetId, String definitionId, String accessPolicyId, String contractPolicyId) {
        var requestBody = ContractDefinitionHelperFunctions.createContractDefinition(assetId, definitionId, accessPolicyId, contractPolicyId);

        baseRequest()
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post("/v2/contractdefinitions")
                .then()
                .statusCode(200)
                .contentType(JSON);
    }

    public void createPolicy(JsonObject policyDefinition) {
        baseRequest()
                .contentType(JSON)
                .body(policyDefinition)
                .when()
                .post("/v2/policydefinitions")
                .then()
                .statusCode(200)
                .contentType(JSON);
    }

    public String negotiateContract(Participant other, String assetId) {
        var dataset = getDatasetForAsset(other, assetId);
        assertThat(dataset).withFailMessage("Catalog received from " + other.runtimeName + " was empty!").isNotEmpty();

        var policy = getDatasetFirstPolicy(dataset);
        var contractId = getDatasetContractId(dataset);
        var requestBody = createNegotiationRequest(other.dspEndpoint, other.getBpn(), contractId.toString(), contractId.assetIdPart(), policy);
        var response = baseRequest()
                .when()
                .body(requestBody)
                .post("/v2/contractnegotiations")
                .then();

        var body = response.extract().body().asString();
        assertThat(response.extract().statusCode()).withFailMessage(body).isBetween(200, 299);

        return response.extract().jsonPath().getString(ID);
    }

    public String negotiateEdr(Participant other, String assetId, JsonArray callbacks) {
        var dataset = getDatasetForAsset(other, assetId);
        assertThat(dataset).withFailMessage("Catalog received from " + other.runtimeName + " was empty!").isNotEmpty();

        var policy = getDatasetFirstPolicy(dataset);
        var contractId = getDatasetContractId(dataset);

        var requestBody = createEdrNegotiationRequest(other.dspEndpoint, other.getBpn(), contractId.toString(), contractId.assetIdPart(), policy, callbacks);


        var response = baseRequest()
                .when()
                .body(requestBody)
                .post("/edrs")
                .then();

        var body = response.extract().body().asString();
        assertThat(response.extract().statusCode()).withFailMessage(body).isBetween(200, 299);

        return response.extract().jsonPath().getString(ID);
    }

    public String getNegotiationState(String negotiationId) {
        return baseRequest()
                .when()
                .get("/v2/contractnegotiations/{id}/state", negotiationId)
                .then()
                .statusCode(200)
                .extract().body().jsonPath().getString("'edc:state'");
    }

    public String getContractAgreementId(String negotiationId) {
        return getContractNegotiationField(negotiationId, "contractAgreementId");
    }

    public String getContractNegotiationError(String negotiationId) {
        return getContractNegotiationField(negotiationId, "errorDetail");
    }

    public JsonObject getEdr(String transferProcessId) {
        return getEdrRequest(transferProcessId)
                .statusCode(200)
                .extract()
                .body()
                .as(JsonObject.class);
    }

    public ValidatableResponse getEdrRequest(String transferProcessId) {
        return baseRequest()
                .when()
                .get("/edrs/{id}", transferProcessId)
                .then();
    }

    public JsonArray getEdrEntriesByAssetId(String assetId) {
        return baseRequest()
                .when()
                .get("/edrs?assetId={assetId}", assetId)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(JsonArray.class);
    }

    public JsonArray getEdrEntriesByAgreementId(String agreementId) {
        return baseRequest()
                .when()
                .get("/edrs?agreementId={agreementId}", agreementId)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(JsonArray.class);
    }

    /**
     * Returns this participant's BusinessPartnerNumber (=BPN). This is constructed of the runtime name plus "-BPN"
     */
    public String getBpn() {
        return bpn;
    }

    public String requestTransfer(String dataRequestId, String contractId, String assetId, Participant other, JsonObject destination) {

        var request = createTransferRequest(dataRequestId, other.dspEndpoint, contractId, assetId, false, destination);
        var response = baseRequest()
                .when()
                .body(request)
                .post("/v2/transferprocesses")
                .then();

        var body = response.extract().body().asString();
        assertThat(response.extract().statusCode()).withFailMessage(body).isBetween(200, 299);

        return response.extract().jsonPath().getString(ID);

    }

    public String getTransferProcessState(String id) {
        return baseRequest()
                .when()
                .get("/v2/transferprocesses/{id}/state", id)
                .then()
                .statusCode(200)
                .extract().body().jsonPath().getString("'edc:state'");
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

    public JsonArray getCatalogDatasets(Participant provider) {
        return getCatalogDatasets(provider, null);
    }

    public JsonArray getCatalogDatasets(Participant provider, JsonObject querySpec) {
        var datasetReference = new AtomicReference<JsonArray>();

        var requestBody = createCatalogRequest(querySpec, provider.dspEndpoint);

        await().atMost(timeout).untilAsserted(() -> {
            var response = baseRequest()
                    .contentType(JSON)
                    .when()
                    .body(requestBody)
                    .post("/v2/catalog/request")
                    .then()
                    .statusCode(200)
                    .extract().body().asString();

            var responseBody = objectMapper.readValue(response, JsonObject.class);

            var catalog = jsonLd.expand(responseBody).orElseThrow(f -> new EdcException(f.getFailureDetail()));

            var datasets = catalog.getJsonArray(DCAT_DATASET_ATTRIBUTE);
            assertThat(datasets).hasSizeGreaterThan(0);

            datasetReference.set(datasets);
        });

        return datasetReference.get();
    }

    public String pullProxyDataByAssetId(Participant provider, String assetId) {
        var body = Map.of("assetId", assetId, "endpointUrl", format("%s/aas/test", provider.gatewayEndpoint));
        return getProxyData(body);
    }

    public Response pullProxyDataResponseByAssetId(Participant provider, String assetId) {
        var body = Map.of("assetId", assetId,
                "endpointUrl", format("%s/aas/test", provider.gatewayEndpoint),
                "providerId", provider.bpn);
        return proxyRequest(body);
    }

    public String pullProxyDataByTransferProcessId(Participant provider, String transferProcessId) {
        var body = Map.of("transferProcessId", transferProcessId,
                "endpointUrl", format("%s/aas/test", provider.gatewayEndpoint));
        return getProxyData(body);

    }

    public JsonObject getDatasetForAsset(Participant provider, String assetId) {
        var datasets = getCatalogDatasets(provider);
        return datasets.stream()
                .map(JsonValue::asJsonObject)
                .filter(it -> assetId.equals(getDatasetAssetId(it)))
                .findFirst()
                .orElseThrow(() -> new EdcException(format("No dataset for asset %s in the catalog", assetId)));
    }

    private String getContractNegotiationField(String negotiationId, String fieldName) {
        return baseRequest()
                .when()
                .get("/v2/contractnegotiations/{id}", negotiationId)
                .then()
                .statusCode(200)
                .extract().body().jsonPath()
                .getString(format("'edc:%s'", fieldName));
    }

    private String getProxyData(Map<String, String> body) {
        return proxyRequest(body)
                .then()
                .assertThat().statusCode(200)
                .extract().body().asString();
    }

    private Response proxyRequest(Map<String, String> body) {
        return given()
                .baseUri(proxyUrl)
                .contentType("application/json")
                .body(body)
                .post(PROXY_SUBPATH);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri(managementUrl)
                .header("x-api-key", apiKey)
                .contentType(JSON);
    }
}
