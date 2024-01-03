/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.lifecycle.tx;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.test.system.utils.Participant;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.TX_NAMESPACE;
import static org.eclipse.tractusx.edc.tests.TestCommon.ASYNC_TIMEOUT;

/**
 * Implementation for TX of a {@link Participant}
 */
public class TxParticipant extends Participant {

    public static final String API_KEY = "testkey";
    public static final String PROXY_SUBPATH = "proxy/aas/request";
    private final URI controlPlaneDefault = URI.create("http://localhost:" + getFreePort());
    private final URI controlPlaneControl = URI.create("http://localhost:" + getFreePort() + "/control");
    private final URI gateway = URI.create("http://localhost:" + getFreePort() + "/api/gateway");
    private final URI backendProviderProxy = URI.create("http://localhost:" + getFreePort() + "/events");
    private final URI dataPlaneProxy = URI.create("http://localhost:" + getFreePort());
    private final URI dataPlanePublic = URI.create("http://localhost:" + getFreePort() + "/public");
    private final URI miwUri = URI.create("http://localhost:" + getFreePort());
    private final URI oauthTokenUri = URI.create("http://localhost:" + getFreePort());
    private final URI backend = URI.create("http://localhost:" + controlPlaneDefault.getPort() + "/api/consumer/datareference");
    private ParticipantEdrApi edrs;
    private ParticipantDataApi data;

    /**
     * Creates an asset with the given ID and props using the participant's Data Management API
     */
    public void createAsset(String id) {
        createAsset(id, new HashMap<>(), Map.of("type", "test-type"));
    }

    /**
     * Stores BPN groups
     */
    public void storeBusinessPartner(String bpn, String... groups) {
        var body = Json.createObjectBuilder()
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
     * The BPN/ID of the participant
     *
     * @return The bpn
     */

    public String getBpn() {
        return id;
    }


    /**
     * Returns the client API for interacting with EDRs
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
     * Creates a policy definition
     */
    public String createPolicy(JsonObject policyDefinition) {
        return managementRequest()
                .contentType(JSON)
                .body(policyDefinition)
                .when()
                .post("/v2/policydefinitions")
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(JSON)
                .extract()
                .path("@id");
    }

    /**
     * Get current error if any of a contract negotiation.
     *
     * @param negotiationId contract negotiation id
     * @return error of the contract negotiation.
     */
    public String getContractNegotiationError(String negotiationId) {
        return getContractNegotiationField(negotiationId, "errorDetail");
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
                put("web.http.gateway.path", gateway.getPath());
                put("web.http.gateway.port", String.valueOf(gateway.getPort()));

                put("edc.transfer.send.retry.limit", "1");
                put("edc.transfer.send.retry.base-delay.ms", "100");
                put("tx.dpf.consumer.proxy.port", String.valueOf(dataPlaneProxy.getPort()));
                put("edc.dataplane.token.validation.endpoint", controlPlaneControl + "/token");
                put("edc.dataplane.selector.httpplane.url", controlPlaneControl.toString());
                put("edc.dataplane.selector.httpplane.sourcetypes", "HttpData");
                put("edc.dataplane.selector.httpplane.destinationtypes", "HttpProxy");
                put("edc.dataplane.selector.httpplane.properties", "{\"publicApiUrl\":\"http://localhost:" + dataPlanePublic.getPort() + "/api/public\"}");
                put("edc.receiver.http.dynamic.endpoint", "http://localhost:" + controlPlaneDefault.getPort() + "/api/consumer/datareference");
                put("tractusx.businesspartnervalidation.log.agreement.validation", "true");
                put("edc.agent.identity.key", "BusinessPartnerNumber");
                put("edc.data.encryption.keys.alias", "test-alias");
                put("tx.dpf.proxy.gateway.aas.proxied.path", backendProviderProxy.toString());
                put("tx.dpf.proxy.gateway.aas.authorization.type", "none");
            }
        };
    }

    /**
     * Returns the SSI configuration
     */
    public Map<String, String> ssiConfiguration() {
        var ssiConfiguration = new HashMap<String, String>() {
            {
                put("tx.ssi.miw.url", miwUri.toString());
                put("tx.ssi.oauth.token.url", oauthTokenUri.toString());
                put("tx.ssi.oauth.client.id", "client_id");
                put("tx.ssi.oauth.client.secret.alias", "client_secret_alias");
                put("tx.ssi.miw.authority.id", "authorityId");
                put("tx.ssi.miw.authority.issuer", "did:web:example.com");
                put("tx.vault.seed.secrets", "client_secret_alias:client_secret");
                put("tx.ssi.endpoint.audience", protocolEndpoint.getUrl().toString());
            }
        };
        var baseConfiguration = getConfiguration();
        ssiConfiguration.putAll(baseConfiguration);
        return ssiConfiguration;
    }

    /**
     * Returns the renewal configuration
     */
    public Map<String, String> renewalConfiguration() {
        return renewalConfiguration("10");
    }

    /**
     * Returns the renewal configuration
     */
    public Map<String, String> renewalConfiguration(String retention) {
        var renewalConfig = new HashMap<String, String>() {
            {
                put("edc.edr.state-machine.expiring-duration", "10");
                put("edc.edr.state-machine.expired-retention", retention);
                put("edc.transfer.proxy.token.validity.seconds", "15");
            }
        };
        var baseConfiguration = getConfiguration();
        renewalConfig.putAll(baseConfiguration);

        return renewalConfig;
    }

    /**
     * Returns the MIW endpoint
     */
    public URI miwEndpoint() {
        return miwUri;
    }

    /**
     * Returns the OAuth2 token endpoint
     */
    public URI authTokenEndpoint() {
        return oauthTokenUri;
    }

    /**
     * Returns the Gateway endpoint
     */
    public URI gatewayEndpoint() {
        return gateway;
    }

    /**
     * Returns the Consumer data plane proxy endpoint
     */
    public URI dataPlaneProxy() {
        return dataPlaneProxy;
    }

    /**
     * Returns the provider gateway backend endpoint
     */
    public URI backendProviderProxy() {
        return backendProviderProxy;
    }

    protected RequestSpecification managementRequest() {
        return managementEndpoint.baseRequest();
    }

    public static final class Builder extends Participant.Builder<TxParticipant, Builder> {

        private Builder() {
            super(new TxParticipant());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public TxParticipant build() {
            super.managementEndpoint(new Endpoint(URI.create("http://localhost:" + getFreePort() + "/api/management"), Map.of("x-api-key", API_KEY)));
            super.protocolEndpoint(new Endpoint(URI.create("http://localhost:" + getFreePort() + "/protocol")));
            super.timeout(ASYNC_TIMEOUT);
            super.build();

            this.participant.edrs = new ParticipantEdrApi(participant, participant.managementEndpoint, participant.backend);
            this.participant.data = new ParticipantDataApi(participant);
            return participant;
        }
    }
}
