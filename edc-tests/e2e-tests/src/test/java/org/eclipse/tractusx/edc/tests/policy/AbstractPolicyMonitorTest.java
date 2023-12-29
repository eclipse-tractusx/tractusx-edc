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

package org.eclipse.tractusx.edc.tests.policy;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.eclipse.tractusx.edc.lifecycle.tx.TxParticipant;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.test.system.utils.PolicyFixtures.inForceDatePolicy;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.eclipse.tractusx.edc.tests.TestCommon.ASYNC_TIMEOUT;

public abstract class AbstractPolicyMonitorTest {

    protected static final TxParticipant SOKRATES = TxParticipant.Builder.newInstance()
            .name(SOKRATES_NAME)
            .id(SOKRATES_BPN)
            .build();


    protected static final TxParticipant PLATO = TxParticipant.Builder.newInstance()
            .name(PLATO_NAME)
            .id(PLATO_BPN)
            .build();
    private final MockWebServer server = new MockWebServer();

    @Test
    void shouldTerminateTransfer_whenPolicyExpires() throws IOException {
        var assetId = UUID.randomUUID().toString();

        Map<String, Object> dataAddress = Map.of(
                "name", "transfer-test",
                "baseUrl", "http://localhost:" + server.getPort(),
                "type", "HttpData",
                "contentType", "application/json",
                "proxyQueryParams", "true"
        );
        PLATO.createAsset(assetId, Map.of(), dataAddress);
        startHttpServerProvider();

        var policy = inForceDatePolicy("gteq", "contractAgreement+0s", "lteq", "contractAgreement+10s");
        var policyId = PLATO.createPolicyDefinition(policy);
        PLATO.createContractDefinition(assetId, UUID.randomUUID().toString(), policyId, policyId);

        var consumerUrl = server.url("/mock/api/consumer");
        var destination = httpDataAddress(consumerUrl.toString());

        var transferProcessId = SOKRATES.requestAsset(PLATO, assetId, Json.createObjectBuilder().build(), destination);
        await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> {
            var state = SOKRATES.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(STARTED.name());
        });

        await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> {
            var state = SOKRATES.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(TERMINATED.name());
        });
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

    private void startHttpServerProvider() throws IOException {
        server.setDispatcher(new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) {
                if ("/mock/api/provider".equals(recordedRequest.getPath().split("\\?")[0])) {
                    return new MockResponse().setResponseCode(200);
                }
                return new MockResponse().setResponseCode(404);
            }
        });
        server.start();
    }
}
