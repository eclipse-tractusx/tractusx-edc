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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.ParticipantRuntime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.verify.VerificationTimes;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PLATO_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.bnpPolicy;
import static org.eclipse.tractusx.edc.tests.helpers.TransferProcessHelperFunctions.createProxyRequest;
import static org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase.ASYNC_TIMEOUT;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.memoryRuntime;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * this test uses in-mem runtimes to negotiate and perform a data transfer, but the EDR expires before the consumer has a
 * chance to obtain the data.
 * The test asserts that the automatic token refresh mechanism renews the token, and the transfer can be completed afterward.
 */
@EndToEndTest
public class TransferWithTokenRefreshTest {

    public static final String MOCK_BACKEND_REMOTE_HOST = "localhost";
    public static final String MOCK_BACKEND_PATH = "/mock/api";
    protected static final TransferParticipant SOKRATES = TransferParticipant.Builder.newInstance()
            .name(SOKRATES_NAME)
            .id(SOKRATES_BPN)
            .build();
    protected static final TransferParticipant PLATO = TransferParticipant.Builder.newInstance()
            .name(PLATO_NAME)
            .id(PLATO_BPN)
            .build();

    @RegisterExtension
    protected static final ParticipantRuntime SOKRATES_RUNTIME = memoryRuntime(SOKRATES.getName(), SOKRATES.getBpn(), SOKRATES.getConfiguration());
    private static final Long VERY_SHORT_TOKEN_EXPIRY = 3L;

    @RegisterExtension
    protected static final ParticipantRuntime PLATO_RUNTIME = memoryRuntime(PLATO.getName(), PLATO.getBpn(), forConfig(PLATO.getConfiguration()));
    protected ClientAndServer server;
    private String privateBackendUrl;


    private static Map<String, String> forConfig(Map<String, String> originalConfig) {
        var newConfig = new HashMap<>(originalConfig);
        newConfig.put("edc.dataplane.token.expiry", String.valueOf(VERY_SHORT_TOKEN_EXPIRY));
        newConfig.put("edc.dataplane.token.expiry.tolerance", "0");
        return newConfig;
    }

    @BeforeEach
    void setup() {
        server = ClientAndServer.startClientAndServer(MOCK_BACKEND_REMOTE_HOST, getFreePort());
        privateBackendUrl = "http://%s:%d%s".formatted(MOCK_BACKEND_REMOTE_HOST, server.getPort(), MOCK_BACKEND_PATH);
    }

    @Test
    void transferData_withExpiredEdr_shouldReturn4xx() {
        var assetId = "api-asset-1";

        Map<String, Object> dataAddress = Map.of(
                "baseUrl", privateBackendUrl,
                "type", "HttpData",
                "contentType", "application/json"
        );

        PLATO.createAsset(assetId, Map.of(), dataAddress);

        var accessPolicyId = PLATO.createPolicyDefinition(createAccessPolicy(SOKRATES.getBpn()));
        var contractPolicyId = PLATO.createPolicyDefinition(createContractPolicy(SOKRATES.getBpn()));
        PLATO.createContractDefinition(assetId, "def-1", accessPolicyId, contractPolicyId);
        var transferProcessId = SOKRATES.requestAsset(PLATO, assetId, Json.createObjectBuilder().build(), createProxyRequest(), "HttpData-PULL");

        var edr = new AtomicReference<JsonObject>();

        // wait until transfer process completes
        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var tpState = SOKRATES.getTransferProcessState(transferProcessId);
                    assertThat(tpState).isNotNull().isEqualTo(TransferProcessStates.STARTED.toString());
                });

        // wait until EDC is available on the consumer side
        server.when(request().withMethod("GET").withPath(MOCK_BACKEND_PATH)).respond(response().withStatusCode(200).withBody("test response"));
        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    edr.set(SOKRATES.edrs().getEdr(transferProcessId));
                    assertThat(edr).isNotNull();
                });


        // wait until the EDR expires
        await().pollDelay(Duration.ofSeconds(VERY_SHORT_TOKEN_EXPIRY + 1))
                .pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var endpoint = edr.get().getString("endpoint");
                    var token = edr.get().getString("authorization");
                    given()
                            .baseUri(endpoint)
                            .header("Authorization", token)
                            .when()
                            .get()
                            .then()
                            .statusCode(403);
                });

        // assert the data has not been fetched
        server.verify(request().withPath(MOCK_BACKEND_PATH), VerificationTimes.never());

        // renew EDR explicitly
        var renewedEdr = SOKRATES.edrs().refreshEdr(transferProcessId)
                .statusCode(200)
                .extract().body()
                .as(JsonObject.class);

        // make sure the consumer has now been able to fetch the data.
        var data = SOKRATES.data().pullData(renewedEdr, Map.of());
        assertThat(data).isNotNull().isEqualTo("test response");

        server.verify(request().withPath(MOCK_BACKEND_PATH), VerificationTimes.exactly(1));
    }

    @Test
    void transferData_withAutomaticRefresh() {
        var assetId = "api-asset-1";

        Map<String, Object> dataAddress = Map.of(
                "baseUrl", privateBackendUrl,
                "type", "HttpData",
                "contentType", "application/json"
        );

        PLATO.createAsset(assetId, Map.of(), dataAddress);

        var accessPolicyId = PLATO.createPolicyDefinition(createAccessPolicy(SOKRATES.getBpn()));
        var contractPolicyId = PLATO.createPolicyDefinition(createContractPolicy(SOKRATES.getBpn()));
        PLATO.createContractDefinition(assetId, "def-1", accessPolicyId, contractPolicyId);
        var transferProcessId = SOKRATES.requestAsset(PLATO, assetId, Json.createObjectBuilder().build(), createProxyRequest(), "HttpData-PULL");

        var edr = new AtomicReference<JsonObject>();

        // wait until transfer process completes
        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var tpState = SOKRATES.getTransferProcessState(transferProcessId);
                    assertThat(tpState).isNotNull().isEqualTo(TransferProcessStates.STARTED.toString());
                });

        // wait until EDC is available on the consumer side
        server.when(request().withMethod("GET").withPath(MOCK_BACKEND_PATH)).respond(response().withStatusCode(200).withBody("test response"));
        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    edr.set(SOKRATES.edrs().getEdr(transferProcessId));
                    assertThat(edr).isNotNull();
                });


        // wait until the EDR expires
        await().pollDelay(Duration.ofSeconds(VERY_SHORT_TOKEN_EXPIRY + 1))
                .pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var endpoint = edr.get().getString("endpoint");
                    var token = edr.get().getString("authorization");
                    given()
                            .baseUri(endpoint)
                            .header("Authorization", token)
                            .when()
                            .get()
                            .then()
                            .statusCode(403);
                });

        // assert the data has not been fetched
        server.verify(request().withPath(MOCK_BACKEND_PATH), VerificationTimes.never());

        // get EDR with automatic refresh
        var renewedEdr = SOKRATES.edrs().getEdrWithRefresh(transferProcessId, true)
                .statusCode(200)
                .extract().body()
                .as(JsonObject.class);

        // make sure the consumer has now been able to fetch the data.
        var data = SOKRATES.data().pullData(renewedEdr, Map.of());
        assertThat(data).isNotNull().isEqualTo("test response");

        server.verify(request().withPath(MOCK_BACKEND_PATH), VerificationTimes.exactly(1));
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
