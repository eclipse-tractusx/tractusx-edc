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
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.tractusx.edc.lifecycle.Participant;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.helpers.EdrNegotiationHelperFunctions.createCallback;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.businessPartnerGroupPolicy;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_PROXIED_AAS_BACKEND_PORT;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PROXIED_PATH;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.platoConfiguration;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.sokratesConfiguration;

public abstract class AbstractDataPlaneProxyTest {

    protected static final Participant SOKRATES = new Participant(SOKRATES_NAME, SOKRATES_BPN, sokratesConfiguration());
    protected static final Participant PLATO = new Participant(PLATO_NAME, PLATO_BPN, platoConfiguration());
    private static final String CUSTOM_BASE_PATH = "/custom";
    private static final String CUSTOM_SUB_PATH = "/sub";

    private static final String CUSTOM_QUERY_PARAMS = "foo=bar";

    private static final Duration ASYNC_TIMEOUT = ofSeconds(45);
    private static final Duration ASYNC_POLL_INTERVAL = ofSeconds(1);

    private static final String CUSTOM_FULL_PATH = CUSTOM_BASE_PATH + CUSTOM_SUB_PATH + "?" + CUSTOM_QUERY_PARAMS;
    private final ObjectMapper mapper = new ObjectMapper();
    private MockWebServer server;


    @Test
    @DisplayName("Verify E2E flow with Data Plane proxies and EDR")
    void httpPullDataTransfer_withEdrAndProxy() {

        var eventsUrl = server.url(PROXIED_PATH);

        var assetId = UUID.randomUUID().toString();
        var authCodeHeaderName = "test-authkey";
        var authCode = "test-authcode";
        PLATO.createAsset(assetId, Json.createObjectBuilder().build(), Json.createObjectBuilder()
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "contentType", "application/json")
                .add(EDC_NAMESPACE + "baseUrl", eventsUrl.toString())
                .add(EDC_NAMESPACE + "authKey", authCodeHeaderName)
                .add(EDC_NAMESPACE + "authCode", authCode)
                .build());

        PLATO.storeBusinessPartner(SOKRATES.getBpn(), "test-group1", "test-group2");
        PLATO.createPolicy(businessPartnerGroupPolicy("policy-1", Operator.IS_ANY_OF, "test-group1"));
        PLATO.createPolicy(businessPartnerGroupPolicy("policy-2", Operator.IS_ALL_OF, "test-group1", "test-group2"));
        PLATO.createContractDefinition(assetId, "def-1", "policy-1", "policy-2");

        var callbacks = Json.createArrayBuilder()
                .add(createCallback(eventsUrl.toString(), true, Set.of("transfer.process.started")))
                .build();

        // response to callback
        server.enqueue(new MockResponse());

        SOKRATES.negotiateEdr(PLATO, assetId, callbacks);

        var transferEvent = waitForTransferCompletion();

        await().pollInterval(ASYNC_POLL_INTERVAL)
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var edrCaches = SOKRATES.getEdrEntriesByAssetId(assetId);
                    assertThat(edrCaches).hasSize(1);
                });
        
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

        var eventsUrl = server.url(PROXIED_PATH);

        var assetId = UUID.randomUUID().toString();
        var authCodeHeaderName = "test-authkey";
        var authCode = "test-authcode";
        PLATO.createAsset(assetId, Json.createObjectBuilder().build(), Json.createObjectBuilder()
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "contentType", "application/json")
                .add(EDC_NAMESPACE + "baseUrl", eventsUrl.toString())
                .add(EDC_NAMESPACE + "authKey", authCodeHeaderName)
                .add(EDC_NAMESPACE + "authCode", authCode)
                .build());

        PLATO.storeBusinessPartner(SOKRATES.getBpn(), "test-group1", "test-group2");
        PLATO.createPolicy(businessPartnerGroupPolicy("policy-1", Operator.NEQ, "forbidden-policy"));
        PLATO.createPolicy(businessPartnerGroupPolicy("policy-2", Operator.EQ, "test-group1", "test-group2"));
        PLATO.createContractDefinition(assetId, "def-1", "policy-1", "policy-2");


        SOKRATES.pullProxyDataResponseByAssetId(PLATO, assetId)
                .then()
                .assertThat().statusCode(400);

    }

    @Test
    @DisplayName("Verify E2E flow with Data Plane proxies and Two EDR")
    void httpPullDataTransfer_shouldFailForAsset_withTwoEdrAndProxy() throws IOException {

        var eventsUrl = server.url(PROXIED_PATH);

        var assetId = UUID.randomUUID().toString();
        var authCodeHeaderName = "test-authkey";
        var authCode = "test-authcode";
        PLATO.createAsset(assetId, Json.createObjectBuilder().build(), Json.createObjectBuilder()
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "contentType", "application/json")
                .add(EDC_NAMESPACE + "baseUrl", eventsUrl.toString())
                .add(EDC_NAMESPACE + "authKey", authCodeHeaderName)
                .add(EDC_NAMESPACE + "authCode", authCode)
                .build());

        PLATO.storeBusinessPartner(SOKRATES.getBpn(), "test-group1", "test-group2");
        PLATO.createPolicy(businessPartnerGroupPolicy("policy-1", Operator.IS_NONE_OF, "forbidden-policy"));
        PLATO.createPolicy(businessPartnerGroupPolicy("policy-2", Operator.IS_ALL_OF, "test-group1", "test-group2"));
        PLATO.createContractDefinition(assetId, "def-1", "policy-1", "policy-2");

        var callbacks = Json.createArrayBuilder()
                .add(createCallback(eventsUrl.toString(), true, Set.of("transfer.process.started")))
                .build();

        // response to callback
        server.enqueue(new MockResponse());
        server.enqueue(new MockResponse());

        SOKRATES.negotiateEdr(PLATO, assetId, callbacks);
        SOKRATES.negotiateEdr(PLATO, assetId, callbacks);

        var transferEvent1 = waitForTransferCompletion();
        var transferEvent2 = waitForTransferCompletion();

        await().pollInterval(ASYNC_POLL_INTERVAL)
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var edrCaches = SOKRATES.getEdrEntriesByAssetId(assetId);
                    assertThat(edrCaches).hasSize(2);
                });


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

    @Test
    @DisplayName("Verify E2E flow with Data Plane provider and EDR")
    void httpPullDataTransfer_withEdrAndProviderDataPlaneProxy() throws IOException {

        var eventsUrl = server.url(PROXIED_PATH);

        var assetId = UUID.randomUUID().toString();
        var authCodeHeaderName = "test-authkey";
        var authCode = "test-authcode";
        PLATO.createAsset(assetId, Json.createObjectBuilder().build(), Json.createObjectBuilder()
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "contentType", "application/json")
                .add(EDC_NAMESPACE + "baseUrl", eventsUrl.toString())
                .add(EDC_NAMESPACE + "authKey", authCodeHeaderName)
                .add(EDC_NAMESPACE + "authCode", authCode)
                .build());

        PLATO.storeBusinessPartner(SOKRATES.getBpn(), "test-group1", "test-group2");
        PLATO.createPolicy(businessPartnerGroupPolicy("policy-1", Operator.IS_ANY_OF, "test-group1"));
        PLATO.createPolicy(businessPartnerGroupPolicy("policy-2", Operator.IS_ALL_OF, "test-group1", "test-group2"));
        PLATO.createContractDefinition(assetId, "def-1", "policy-1", "policy-2");

        var callbacks = Json.createArrayBuilder()
                .add(createCallback(eventsUrl.toString(), true, Set.of("transfer.process.started")))
                .build();

        // response to callback
        server.enqueue(new MockResponse());

        SOKRATES.negotiateEdr(PLATO, assetId, callbacks);

        var transferEvent = waitForTransferCompletion();

        await().pollInterval(ASYNC_POLL_INTERVAL)
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var edrCaches = SOKRATES.getEdrEntriesByAssetId(assetId);
                    assertThat(edrCaches).hasSize(1);
                });

        var body = "{\"response\": \"ok\"}";

        server.enqueue(new MockResponse().setBody(body));
        var data = SOKRATES.pullProviderDataPlaneDataByAssetId(PLATO, assetId);
        assertThat(data).isEqualTo(body);

        server.enqueue(new MockResponse().setBody(body));
        data = SOKRATES.pullProviderDataPlaneDataByTransferProcessId(PLATO, transferEvent.getPayload().getTransferProcessId());
        assertThat(data).isEqualTo(body);
    }

    @Test
    @DisplayName("Verify E2E flow with Data Plane provider and EDR")
    void httpPullDataTransfer_withEdrAndProviderDataPlaneProxyAndCustomProperties() throws IOException {

        var eventsUrl = server.url(PROXIED_PATH);

        var customUrl = server.url(CUSTOM_BASE_PATH);

        var body = "{\"response\": \"ok\"}";

        server.setDispatcher(new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
                return switch (recordedRequest.getPath()) {
                    case PROXIED_PATH -> new MockResponse();
                    case CUSTOM_FULL_PATH -> new MockResponse().setBody(body);
                    default -> new MockResponse().setResponseCode(404);
                };
            }
        });

        var assetId = UUID.randomUUID().toString();
        var authCodeHeaderName = "test-authkey";
        var authCode = "test-authcode";
        PLATO.createAsset(assetId, Json.createObjectBuilder().build(), Json.createObjectBuilder()
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "contentType", "application/json")
                .add(EDC_NAMESPACE + "baseUrl", customUrl.toString())
                .add(EDC_NAMESPACE + "authKey", authCodeHeaderName)
                .add(EDC_NAMESPACE + "authCode", authCode)
                .add(EDC_NAMESPACE + "proxyPath", "true")
                .add(EDC_NAMESPACE + "proxyQueryParams", "true")
                .build());

        PLATO.storeBusinessPartner(SOKRATES.getBpn(), "test-group1", "test-group2");
        PLATO.createPolicy(businessPartnerGroupPolicy("policy-1", Operator.NEQ, "forbidden-policy"));
        PLATO.createPolicy(businessPartnerGroupPolicy("policy-2", Operator.EQ, "test-group1", "test-group2"));
        PLATO.createContractDefinition(assetId, "def-1", "policy-1", "policy-2");

        var callbacks = Json.createArrayBuilder()
                .add(createCallback(eventsUrl.toString(), true, Set.of("transfer.process.started")))
                .build();

        SOKRATES.negotiateEdr(PLATO, assetId, callbacks);

        waitForTransferCompletion();

        await().pollInterval(ASYNC_POLL_INTERVAL)
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var edrCaches = SOKRATES.getEdrEntriesByAssetId(assetId);
                    assertThat(edrCaches).hasSize(1);
                });

        var data = SOKRATES.pullProviderDataPlaneDataByAssetIdAndCustomProperties(PLATO, assetId, CUSTOM_SUB_PATH, CUSTOM_QUERY_PARAMS);
        assertThat(data).isEqualTo(body);

    }

    @BeforeEach
    void setup() throws IOException {
        server = new MockWebServer();
        server.start(PLATO_PROXIED_AAS_BACKEND_PORT);
    }

    @AfterEach
    void teardown() throws IOException {
        server.shutdown();
    }

    private EventEnvelope<TransferProcessStarted> waitForTransferCompletion() {
        try {
            var request = server.takeRequest(60, TimeUnit.SECONDS);
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
