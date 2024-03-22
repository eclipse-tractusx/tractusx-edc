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
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.tractusx.edc.tests.TractusxParticipantBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.tests.TxParticipant.ASYNC_TIMEOUT;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.bnpPolicy;

/**
 * Base tests for Http PULL scenario
 */
public abstract class HttpConsumerPullBaseTest {

    protected MockWebServer server;

    public static JsonObject createProxyRequest() {
        return Json.createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "HttpProxy-PULL")
                .build();

    }

    @BeforeEach
    void setup() {
        server = new MockWebServer();
    }

    @Test
    void transferData_privateBackend() throws IOException, InterruptedException {
        var assetId = "api-asset-1";
        var url = server.url("/mock/api");
        server.start();


        Map<String, Object> dataAddress = Map.of(
                "baseUrl", url.toString(),
                "type", "HttpData",
                "contentType", "application/json"
        );

        plato().createAsset(assetId, Map.of(), dataAddress);

        var accessPolicyId = plato().createPolicyDefinition(createAccessPolicy(sokrates().getBpn()));
        var contractPolicyId = plato().createPolicyDefinition(createContractPolicy(sokrates().getBpn()));
        plato().createContractDefinition(assetId, "def-1", accessPolicyId, contractPolicyId);
        var transferProcessId = sokrates().requestAsset(plato(), assetId, Json.createObjectBuilder().build(), createProxyRequest());

        var contractAgreementId = new AtomicReference<String>();
        var edr = new AtomicReference<JsonObject>();

        // wait until transfer process completes
        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var tpState = sokrates().getTransferProcessState(transferProcessId);
                    assertThat(tpState).isNotNull().isEqualTo(TransferProcessStates.STARTED.toString());
                });

        // wait until EDC is available on the consumer side
        server.enqueue(new MockResponse().setBody("test response").setResponseCode(200));
        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    edr.set(sokrates().edrs().getEdr(transferProcessId));
                    assertThat(edr).isNotNull();
                });

        // pull data out of provider's backend service:
        // Prov-DP -> Prov-backend
        assertThat(sokrates().data().pullData(edr.get(), Map.of())).isEqualTo("test response");
        var rq = server.takeRequest();
        assertThat(rq.getHeader("Edc-Contract-Agreement-Id")).isNotNull();
        assertThat(rq.getHeader("Edc-Bpn")).isEqualTo(sokrates().getBpn());
        assertThat(rq.getMethod()).isEqualToIgnoringCase("GET");
    }

    @AfterEach
    void teardown() throws IOException {
        server.shutdown();
    }

    protected abstract TractusxParticipantBase plato();

    protected abstract TractusxParticipantBase sokrates();

    protected JsonObject createAccessPolicy(String bpn) {
        return bnpPolicy(bpn);
    }

    protected JsonObject createContractPolicy(String bpn) {
        return bnpPolicy(bpn);
    }
}
