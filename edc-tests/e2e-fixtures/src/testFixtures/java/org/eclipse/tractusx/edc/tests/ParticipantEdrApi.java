/********************************************************************************
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
 ********************************************************************************/

package org.eclipse.tractusx.edc.tests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.test.system.utils.Participant;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;

import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ASSIGNER_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_TARGET_ATTRIBUTE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.tests.helpers.CatalogHelperFunctions.getDatasetFirstPolicy;
import static org.eclipse.tractusx.edc.tests.helpers.EdrNegotiationHelperFunctions.createEdrNegotiationRequest;

/**
 * E2E test helper for the EDR APIs
 */
public class ParticipantEdrApi {

    private final Participant participant;

    public ParticipantEdrApi(Participant participant) {
        this.participant = participant;
    }

    /**
     * Get the cached EDR for a transfer process
     *
     * @param transferProcessId The transfer process id
     * @return The EDR
     */
    public JsonObject getEdr(String transferProcessId) {
        return getEdrRequest(transferProcessId)
                .statusCode(200)
                .extract()
                .body()
                .as(JsonObject.class);
    }

    /**
     * Get the cached EDR for a transfer process as {@link ValidatableResponse}
     *
     * @param transferProcessId The transfer process id
     * @return The {@link ValidatableResponse}
     */
    public ValidatableResponse getEdrRequest(String transferProcessId) {
        return getEdrWithRefresh(transferProcessId, false);
    }

    /**
     * Get the cached EDR for a transfer process as {@link ValidatableResponse}
     *
     * @param transferProcessId The transfer process id
     * @return The {@link ValidatableResponse}
     */
    public ValidatableResponse getEdrWithRefresh(String transferProcessId, boolean autoRefresh) {
        return baseEdrRequest()
                .when()
                .get("/v3/edrs/{id}/dataaddress?auto_refresh={auto_refresh}", transferProcessId, autoRefresh)
                .then()
                .log().ifError();

    }

    /**
     * Triggers the explicit renewal of an EDR identified by {@code transferProcessId}
     */
    public ValidatableResponse refreshEdr(String transferProcessId) {
        return baseEdrRequest()
                .when()
                .post("/v3/edrs/{id}/refresh", transferProcessId)
                .then()
                .log().ifError();
    }

    /**
     * Start an EDR negotiation using the EDRs API.
     *
     * @param other     The provider
     * @param assetId   The asset ID
     * @param callbacks The callbacks
     * @return The contract negotiation id
     */
    public String negotiateEdr(TransferParticipant other, String assetId, JsonArray callbacks) {
        var dataset = participant.getDatasetForAsset(other, assetId);
        assertThat(dataset).withFailMessage("Catalog received from " + other.getName() + " was empty!").isNotEmpty();

        var policy = createObjectBuilder(getDatasetFirstPolicy(dataset))
                .add(ODRL_TARGET_ATTRIBUTE, createObjectBuilder().add(ID, dataset.get(ID)))
                .add(ODRL_ASSIGNER_ATTRIBUTE, createObjectBuilder().add(ID, other.getBpn()))
                .build();

        var requestBody = createEdrNegotiationRequest(other.getProtocolEndpoint().getUrl().toString(), policy, callbacks);


        var response = baseEdrRequest()
                .when()
                .body(requestBody)
                .post("/v3/edrs")
                .then();

        var body = response.extract().body().asString();
        assertThat(response.extract().statusCode()).withFailMessage(body).isBetween(200, 299);

        return response.extract().jsonPath().getString(ID);
    }

    /**
     * Get the cached EDRs for a contract negotiation
     *
     * @param contractNegotiationId The contract negotiation id
     * @return The EDRs
     */
    public JsonArray getEdrEntriesByContractNegotiationId(String contractNegotiationId) {
        var query = createQuery("contractNegotiationId", "=", contractNegotiationId);
        return baseEdrRequest()
                .when()
                .body(query)
                .post("/v3/edrs/request")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(JsonArray.class);
    }

    /**
     * Get the cached EDRs for a contract agreement
     *
     * @param agreementId The contract agreement id
     * @return The EDRs
     */
    public JsonArray getEdrEntriesByAgreementId(String agreementId) {
        var query = createQuery("agreementId", "=", agreementId);
        return baseEdrRequest()
                .when()
                .body(query)
                .post("/v3/edrs/request")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(JsonArray.class);
    }

    /**
     * Get the cached EDRs for an asset
     *
     * @param assetId The asset id
     * @return The EDRs
     */
    public JsonArray getEdrEntriesByAssetId(String assetId) {
        var query = createQuery("assetId", "=", assetId);
        return baseEdrRequest()
                .when()
                .body(query)
                .post("/v3/edrs/request")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(JsonArray.class);
    }

    /**
     * Waits for the EDR associated with the transfer process to be available
     *
     * @param transferProcessId The transfer process id
     * @return The {@link JsonObject} representation of the EDR
     */
    public JsonObject waitForEdr(String transferProcessId) {
        var edr = new AtomicReference<JsonObject>();
        await().pollInterval(fibonacci())
                .atMost(participant.getTimeout())
                .untilAsserted(() -> {
                    edr.set(getEdr(transferProcessId));
                    assertThat(edr).isNotNull();
                });

        return edr.get();
    }

    /**
     * Creates a query spec as JSON object that can be passed into the new EDR-V2 API (/request). Not yet used
     */
    private String createQuery(String leftOp, String op, String rightOp) {
        return Json.createObjectBuilder()
                .add(CONTEXT, Json.createObjectBuilder().add(VOCAB, EDC_NAMESPACE).build())
                .add(TYPE, "QuerySpec")
                .add("filterExpression", Json.createObjectBuilder()
                        .add("operandLeft", leftOp)
                        .add("operator", op)
                        .add("operandRight", rightOp)
                        .build())
                .build()
                .toString();
    }

    private RequestSpecification baseEdrRequest() {
        return participant.getManagementEndpoint().baseRequest().contentType(JSON);
    }
}
