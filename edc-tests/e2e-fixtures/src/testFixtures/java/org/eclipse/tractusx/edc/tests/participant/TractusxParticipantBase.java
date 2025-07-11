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

package org.eclipse.tractusx.edc.tests.participant;

import io.restassured.response.ValidatableResponse;
import jakarta.json.Json;
import org.eclipse.edc.connector.controlplane.test.system.utils.LazySupplier;
import org.eclipse.edc.connector.controlplane.test.system.utils.Participant;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.tractusx.edc.tests.IdentityParticipant;
import org.eclipse.tractusx.edc.tests.ParticipantConsumerDataPlaneApi;
import org.eclipse.tractusx.edc.tests.ParticipantDataApi;
import org.eclipse.tractusx.edc.tests.ParticipantEdrApi;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry.AR_ENTRY_AGREEMENT_ID;
import static org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry.AR_ENTRY_REASON;
import static org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry.AR_ENTRY_TYPE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;


/**
 * Base class for doing E2E tests with participants.
 */
public abstract class TractusxParticipantBase extends IdentityParticipant {

    public static final String MANAGEMENT_API_KEY = "testkey";
    public static final Duration ASYNC_TIMEOUT = ofSeconds(120);
    public static final Duration ASYNC_POLL_INTERVAL = ofSeconds(1);
    private static final String CONSUMER_PROXY_API_KEY = "consumerProxyKey";
    private static final String API_KEY_HEADER_NAME = "x-api-key";
    protected final LazySupplier<URI> dataPlaneProxy = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort()));
    private final LazySupplier<URI> dataPlanePublic = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/public"));
    private final LazySupplier<URI> federatedCatalog = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/api/catalog"));
    protected ParticipantEdrApi edrs;
    protected ParticipantDataApi data;
    protected ParticipantConsumerDataPlaneApi dataPlane;
    protected String did;

    public void createAsset(String id) {
        createAsset(id, new HashMap<>(), Map.of("type", "test-type"));
    }

    public String getBpn() {
        return getId();
    }

    public Config getConfig() {
        var settings = new HashMap<String, String>() {
            {
                put("edc.runtime.id", name);
                put("edc.participant.id", id);
                put("web.http.port", String.valueOf(getFreePort()));
                put("web.http.path", "/api");
                put("web.http.protocol.port", String.valueOf(controlPlaneProtocol.get().getPort()));
                put("web.http.protocol.path", controlPlaneProtocol.get().getPath());
                put("web.http.management.port", String.valueOf(controlPlaneManagement.get().getPort()));
                put("web.http.management.path", controlPlaneManagement.get().getPath());
                put("web.http.management.auth.key", MANAGEMENT_API_KEY);
                put("web.http.control.port", String.valueOf(getFreePort()));
                put("web.http.control.path", "/control");
                put("web.http.version.port", String.valueOf(getFreePort()));
                put("web.http.version.path", "/version");
                put("web.http.catalog.port", String.valueOf(federatedCatalog.get().getPort()));
                put("web.http.catalog.path", federatedCatalog.get().getPath());
                put("web.http.catalog.auth.type", "tokenbased");
                put("web.http.catalog.auth.key", MANAGEMENT_API_KEY);
                put("edc.dsp.callback.address", controlPlaneProtocol.get().toString());
                put("web.http.public.path", dataPlanePublic.get().getPath());
                put("web.http.public.port", String.valueOf(dataPlanePublic.get().getPort()));
                put("edc.transfer.proxy.token.signer.privatekey.alias", getPrivateKeyAlias());
                put("edc.transfer.proxy.token.verifier.publickey.alias", getFullKeyId());
                put("edc.transfer.send.retry.limit", "1");
                put("edc.transfer.send.retry.base-delay.ms", "100");
                put("tx.edc.dpf.consumer.proxy.port", String.valueOf(dataPlaneProxy.get().getPort()));
                put("tx.edc.dpf.consumer.proxy.auth.apikey", CONSUMER_PROXY_API_KEY);
                put("tractusx.businesspartnervalidation.log.agreement.validation", "true");
                put("edc.agent.identity.key", "BusinessPartnerNumber");
                put("edc.iam.issuer.id", getDid());
                put("edc.iam.sts.oauth.token.url", "http://sts.example.com/token");
                put("edc.iam.sts.oauth.client.id", "test-clientid");
                put("edc.iam.sts.oauth.client.secret.alias", "test-clientid-alias");
                put("tx.edc.iam.iatp.bdrs.server.url", "http://sts.example.com");
                put("edc.dataplane.api.public.baseurl", "%s/v2/data".formatted(dataPlanePublic.get()));
                put("edc.catalog.cache.execution.delay.seconds", "2");
                put("edc.catalog.cache.execution.period.seconds", "2");
                put("edc.policy.validation.enabled", "true");
            }
        };

        return ConfigFactory.fromMap(settings);
    }

    /**
     * Returns the client api for fetching EDRs
     */
    public ParticipantEdrApi edrs() {
        return edrs;
    }

    /**
     * Returns the client API for fetching data
     */
    public ParticipantDataApi data() {
        return data;
    }


    /**
     * Returns the consumer data plane api for fetching data via consumer proxy
     */
    public ParticipantConsumerDataPlaneApi dataPlane() {
        return dataPlane;
    }

    /**
     * Stores BPN groups
     */
    public void storeBusinessPartner(String bpn, String... groups) {
        var body = createObjectBuilder()
                .add(ID, bpn)
                .add(TX_NAMESPACE + "groups", Json.createArrayBuilder(Arrays.asList(groups)))
                .build();
        baseManagementRequest()
                .contentType(JSON)
                .body(body)
                .when()
                .post("/v3/business-partner-groups")
                .then()
                .statusCode(204);
    }

    /**
     * Updates a BPN's groups
     */
    public void updateBusinessPartner(String bpn, String... groups) {
        var body = createObjectBuilder()
                .add(ID, bpn)
                .add(TX_NAMESPACE + "groups", Json.createArrayBuilder(Arrays.asList(groups)))
                .build();
        baseManagementRequest()
                .contentType(JSON)
                .body(body)
                .when()
                .put("/v3/business-partner-groups")
                .then()
                .statusCode(204);
    }

    /**
     * Delete a BPN
     */
    public void deleteBusinessPartner(String bpn) {
        baseManagementRequest()
                .when()
                .delete("/v3/business-partner-groups/{bpn}", bpn)
                .then()
                .statusCode(204);
    }

    public ValidatableResponse retireProviderAgreement(String agreementId) {
        var body = createObjectBuilder()
                .add(TYPE, AR_ENTRY_TYPE)
                .add(AR_ENTRY_AGREEMENT_ID, agreementId)
                .add(AR_ENTRY_REASON, "long-reason")
                .build();
        return baseManagementRequest()
                .contentType(JSON)
                .body(body)
                .when()
                .post("/v3/contractagreements/retirements")
                .then();
    }

    /**
     * waits for the configured timeout until the transfer process reaches the provided state
     *
     * @param transferProcessId The transfer process id
     * @param state             The transfer process state to check
     */
    public void waitForTransferProcess(String transferProcessId, TransferProcessStates state) {

        await().pollInterval(fibonacci())
                .atMost(timeout)
                .untilAsserted(() -> {
                    var tpState = getTransferProcessState(transferProcessId);
                    assertThat(tpState).isNotNull().isEqualTo(state.toString());
                });
    }

    @Override
    public String getFullKeyId() {
        return getDid() + "#" + getKeyId();
    }

    @NotNull
    public String getDid() {
        return did;
    }

    public ValidatableResponse getCatalog(TractusxParticipantBase provider) {
        var requestBodyBuilder = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(VOCAB, EDC_NAMESPACE))
                .add(TYPE, "CatalogRequest")
                .add("counterPartyId", provider.id)
                .add("counterPartyAddress", provider.getProtocolUrl())
                .add("protocol", protocol);

        return baseManagementRequest()
                .header("x-api-key", MANAGEMENT_API_KEY)
                .contentType(JSON)
                .when()
                .body(requestBodyBuilder.build())
                .post("/v3/catalog/request")
                .then();

    }

    public ValidatableResponse getFederatedCatalog() {
        var requestBodyBuilder = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(VOCAB, EDC_NAMESPACE))
                .add(TYPE, "QuerySpec");


        return given()
                .baseUri(federatedCatalog.get().toString())
                .header("x-api-key", MANAGEMENT_API_KEY)
                .contentType(JSON)
                .when()
                .body(requestBodyBuilder.build())
                .post("/v1alpha/catalog/query")
                .then();

    }

    public String getTransferProcessField(String transferProcessId, String fieldName) {
        return baseManagementRequest()
                .contentType(JSON)
                .when()
                .get("/v3/transferprocesses/{id}", transferProcessId)
                .then()
                .statusCode(200)
                .extract().body().jsonPath()
                .getString(fieldName);
    }

    public static class Builder<P extends TractusxParticipantBase, B extends Builder<P, B>> extends Participant.Builder<P, B> {
        protected Builder(P participant) {
            super(participant);
        }

        public B did(String did) {
            this.participant.did = did;
            return self();
        }

        @Override
        public P build() {
            if (participant.did == null) {
                participant.did = "did:web:" + participant.name.toLowerCase();
            }

            participant.enrichManagementRequest = requestSpecification -> requestSpecification.headers(Map.of(API_KEY_HEADER_NAME, MANAGEMENT_API_KEY));
            super.timeout(ASYNC_TIMEOUT);
            super.build();

            this.participant.edrs = new ParticipantEdrApi(participant);
            this.participant.data = new ParticipantDataApi();
            this.participant.dataPlane = new ParticipantConsumerDataPlaneApi(this.participant.dataPlaneProxy, Map.of("x-api-key", CONSUMER_PROXY_API_KEY));
            return participant;
        }
    }

}
