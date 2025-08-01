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
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.dataplane.spi.DataFlowStates;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.tractusx.edc.tests.ParticipantAwareTest;
import org.eclipse.tractusx.edc.tests.RuntimeAwareTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static jakarta.json.Json.createObjectBuilder;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.bpnPolicy;
import static org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase.ASYNC_TIMEOUT;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.verify.VerificationTimes.exactly;

/**
 * Base tests for Provider PUSH scenario
 */
public abstract class ProviderPushBaseTest implements ParticipantAwareTest, RuntimeAwareTest {

    public static final String MOCK_BACKEND_REMOTE_HOST = "localhost";
    public static final String MOCK_BACKEND_SOURCE_PATH = "/mock/api/provider";
    public static final String MOCK_BACKEND_DESTINATION_PATH = "/mock/api/consumer";
    public static final Duration POLL_DELAY = ofSeconds(3);

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

        await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> transferProcessIsInState(transferProcessId, COMPLETED));
        server.verify(request().withPath(MOCK_BACKEND_SOURCE_PATH));
        server.verify(request().withPath(MOCK_BACKEND_DESTINATION_PATH));
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
        var consumerTransferProcessId = consumer()
                .requestAssetFrom(assetId, provider())
                .withDestination(destination)
                .withTransferType("HttpData-PUSH")
                .execute();

        consumer().awaitTransferToBeInState(consumerTransferProcessId, TransferProcessStates.STARTED);
        var providerTransferProcessId = consumer().getTransferProcessField(consumerTransferProcessId, "correlationId");

        // Reassert after 3 seconds
        waitAndAssert(
                POLL_DELAY,
                () -> transferProcessIsInState(consumerTransferProcessId, TransferProcessStates.STARTED),
                () -> dataFlowIsInState(providerTransferProcessId, DataFlowStates.STARTED));
        server.verify(request().withPath(MOCK_BACKEND_SOURCE_PATH));
        server.verify(request().withPath(MOCK_BACKEND_DESTINATION_PATH));

        provider().triggerDataTransfer(providerTransferProcessId);

        waitAndAssert(
                POLL_DELAY,
                () -> transferProcessIsInState(consumerTransferProcessId, TransferProcessStates.STARTED),
                () -> dataFlowIsInState(providerTransferProcessId, DataFlowStates.STARTED));
        server.verify(request().withPath(MOCK_BACKEND_SOURCE_PATH), exactly(2));
        server.verify(request().withPath(MOCK_BACKEND_DESTINATION_PATH), exactly(2));

        consumer().terminateTransfer(consumerTransferProcessId);
        consumer().awaitTransferToBeInState(consumerTransferProcessId, TransferProcessStates.TERMINATED);
        await().atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> dataFlowIsInState(providerTransferProcessId, DataFlowStates.TERMINATED));
    }

    @AfterEach
    void teardown() {
        server.stop();
    }

    private void waitAndAssert(Duration duration, Runnable... assertions) {
        await().pollDelay(duration).atMost(ASYNC_TIMEOUT).untilAsserted(() -> {
            Stream.of(assertions).forEach(Runnable::run);
        });
    }

    private void transferProcessIsInState(String transferProcessId, TransferProcessStates state) {
        assertThat(consumer().getTransferProcessState(transferProcessId)).isEqualTo(state.name());
    }

    private void dataFlowIsInState(String dataFlowId, DataFlowStates state) {
        var dataflow = providerRuntime().getService(DataPlaneStore.class).findById(dataFlowId);
        assertThat(dataflow.getState()).isEqualTo(state.code());
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
