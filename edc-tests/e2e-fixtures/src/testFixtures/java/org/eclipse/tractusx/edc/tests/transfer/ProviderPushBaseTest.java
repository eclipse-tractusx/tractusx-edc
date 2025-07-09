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

package org.eclipse.tractusx.edc.tests.transfer;

import jakarta.json.JsonObject;
import org.eclipse.tractusx.edc.tests.ParticipantAwareTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.util.Map;
import java.util.UUID;

import static jakarta.json.Json.createObjectBuilder;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.bpnPolicy;
import static org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase.ASYNC_POLL_INTERVAL;
import static org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase.ASYNC_TIMEOUT;
import static org.mockserver.model.HttpRequest.request;

/**
 * Base tests for Provider PUSH scenario
 */
public abstract class ProviderPushBaseTest implements ParticipantAwareTest {

    public static final String MOCK_BACKEND_REMOTE_HOST = "localhost";
    public static final String MOCK_BACKEND_SOURCE_PATH = "/mock/api/provider";
    public static final String MOCK_BACKEND_DESTINATION_PATH = "/mock/api/consumer";

    private ClientAndServer server;

    @BeforeEach
    void setup() {
        server = ClientAndServer.startClientAndServer(MOCK_BACKEND_REMOTE_HOST, getFreePort());
    }

    @Test
    void httpPushDataTransfer() {
        var sourceUrl = createMockHttpDataUrl(MOCK_BACKEND_SOURCE_PATH);
        var destinationUrl = createMockHttpDataUrl(MOCK_BACKEND_DESTINATION_PATH);

        var assetId = UUID.randomUUID().toString();
        Map<String, Object> dataAddress = Map.of(
                "name", "transfer-test",
                "baseUrl", sourceUrl,
                "type", "HttpData",
                "contentType", "application/json");
        provider().createAsset(assetId, Map.of(), dataAddress);
        var policyId = provider().createPolicyDefinition(bpnPolicy(consumer().getBpn()));
        provider().createContractDefinition(assetId, "def-1", policyId, policyId);

        var destination = httpDataAddress(destinationUrl);
        var transferProcessId = consumer()
                .requestAssetFrom(assetId, provider())
                .withDestination(destination)
                .withTransferType("HttpData-PUSH")
                .execute();

        await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> {
            var state = consumer().getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(COMPLETED.name());
        });
    }

    @Test
    void httpPushNonFiniteDataTransfer() {
        var sourceUrl = createMockHttpDataUrl(MOCK_BACKEND_SOURCE_PATH);
        var destinationUrl = createMockHttpDataUrl(MOCK_BACKEND_DESTINATION_PATH);

        var assetId = UUID.randomUUID().toString();
        Map<String, Object> dataAddress = Map.of(
                "name", "transfer-test",
                "baseUrl", sourceUrl,
                "type", "HttpData",
                "contentType", "application/json",
                "isNonFinite", "true");
        provider().createAsset(assetId, Map.of(), dataAddress);
        var policyId = provider().createPolicyDefinition(bpnPolicy(consumer().getBpn()));
        provider().createContractDefinition(assetId, "def-1", policyId, policyId);

        var destination = httpDataAddress(destinationUrl);
        var transferProcessId = consumer()
                .requestAssetFrom(assetId, provider())
                .withDestination(destination)
                .withTransferType("HttpData-PUSH")
                .execute();

        consumer().awaitTransferToBeInState(transferProcessId, STARTED);

        // Reassert after 3 seconds
        await().pollDelay(3, SECONDS).atMost(ASYNC_TIMEOUT).untilAsserted(() -> {
            var state = consumer().getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(STARTED.name());
        });

        consumer().terminateTransfer(transferProcessId);
        consumer().awaitTransferToBeInState(transferProcessId, TERMINATED);
    }

    @AfterEach
    void teardown() {
        server.stop();
    }

    private String createMockHttpDataUrl(String path) {
        server.when(request().withPath(path))
                .respond(HttpResponse.response().withStatusCode(200));
        return "http://%s:%d%s".formatted(MOCK_BACKEND_REMOTE_HOST, server.getPort(), path);
    }

    private JsonObject httpDataAddress(String baseUrl) {
        return createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "properties", createObjectBuilder()
                        .add(EDC_NAMESPACE + "baseUrl", baseUrl)
                        .build())
                .build();
    }

}
