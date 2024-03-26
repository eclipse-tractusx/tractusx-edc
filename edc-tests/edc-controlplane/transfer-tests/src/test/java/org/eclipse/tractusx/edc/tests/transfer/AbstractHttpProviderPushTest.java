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

package org.eclipse.tractusx.edc.tests.transfer;

import jakarta.json.JsonObject;
import org.eclipse.tractusx.edc.tests.TxParticipant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.util.Map;
import java.util.UUID;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PLATO_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.eclipse.tractusx.edc.tests.TxParticipant.ASYNC_TIMEOUT;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.bnpPolicy;
import static org.mockserver.model.HttpRequest.request;

public abstract class AbstractHttpProviderPushTest {
    public static final String MOCK_BACKEND_REMOTEHOST = "localhost";
    protected static final TxParticipant SOKRATES = TxParticipant.Builder.newInstance()
            .name(SOKRATES_NAME)
            .id(SOKRATES_BPN)
            .build();

    protected static final TxParticipant PLATO = TxParticipant.Builder.newInstance()
            .name(PLATO_NAME)
            .id(PLATO_BPN)
            .build();

    private ClientAndServer server;

    @BeforeEach
    void setup() {
        server = ClientAndServer.startClientAndServer(MOCK_BACKEND_REMOTEHOST, getFreePort());
    }

    @Test
    void httpPushDataTransfer() {
        var assetId = UUID.randomUUID().toString();

        var providerUrl = "http://%s:%d%s".formatted(MOCK_BACKEND_REMOTEHOST, server.getPort(), "/mock/api/provider");
        var consumerUrl = "http://%s:%d%s".formatted(MOCK_BACKEND_REMOTEHOST, server.getPort(), "/mock/api/consumer");

        server.when(request().withPath("/mock/api/provider"))
                .respond(HttpResponse.response().withStatusCode(200));
        server.when(request().withPath("/mock/api/consumer"))
                .respond(HttpResponse.response().withStatusCode(200));

        Map<String, Object> dataAddress = Map.of(
                "name", "transfer-test",
                "baseUrl", providerUrl,
                "type", "HttpData",
                "contentType", "application/json"
        );

        PLATO.createAsset(assetId, Map.of(), dataAddress);
        var policyId = PLATO.createPolicyDefinition(bnpPolicy(SOKRATES.getBpn()));
        PLATO.createContractDefinition(assetId, "def-1", policyId, policyId);

        var destination = httpDataAddress(consumerUrl);

        var transferProcessId = SOKRATES.requestAsset(PLATO, assetId, createObjectBuilder().build(), destination, "HttpData-PUSH");
        await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> {
            var state = SOKRATES.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(COMPLETED.name());
        });
    }

    @AfterEach
    void teardown() {
        server.stop();
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
