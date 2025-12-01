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

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.tractusx.edc.tests.ParticipantAwareTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.bpnPolicy;
import static org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase.ASYNC_TIMEOUT;

/**
 * Base tests for Http PULL scenario
 */
public abstract class ConsumerPullBaseTest implements ParticipantAwareTest {

    public static final String MOCK_BACKEND_REMOTE_HOST = "localhost";
    public static final String MOCK_BACKEND_PATH = "/mock/api";
    @RegisterExtension
    protected static WireMockExtension server = WireMockExtension.newInstance()
            .options(wireMockConfig().bindAddress(MOCK_BACKEND_REMOTE_HOST).dynamicPort())
            .build();

    protected String privateBackendUrl;

    @BeforeEach
    void setup() {
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

        provider().createAsset(assetId, Map.of(), dataAddress);

        var accessPolicyId = provider().createPolicyDefinition(createAccessPolicy(consumer().getBpn()));
        var contractPolicyId = provider().createPolicyDefinition(createContractPolicy(consumer().getBpn()));
        provider().createContractDefinition(assetId, "def-1", accessPolicyId, contractPolicyId);
        var transferProcessId = consumer().requestAssetFrom(assetId, provider()).withTransferType("HttpData-PULL")
                .withDestination(httpDataDestination()).execute();

        var edr = new AtomicReference<JsonObject>();

        // wait until transfer process completes
        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var tpState = consumer().getTransferProcessState(transferProcessId);
                    assertThat(tpState).isNotNull().isEqualTo(TransferProcessStates.STARTED.toString());
                });

        // wait until EDC is available on the consumer side
        server.stubFor(get(MOCK_BACKEND_PATH).willReturn(ok("test response")));
        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    edr.set(consumer().edrs().getEdr(transferProcessId));
                    assertThat(edr).isNotNull();
                });

        // pull data out of provider's backend service:
        // Prov-DP -> Prov-backend
        assertThat(consumer().data().pullData(edr.get(), Map.of())).isEqualTo("test response");

        server.verify(1, getRequestedFor(urlPathEqualTo(MOCK_BACKEND_PATH))
                .withHeader("Edc-Bpn", equalTo(consumer().getBpn()))
                .withHeader("Edc-Contract-Agreement-Id", matching(".+")));
    }

    @Test
    void transferData_privateBackend_withConsumerDataPlane() {
        var assetId = "api-asset-1";

        Map<String, Object> dataAddress = Map.of(
                "baseUrl", privateBackendUrl,
                "type", "HttpData",
                "contentType", "application/json"
        );

        provider().createAsset(assetId, Map.of(), dataAddress);

        var accessPolicyId = provider().createPolicyDefinition(createAccessPolicy(consumer().getBpn()));
        var contractPolicyId = provider().createPolicyDefinition(createContractPolicy(consumer().getBpn()));
        provider().createContractDefinition(assetId, "def-1", accessPolicyId, contractPolicyId);
        var transferProcessId = consumer().requestAssetFrom(assetId, provider()).withTransferType("HttpData-PULL")
                .withDestination(httpDataDestination()).execute();

        var edr = new AtomicReference<JsonObject>();

        // wait until transfer process completes
        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var tpState = consumer().getTransferProcessState(transferProcessId);
                    assertThat(tpState).isNotNull().isEqualTo(TransferProcessStates.STARTED.toString());
                });

        // wait until EDC is available on the consumer side
        server.stubFor(get(MOCK_BACKEND_PATH).willReturn(ok("test response")));
        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    edr.set(consumer().edrs().getEdr(transferProcessId));
                    assertThat(edr).isNotNull();
                });


        // pull data out of provider's backend service:
        //Consumer-DP -> Prov-DP -> Prov-backend
        assertThat(consumer().dataPlane().pullData(Map.of("transferProcessId", transferProcessId))).isEqualTo("test response");

        server.verify(1, getRequestedFor(urlPathEqualTo(MOCK_BACKEND_PATH)).withHeader("Edc-Bpn", equalTo(consumer().getBpn())).withHeader("Edc-Contract-Agreement-Id", matching(".+")));
    }

    protected JsonObject httpDataDestination() {
        return createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "properties", createObjectBuilder()
                        .add(EDC_NAMESPACE + "baseUrl", "http://localhost:8080")
                        .build())
                .build();
    }

    protected JsonObject createAccessPolicy(String bpn) {
        return bpnPolicy(bpn);
    }

    protected JsonObject createContractPolicy(String bpn) {
        return bpnPolicy(bpn);
    }
}
