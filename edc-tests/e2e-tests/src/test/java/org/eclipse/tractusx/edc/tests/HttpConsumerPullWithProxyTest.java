/*
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.tests;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.lifecycle.MultiRuntimeTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.HTTP_PROXY;
import static org.eclipse.tractusx.edc.policy.PolicyHelperFunctions.businessPartnerNumberPolicy;

@EndToEndTest
public class HttpConsumerPullWithProxyTest extends MultiRuntimeTest {
    private static final Duration ASYNC_TIMEOUT = ofSeconds(45);
    private static final Duration ASYNC_POLL_INTERVAL = ofSeconds(1);
    private final long ONE_WEEK = 60 * 60 * 24 * 7;
    MockWebServer server = new MockWebServer();

    @Test
    void transferData_privateBackend() throws IOException, InterruptedException {
        var assetId = "api-asset-1";
        var url = server.url("/mock/api");
        server.start();

        var authCodeHeaderName = "test-authkey";
        var authCode = "test-authcode";
        plato.createAsset(assetId, Map.of(), HttpDataAddress.Builder.newInstance()
                .contentType("application/json")
                .baseUrl(url.toString())
                .authKey(authCodeHeaderName)
                .authCode(authCode)
                .build());
        plato.createPolicy(businessPartnerNumberPolicy("policy-1", sokrates.getBpn()));
        plato.createPolicy(businessPartnerNumberPolicy("policy-2", sokrates.getBpn()));
        plato.createContractDefinition(assetId, "def-1", "policy-1", "policy-2", ONE_WEEK);
        var negotiationId = sokrates.negotiateContract(plato, assetId);

        // forward declarations of our actual values
        var transferProcessId = new AtomicReference<String>();
        var dataRequestId = UUID.randomUUID().toString();
        var contractAgreementId = new AtomicReference<String>();
        var edr = new AtomicReference<EndpointDataReference>();


        // wait for the successful contract negotiation
        await().pollInterval(ASYNC_POLL_INTERVAL)
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var negotiation = sokrates.getNegotiation(negotiationId);
                    assertThat(negotiation.getState()).isEqualTo(ContractNegotiationStates.CONFIRMED.toString());
                    contractAgreementId.set(negotiation.getContractAgreementId());
                    assertThat(contractAgreementId).isNotNull();
                    transferProcessId.set(sokrates.requestTransfer(contractAgreementId.get(), assetId, plato, DataAddress.Builder.newInstance()
                            .type(HTTP_PROXY)
                            .build(), dataRequestId));
                    assertThat(transferProcessId).isNotNull();
                });

        // wait until transfer process completes
        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var tp = sokrates.getTransferProcess(transferProcessId.get());
                    assertThat(tp).isNotNull()
                            .extracting(TransferProcessDto::getState).isEqualTo(TransferProcessStates.COMPLETED.toString());
                });

        // wait until EDC is available on the consumer side
        server.enqueue(new MockResponse().setBody("test response").setResponseCode(200));
        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    edr.set(sokrates.getDataReference(dataRequestId));
                    assertThat(edr).isNotNull();
                });

        // pull data out of provider's backend service:
        // Cons-DP -> Prov-DP -> Prov-backend
        assertThat(sokrates.pullData(edr.get(), Map.of())).isEqualTo("test response");
        var rq = server.takeRequest();
        assertThat(rq.getHeader(authCodeHeaderName)).isEqualTo(authCode);
        assertThat(rq.getHeader("Edc-Contract-Agreement-Id")).isEqualTo(contractAgreementId.get());
        assertThat(rq.getMethod()).isEqualToIgnoringCase("GET");
    }

    @AfterEach
    void teardown() throws IOException {
        server.shutdown();
    }
}
