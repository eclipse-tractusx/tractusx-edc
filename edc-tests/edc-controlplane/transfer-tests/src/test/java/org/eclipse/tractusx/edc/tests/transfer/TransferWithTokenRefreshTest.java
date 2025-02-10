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
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.verify.VerificationTimes;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.bnpPolicy;
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
    protected static final TransferParticipant CONSUMER = TransferParticipant.Builder.newInstance()
            .name(CONSUMER_NAME)
            .id(CONSUMER_BPN)
            .build();
    protected static final TransferParticipant PROVIDER = TransferParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_BPN)
            .build();

    @RegisterExtension
    protected static final RuntimeExtension CONSUMER_RUNTIME = memoryRuntime(CONSUMER.getName(), CONSUMER.getBpn(), CONSUMER.getConfiguration());
    private static final Long VERY_SHORT_TOKEN_EXPIRY = 3L;

    @RegisterExtension
    protected static final RuntimeExtension PROVIDER_RUNTIME = memoryRuntime(PROVIDER.getName(), PROVIDER.getBpn(), forConfig(PROVIDER.getConfiguration()))
            .registerServiceMock(BdrsClient.class, (c) -> CONSUMER.getDid());
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

        PROVIDER.createAsset(assetId, Map.of(), dataAddress);

        var accessPolicyId = PROVIDER.createPolicyDefinition(createAccessPolicy(CONSUMER.getBpn()));
        var contractPolicyId = PROVIDER.createPolicyDefinition(createContractPolicy(CONSUMER.getBpn()));
        PROVIDER.createContractDefinition(assetId, "def-1", accessPolicyId, contractPolicyId);
        var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER).withTransferType("HttpData-PULL")
                .withDestination(httpDataDestination()).execute();


        CONSUMER.waitForTransferProcess(transferProcessId, TransferProcessStates.STARTED);

        // wait until EDC is available on the consumer side
        server.when(request().withMethod("GET").withPath(MOCK_BACKEND_PATH)).respond(response().withStatusCode(200).withBody("test response"));

        var edr = CONSUMER.edrs().waitForEdr(transferProcessId);


        // wait until the EDR expires
        await().pollDelay(Duration.ofSeconds(VERY_SHORT_TOKEN_EXPIRY + 1))
                .pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var endpoint = edr.getString("endpoint");
                    var token = edr.getString("authorization");
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
        var renewedEdr = CONSUMER.edrs().refreshEdr(transferProcessId)
                .statusCode(200)
                .extract().body()
                .as(JsonObject.class);

        // make sure the consumer has now been able to fetch the data.
        var data = CONSUMER.data().pullData(renewedEdr, Map.of());
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

        PROVIDER.createAsset(assetId, Map.of(), dataAddress);

        var accessPolicyId = PROVIDER.createPolicyDefinition(createAccessPolicy(CONSUMER.getBpn()));
        var contractPolicyId = PROVIDER.createPolicyDefinition(createContractPolicy(CONSUMER.getBpn()));
        PROVIDER.createContractDefinition(assetId, "def-1", accessPolicyId, contractPolicyId);
        var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER).withTransferType("HttpData-PULL")
                .withDestination(httpDataDestination()).execute();

        CONSUMER.waitForTransferProcess(transferProcessId, TransferProcessStates.STARTED);

        // wait until EDC is available on the consumer side
        server.when(request().withMethod("GET").withPath(MOCK_BACKEND_PATH)).respond(response().withStatusCode(200).withBody("test response"));

        var edr = CONSUMER.edrs().waitForEdr(transferProcessId);

        // wait until the EDR expires
        await().pollDelay(Duration.ofSeconds(VERY_SHORT_TOKEN_EXPIRY + 1))
                .pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var endpoint = edr.getString("endpoint");
                    var token = edr.getString("authorization");
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
        var renewedEdr = CONSUMER.edrs().getEdrWithRefresh(transferProcessId, true)
                .statusCode(200)
                .extract().body()
                .as(JsonObject.class);

        // make sure the consumer has now been able to fetch the data.
        var data = CONSUMER.data().pullData(renewedEdr, Map.of());
        assertThat(data).isNotNull().isEqualTo("test response");

        server.verify(request().withPath(MOCK_BACKEND_PATH), VerificationTimes.exactly(1));
    }

    private JsonObject httpDataDestination() {
        return createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "properties", createObjectBuilder()
                        .add(EDC_NAMESPACE + "baseUrl", "http://localhost:8080")
                        .build())
                .build();
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
