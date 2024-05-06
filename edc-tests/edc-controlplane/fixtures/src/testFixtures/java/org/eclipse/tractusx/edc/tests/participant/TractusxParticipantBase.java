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
import org.eclipse.edc.connector.controlplane.test.system.utils.Participant;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
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
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;

/**
 * Base class for doing E2E tests with participants.
 */
public abstract class TractusxParticipantBase extends IdentityParticipant {

    public static final String API_KEY = "testkey";
    public static final Duration ASYNC_TIMEOUT = ofSeconds(60);
    public static final Duration ASYNC_POLL_INTERVAL = ofSeconds(1);
    protected final URI dataPlaneProxy = URI.create("http://localhost:" + getFreePort());
    private final URI controlPlaneDefault = URI.create("http://localhost:" + getFreePort());
    private final URI controlPlaneControl = URI.create("http://localhost:" + getFreePort() + "/control");
    private final URI backendProviderProxy = URI.create("http://localhost:" + getFreePort() + "/events");
    private final URI dataPlanePublic = URI.create("http://localhost:" + getFreePort() + "/public");

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

    /**
     * Returns the base configuration
     */
    public Map<String, String> getConfiguration() {
        return new HashMap<>() {
            {
                put("edc.connector.name", name);
                put("edc.participant.id", id);
                put("web.http.port", String.valueOf(controlPlaneDefault.getPort()));
                put("web.http.path", "/api");
                put("web.http.protocol.port", String.valueOf(protocolEndpoint.getUrl().getPort()));
                put("web.http.protocol.path", protocolEndpoint.getUrl().getPath());
                put("web.http.management.port", String.valueOf(managementEndpoint.getUrl().getPort()));
                put("web.http.management.path", managementEndpoint.getUrl().getPath());
                put("web.http.control.port", String.valueOf(controlPlaneControl.getPort()));
                put("web.http.control.path", controlPlaneControl.getPath());
                put("edc.dsp.callback.address", protocolEndpoint.getUrl().toString());
                put("edc.api.auth.key", "testkey");
                put("web.http.public.path", "/api/public");
                put("web.http.public.port", String.valueOf(dataPlanePublic.getPort()));
                put("edc.transfer.proxy.token.signer.privatekey.alias", getPrivateKeyAlias());
                put("edc.transfer.proxy.token.verifier.publickey.alias", getFullKeyId());
                put("edc.transfer.send.retry.limit", "1");
                put("edc.transfer.send.retry.base-delay.ms", "100");
                put("tx.dpf.consumer.proxy.port", String.valueOf(dataPlaneProxy.getPort()));
                put("edc.dataplane.token.validation.endpoint", controlPlaneControl + "/token");
                put("edc.dataplane.selector.httpplane.url", controlPlaneControl.toString());
                put("edc.dataplane.selector.httpplane.sourcetypes", "HttpData");
                put("edc.dataplane.selector.httpplane.destinationtypes", "HttpProxy");
                put("edc.dataplane.selector.httpplane.transfertypes", "HttpProxy-PULL");
                put("edc.dataplane.selector.httpplane.properties", "{\"publicApiUrl\":\"http://localhost:" + dataPlanePublic.getPort() + "/api/public/v2\"}");
                put("edc.receiver.http.dynamic.endpoint", "http://localhost:" + controlPlaneDefault.getPort() + "/api/consumer/datareference");
                put("tractusx.businesspartnervalidation.log.agreement.validation", "true");
                put("edc.agent.identity.key", "BusinessPartnerNumber");
                put("tx.dpf.proxy.gateway.aas.proxied.path", backendProviderProxy.toString());
                put("tx.dpf.proxy.gateway.aas.authorization.type", "none");
                put("edc.iam.issuer.id", getDid());
                put("edc.iam.sts.oauth.token.url", "http://sts.example.com/token");
                put("edc.iam.sts.oauth.client.id", "test-clientid");
                put("edc.iam.sts.oauth.client.secret.alias", "test-clientid-alias");
                put("edc.iam.sts.dim.url", "http://sts.example.com");
                put("tx.iam.iatp.bdrs.server.url", "http://sts.example.com");
                put("edc.dataplane.api.public.baseurl", "http://localhost:%d/api/public/v2/data".formatted(dataPlanePublic.getPort()));
            }
        };
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
        managementEndpoint.baseRequest()
                .contentType(JSON)
                .body(body)
                .when()
                .post("/business-partner-groups")
                .then()
                .statusCode(204);
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
                .add("counterPartyAddress", provider.protocolEndpoint.getUrl().toString())
                .add("protocol", protocol);


        return managementEndpoint.baseRequest()
                .contentType(JSON)
                .when()
                .body(requestBodyBuilder.build())
                .post("/v2/catalog/request")
                .then();

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
        public TractusxParticipantBase build() {
            if (participant.did == null) {
                participant.did = "did:web:" + participant.name.toLowerCase();
            }

            super.managementEndpoint(new Endpoint(URI.create("http://localhost:" + getFreePort() + "/api/management"), Map.of("x-api-key", API_KEY)));
            super.protocolEndpoint(new Endpoint(URI.create("http://localhost:" + getFreePort() + "/protocol")));
            super.timeout(ASYNC_TIMEOUT);
            super.build();

            this.participant.edrs = new ParticipantEdrApi(participant);
            this.participant.data = new ParticipantDataApi();
            this.participant.dataPlane = new ParticipantConsumerDataPlaneApi(new Endpoint(this.participant.dataPlaneProxy, Map.of("x-api-key", API_KEY)));
            return participant;
        }
    }

}
