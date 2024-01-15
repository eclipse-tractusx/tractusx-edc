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

package org.eclipse.tractusx.edc.tests.transfer;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.lifecycle.tx.TxParticipant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.bnpPolicy;
import static org.eclipse.tractusx.edc.helpers.TransferProcessHelperFunctions.createProxyRequest;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.eclipse.tractusx.edc.tests.TestCommon.ASYNC_TIMEOUT;

public abstract class AbstractHttpConsumerPullWithProxyTest {

    protected static final TxParticipant SOKRATES = TxParticipant.Builder.newInstance()
            .name(SOKRATES_NAME)
            .id(SOKRATES_BPN)
            .build();

    protected static final TxParticipant PLATO = TxParticipant.Builder.newInstance()
            .name(PLATO_NAME)
            .id(PLATO_BPN)
            .build();

    private MockWebServer server;

    @BeforeEach
    void setup() throws IOException {
        server = new MockWebServer();
    }

    @Test
    void transferData_privateBackend() throws IOException, InterruptedException {
        var assetId = "api-asset-1";
        var url = server.url("/mock/api");
        server.start();

        var authCodeHeaderName = "test-authkey";
        var authCode = "test-authcode";

        Map<String, Object> dataAddress = Map.of(
                "baseUrl", url.toString(),
                "type", "HttpData",
                "contentType", "application/json",
                "authKey", authCodeHeaderName,
                "authCode", authCode
        );

        PLATO.createAsset(assetId, Map.of(), dataAddress);

        var accessPolicyId = PLATO.createPolicyDefinition(createAccessPolicy(SOKRATES.getBpn()));
        var contractPolicyId = PLATO.createPolicyDefinition(createContractPolicy(SOKRATES.getBpn()));
        PLATO.createContractDefinition(assetId, "def-1", accessPolicyId, contractPolicyId);
        var transferProcessId = SOKRATES.requestAsset(PLATO, assetId, Json.createObjectBuilder().build(), createProxyRequest());

        var contractAgreementId = new AtomicReference<String>();
        var edr = new AtomicReference<EndpointDataReference>();

        // wait until transfer process completes
        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var tpState = SOKRATES.getTransferProcessState(transferProcessId);
                    assertThat(tpState).isNotNull().isEqualTo(TransferProcessStates.STARTED.toString());
                });

        // wait until EDC is available on the consumer side
        server.enqueue(new MockResponse().setBody("test response").setResponseCode(200));
        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    edr.set(SOKRATES.edrs().getDataReferenceFromBackend(transferProcessId));
                    assertThat(edr).isNotNull();
                });

        // pull data out of provider's backend service:
        // Cons-DP -> Prov-DP -> Prov-backend
        assertThat(SOKRATES.data().pullData(edr.get(), Map.of())).isEqualTo("test response");
        var rq = server.takeRequest();
        assertThat(rq.getHeader(authCodeHeaderName)).isEqualTo(authCode);
        assertThat(rq.getHeader("Edc-Contract-Agreement-Id")).isEqualTo(edr.get().getContractId());
        assertThat(rq.getHeader("Edc-Bpn")).isEqualTo(SOKRATES.getBpn());
        assertThat(rq.getMethod()).isEqualToIgnoringCase("GET");
    }

    @AfterEach
    void teardown() throws IOException {
        server.shutdown();
    }

    protected JsonObject createAccessPolicy(String bpn) {
        return bnpPolicy(bpn);
    }

    protected JsonObject createContractPolicy(String bpn) {
        return bnpPolicy(bpn);
    }
}
