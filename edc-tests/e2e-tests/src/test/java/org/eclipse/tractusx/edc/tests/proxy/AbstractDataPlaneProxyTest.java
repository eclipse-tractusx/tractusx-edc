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

package org.eclipse.tractusx.edc.tests.proxy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessCompleted;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.tractusx.edc.lifecycle.Participant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.helpers.EdrNegotiationHelperFunctions.createCallback;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.businessPartnerNumberPolicy;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_PROXIED_AAS_BACKEND_PORT;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.platoConfiguration;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.sokratesConfiguration;

public abstract class AbstractDataPlaneProxyTest {

    protected static final Participant SOKRATES = new Participant(SOKRATES_NAME, SOKRATES_BPN, sokratesConfiguration());
    protected static final Participant PLATO = new Participant(PLATO_NAME, PLATO_BPN, platoConfiguration());

    MockWebServer server = new MockWebServer();

    ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Verify E2E flow with Data Plane proxies and EDR")
    void httpPullDataTransfer_withEdrAndProxy() throws IOException {

        var eventsUrl = server.url("/events");

        var assetId = "api-asset-1";
        var authCodeHeaderName = "test-authkey";
        var authCode = "test-authcode";
        PLATO.createAsset(assetId, Json.createObjectBuilder().build(), Json.createObjectBuilder()
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "contentType", "application/json")
                .add(EDC_NAMESPACE + "baseUrl", eventsUrl.toString())
                .add(EDC_NAMESPACE + "authKey", authCodeHeaderName)
                .add(EDC_NAMESPACE + "authCode", authCode)
                .build());

        PLATO.createPolicy(businessPartnerNumberPolicy("policy-1", SOKRATES.getBpn()));
        PLATO.createPolicy(businessPartnerNumberPolicy("policy-2", SOKRATES.getBpn()));
        PLATO.createContractDefinition(assetId, "def-1", "policy-1", "policy-2");

        var callbacks = Json.createArrayBuilder()
                .add(createCallback(eventsUrl.toString(), true, Set.of("transfer.process.completed")))
                .build();

        // response to callback
        server.enqueue(new MockResponse());

        SOKRATES.negotiateEdr(PLATO, assetId, callbacks);

        var transferEvent = waitForTransferCompletion();

        var body = "{\"response\": \"ok\"}";

        server.enqueue(new MockResponse().setBody(body));
        var data = SOKRATES.pullProxyDataByAssetId(PLATO, assetId);
        assertThat(data).isEqualTo(body);

        server.enqueue(new MockResponse().setBody(body));
        data = SOKRATES.pullProxyDataByTransferProcessId(PLATO, transferEvent.getPayload().getTransferProcessId());
        assertThat(data).isEqualTo(body);
    }

    @Test
    @DisplayName("Verify E2E flow with Data Plane proxies fails when EDR is not found")
    void httpPullDataTransfer_withoutEdr() throws IOException {

        var eventsUrl = server.url("/events");

        var assetId = "api-asset-1";
        var authCodeHeaderName = "test-authkey";
        var authCode = "test-authcode";
        PLATO.createAsset(assetId, Json.createObjectBuilder().build(), Json.createObjectBuilder()
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "contentType", "application/json")
                .add(EDC_NAMESPACE + "baseUrl", eventsUrl.toString())
                .add(EDC_NAMESPACE + "authKey", authCodeHeaderName)
                .add(EDC_NAMESPACE + "authCode", authCode)
                .build());

        PLATO.createPolicy(businessPartnerNumberPolicy("policy-1", SOKRATES.getBpn()));
        PLATO.createPolicy(businessPartnerNumberPolicy("policy-2", SOKRATES.getBpn()));
        PLATO.createContractDefinition(assetId, "def-1", "policy-1", "policy-2");


        SOKRATES.pullProxyDataResponseByAssetId(PLATO, assetId)
                .then()
                .assertThat().statusCode(400);

    }

    @Test
    @DisplayName("Verify E2E flow with Data Plane proxies and Two EDR")
    void httpPullDataTransfer_shouldFailForAsset_withTwoEdrAndProxy() throws IOException {

        var eventsUrl = server.url("/events");

        var assetId = "api-asset-1";
        var authCodeHeaderName = "test-authkey";
        var authCode = "test-authcode";
        PLATO.createAsset(assetId, Json.createObjectBuilder().build(), Json.createObjectBuilder()
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "contentType", "application/json")
                .add(EDC_NAMESPACE + "baseUrl", eventsUrl.toString())
                .add(EDC_NAMESPACE + "authKey", authCodeHeaderName)
                .add(EDC_NAMESPACE + "authCode", authCode)
                .build());

        PLATO.createPolicy(businessPartnerNumberPolicy("policy-1", SOKRATES.getBpn()));
        PLATO.createPolicy(businessPartnerNumberPolicy("policy-2", SOKRATES.getBpn()));
        PLATO.createContractDefinition(assetId, "def-1", "policy-1", "policy-2");

        var callbacks = Json.createArrayBuilder()
                .add(createCallback(eventsUrl.toString(), true, Set.of("transfer.process.completed")))
                .build();

        // response to callback
        server.enqueue(new MockResponse());
        server.enqueue(new MockResponse());

        SOKRATES.negotiateEdr(PLATO, assetId, callbacks);
        SOKRATES.negotiateEdr(PLATO, assetId, callbacks);

        var transferEvent1 = waitForTransferCompletion();
        var transferEvent2 = waitForTransferCompletion();

        var body = "{\"response\": \"ok\"}";

        server.enqueue(new MockResponse().setBody(body));
        SOKRATES.pullProxyDataResponseByAssetId(PLATO, assetId).then()
                .assertThat().statusCode(428);

        server.enqueue(new MockResponse().setBody(body));
        var data = SOKRATES.pullProxyDataByTransferProcessId(PLATO, transferEvent1.getPayload().getTransferProcessId());
        assertThat(data).isEqualTo(body);

        server.enqueue(new MockResponse().setBody(body));
        data = SOKRATES.pullProxyDataByTransferProcessId(PLATO, transferEvent2.getPayload().getTransferProcessId());
        assertThat(data).isEqualTo(body);
    }

    @BeforeEach
    void setup() throws IOException {
        server.start(PLATO_PROXIED_AAS_BACKEND_PORT);
    }

    @AfterEach
    void teardown() throws IOException {
        server.shutdown();
    }

    EventEnvelope<TransferProcessCompleted> waitForTransferCompletion() {
        try {
            var request = server.takeRequest(20, TimeUnit.SECONDS);
            if (request != null) {
                return mapper.readValue(request.getBody().inputStream(), new TypeReference<>() {
                });
            } else {
                throw new RuntimeException("Timeout exceeded waiting for events");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
