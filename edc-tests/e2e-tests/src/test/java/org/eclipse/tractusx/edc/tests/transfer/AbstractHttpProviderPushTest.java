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
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.eclipse.tractusx.edc.lifecycle.Participant;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.businessPartnerNumberPolicy;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.platoConfiguration;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.sokratesConfiguration;
import static org.eclipse.tractusx.edc.tests.TestCommon.ASYNC_TIMEOUT;

public abstract class AbstractHttpProviderPushTest {

    protected static final Participant SOKRATES = new Participant(SOKRATES_NAME, SOKRATES_BPN, sokratesConfiguration());
    protected static final Participant PLATO = new Participant(PLATO_NAME, PLATO_BPN, platoConfiguration());

    private MockWebServer server;

    @BeforeEach
    void setup() {
        server = new MockWebServer();
    }

    @Test
    void httpPushDataTransfer() throws IOException {
        var assetId = UUID.randomUUID().toString();

        var providerUrl = server.url("/mock/api/provider");
        var consumerUrl = server.url("/mock/api/consumer");

        server.setDispatcher(new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
                return switch (recordedRequest.getPath().split("\\?")[0]) {
                    case "/mock/api/provider" -> new MockResponse().setResponseCode(200);
                    case "/mock/api/consumer" -> new MockResponse().setResponseCode(200);
                    default -> new MockResponse().setResponseCode(404);
                };
            }
        });

        server.start();

        PLATO.createAsset(assetId, Json.createObjectBuilder().build(), httpDataAddress(providerUrl.toString()));
        PLATO.createPolicy(createTestPolicy("policy-1", SOKRATES.getBpn()));
        PLATO.createContractDefinition(assetId, "def-1", "policy-1", "policy-1");

        var destination = httpDataAddress(consumerUrl.toString());

        var transferProcessId = SOKRATES.requestAsset(PLATO, assetId, destination);
        await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> {
            var state = SOKRATES.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(COMPLETED.name());
        });
    }

    @AfterEach
    void teardown() throws IOException {
        server.shutdown();
    }

    protected JsonObject createTestPolicy(String policyId, String bpn) {
        return businessPartnerNumberPolicy(policyId, bpn);
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
