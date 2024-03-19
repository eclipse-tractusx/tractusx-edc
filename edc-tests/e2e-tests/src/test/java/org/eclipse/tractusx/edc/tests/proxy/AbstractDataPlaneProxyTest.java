/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
import org.eclipse.tractusx.edc.lifecycle.tx.TxParticipant;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.tractusx.edc.helpers.EdrNegotiationHelperFunctions.createCallback;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.bpnGroupPolicy;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.eclipse.tractusx.edc.tests.TestCommon.ASYNC_POLL_INTERVAL;
import static org.eclipse.tractusx.edc.tests.TestCommon.ASYNC_TIMEOUT;

public abstract class AbstractDataPlaneProxyTest {

    protected static final TxParticipant SOKRATES = TxParticipant.Builder.newInstance()
            .name(SOKRATES_NAME)
            .id(SOKRATES_BPN)
            .build();

    protected static final TxParticipant PLATO = TxParticipant.Builder.newInstance()
            .name(PLATO_NAME)
            .id(PLATO_BPN)
            .build();
    private static final String CUSTOM_BASE_PATH = "/custom";
    private static final String CUSTOM_SUB_PATH = "/sub";
    private static final String CUSTOM_QUERY_PARAMS = "foo=bar";
    private static final String CUSTOM_FULL_PATH = CUSTOM_BASE_PATH + CUSTOM_SUB_PATH + "?" + CUSTOM_QUERY_PARAMS;
    private final ObjectMapper mapper = new ObjectMapper();
    private MockWebServer server;

    @NotNull
    private static Map<String, Object> dataAddress(String url) {
        return Map.of(
                "baseUrl", url,
                "type", "HttpData",
                "contentType", "application/json",
                "authKey", "test-authkey",
                "authCode", "test-authcode",
                "proxyPath", "true",
                "proxyQueryParams", "true"
        );
    }


    @Test
    @DisplayName("Verify E2E flow with Data Plane proxies fails when EDR is not found")
    void httpPullDataTransfer_withoutEdr() throws IOException {

        var eventsUrl = server.url(PLATO.backendProviderProxy().getPath());
        var assetId = UUID.randomUUID().toString();

        PLATO.createAsset(assetId, Map.of(), dataAddress(eventsUrl.url().toString()));


        PLATO.storeBusinessPartner(SOKRATES.getBpn(), "test-group1", "test-group2");
        var accessPolicy = PLATO.createPolicyDefinition(bpnGroupPolicy(Operator.NEQ, "forbidden-policy"));
        var contractPolicy = PLATO.createPolicyDefinition(bpnGroupPolicy(Operator.EQ, "test-group1", "test-group2"));
        PLATO.createContractDefinition(assetId, "def-1", accessPolicy, contractPolicy);


        SOKRATES.data().pullProxyDataResponseByAssetId(PLATO, assetId)
                .then()
                .assertThat().statusCode(400);

    }

    @Test
    @DisplayName("Verify E2E flow with Data Plane provider and EDR")
    void httpPullDataTransfer_withEdrAndProviderDataPlaneProxy() throws IOException {

        var eventsUrl = server.url(PLATO.backendProviderProxy().getPath());
        var assetId = UUID.randomUUID().toString();
        PLATO.createAsset(assetId, Map.of(), dataAddress(eventsUrl.url().toString()));

        PLATO.storeBusinessPartner(SOKRATES.getBpn(), "test-group1", "test-group2");
        var accessPolicy = PLATO.createPolicyDefinition(bpnGroupPolicy(Operator.IS_ANY_OF, "test-group1"));
        var contractPolicy = PLATO.createPolicyDefinition(bpnGroupPolicy(Operator.IS_ALL_OF, "test-group1", "test-group2"));
        PLATO.createContractDefinition(assetId, "def-1", accessPolicy, contractPolicy);

        var callbacks = Json.createArrayBuilder()
                .add(createCallback(eventsUrl.toString(), true, Set.of("transfer.process.started")))
                .build();

        // response to callback
        server.enqueue(new MockResponse());

        SOKRATES.edrs().negotiateEdr(PLATO, assetId, callbacks);

        var transferEvent = waitForTransferCompletion();

        await().pollInterval(ASYNC_POLL_INTERVAL)
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var edrCaches = SOKRATES.edrs().getEdrEntriesByAssetId(assetId);
                    assertThat(edrCaches).hasSize(1);
                });

        var body = "{\"response\": \"ok\"}";

        server.enqueue(new MockResponse().setBody(body));
        var data = SOKRATES.data().pullProviderDataPlaneDataByAssetId(PLATO, assetId);
        assertThat(data).isEqualTo(body);

        server.enqueue(new MockResponse().setBody(body));
        data = SOKRATES.data().pullProviderDataPlaneDataByTransferProcessId(PLATO, transferEvent.getPayload().getTransferProcessId());
        assertThat(data).isEqualTo(body);
    }

    @Test
    @DisplayName("Verify E2E flow with Data Plane provider and EDR")
    void httpPullDataTransfer_withEdrAndProviderDataPlaneProxyAndCustomProperties() throws IOException {

        var eventsPath = PLATO.backendProviderProxy().getPath();
        var eventsUrl = server.url(eventsPath);

        var customUrl = server.url(CUSTOM_BASE_PATH);

        var body = "{\"response\": \"ok\"}";

        server.setDispatcher(new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
                var path = recordedRequest.getPath();
                if (PLATO.backendProviderProxy().getPath().equals(path)) {
                    return new MockResponse();
                } else if (CUSTOM_FULL_PATH.equals(path)) {
                    return new MockResponse().setBody(body);
                } else {
                    return new MockResponse().setResponseCode(404);
                }
            }
        });

        var assetId = UUID.randomUUID().toString();
        PLATO.createAsset(assetId, Map.of(), dataAddress(customUrl.url().toString()));

        PLATO.storeBusinessPartner(SOKRATES.getBpn(), "test-group1", "test-group2");
        var accessPolicy = PLATO.createPolicyDefinition(bpnGroupPolicy(Operator.NEQ, "forbidden-policy"));
        var contractPolicy = PLATO.createPolicyDefinition(bpnGroupPolicy(Operator.EQ, "test-group1", "test-group2"));
        PLATO.createContractDefinition(assetId, "def-1", accessPolicy, contractPolicy);

        var callbacks = Json.createArrayBuilder()
                .add(createCallback(eventsUrl.toString(), true, Set.of("transfer.process.started")))
                .build();

        SOKRATES.edrs().negotiateEdr(PLATO, assetId, callbacks);

        waitForTransferCompletion();

        await().pollInterval(ASYNC_POLL_INTERVAL)
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var edrCaches = SOKRATES.edrs().getEdrEntriesByAssetId(assetId);
                    assertThat(edrCaches).hasSize(1);
                });

        var data = SOKRATES.data().pullProviderDataPlaneDataByAssetIdAndCustomProperties(PLATO, assetId, CUSTOM_SUB_PATH, CUSTOM_QUERY_PARAMS);
        assertThat(data).isEqualTo(body);

    }

    @BeforeEach
    void setup() throws IOException {
        server = new MockWebServer();
        server.start(PLATO.backendProviderProxy().getPort());
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
