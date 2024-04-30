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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.tractusx.edc.tests.ParticipantAwareTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.verify.VerificationTimes;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.bnpPolicy;
import static org.eclipse.tractusx.edc.tests.helpers.TransferProcessHelperFunctions.createProxyRequest;
import static org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase.ASYNC_TIMEOUT;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * Base tests for Http PULL scenario
 */
public abstract class HttpConsumerPullBaseTest implements ParticipantAwareTest {

    public static final String MOCK_BACKEND_REMOTE_HOST = "localhost";
    public static final String MOCK_BACKEND_PATH = "/mock/api";
    protected ClientAndServer server;

    protected String privateBackendUrl;


    @BeforeEach
    void setup() {
        server = ClientAndServer.startClientAndServer(MOCK_BACKEND_REMOTE_HOST, getFreePort());
        privateBackendUrl = "http://%s:%d%s".formatted(MOCK_BACKEND_REMOTE_HOST, server.getPort(), MOCK_BACKEND_PATH);
    }

    @Test
    void transferData_privateBackend() {
        var assetId = "api-asset-1";


        Map<String, Object> dataAddress = Map.of(
                "baseUrl", privateBackendUrl,
                "type", "HttpData",
                "contentType", "application/json"
        );

        plato().createAsset(assetId, Map.of(), dataAddress);

        var accessPolicyId = plato().createPolicyDefinition(createAccessPolicy(sokrates().getBpn()));
        var contractPolicyId = plato().createPolicyDefinition(createContractPolicy(sokrates().getBpn()));
        plato().createContractDefinition(assetId, "def-1", accessPolicyId, contractPolicyId);
        var transferProcessId = sokrates().requestAsset(plato(), assetId, Json.createObjectBuilder().build(), createProxyRequest(), "HttpData-PULL");

        var edr = new AtomicReference<JsonObject>();

        // wait until transfer process completes
        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var tpState = sokrates().getTransferProcessState(transferProcessId);
                    assertThat(tpState).isNotNull().isEqualTo(TransferProcessStates.STARTED.toString());
                });

        // wait until EDC is available on the consumer side
        server.when(request().withMethod("GET").withPath(MOCK_BACKEND_PATH)).respond(response().withStatusCode(200).withBody("test response"));
        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    edr.set(sokrates().edrs().getEdr(transferProcessId));
                    assertThat(edr).isNotNull();
                });

        // pull data out of provider's backend service:
        // Prov-DP -> Prov-backend
        assertThat(sokrates().data().pullData(edr.get(), Map.of())).isEqualTo("test response");

        server.verify(request()
                .withPath(MOCK_BACKEND_PATH)
                .withHeader("Edc-Contract-Agreement-Id")
                .withHeader("Edc-Bpn", sokrates().getBpn())
                .withMethod("GET"), VerificationTimes.exactly(1));

    }

    @Test
    void transferData_privateBackend_withConsumerDataPlane() {
        var assetId = "api-asset-1";


        Map<String, Object> dataAddress = Map.of(
                "baseUrl", privateBackendUrl,
                "type", "HttpData",
                "contentType", "application/json"
        );

        plato().createAsset(assetId, Map.of(), dataAddress);

        var accessPolicyId = plato().createPolicyDefinition(createAccessPolicy(sokrates().getBpn()));
        var contractPolicyId = plato().createPolicyDefinition(createContractPolicy(sokrates().getBpn()));
        plato().createContractDefinition(assetId, "def-1", accessPolicyId, contractPolicyId);
        var transferProcessId = sokrates().requestAsset(plato(), assetId, Json.createObjectBuilder().build(), createProxyRequest(), "HttpData-PULL");

        var edr = new AtomicReference<JsonObject>();

        // wait until transfer process completes
        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var tpState = sokrates().getTransferProcessState(transferProcessId);
                    assertThat(tpState).isNotNull().isEqualTo(TransferProcessStates.STARTED.toString());
                });

        // wait until EDC is available on the consumer side
        server.when(request().withMethod("GET").withPath(MOCK_BACKEND_PATH)).respond(response().withStatusCode(200).withBody("test response"));
        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    edr.set(sokrates().edrs().getEdr(transferProcessId));
                    assertThat(edr).isNotNull();
                });


        // pull data out of provider's backend service:
        //Consumer-DP -> Prov-DP -> Prov-backend
        assertThat(sokrates().dataPlane().pullData(Map.of("transferProcessId", transferProcessId))).isEqualTo("test response");

        server.verify(request()
                .withPath(MOCK_BACKEND_PATH)
                .withHeader("Edc-Contract-Agreement-Id")
                .withHeader("Edc-Bpn", sokrates().getBpn())
                .withMethod("GET"), VerificationTimes.exactly(1));
    }
    
    @AfterEach
    void teardown() {
        server.stop();
    }

    protected JsonObject createAccessPolicy(String bpn) {
        return bnpPolicy(bpn);
    }

    protected JsonObject createContractPolicy(String bpn) {
        return bnpPolicy(bpn);
    }
}
