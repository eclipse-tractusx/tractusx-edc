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
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.lifecycle.Participant;
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
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.businessPartnerNumberPolicy;
import static org.eclipse.tractusx.edc.helpers.TransferProcessHelperFunctions.createProxyRequest;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.platoConfiguration;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.sokratesConfiguration;

public abstract class AbstractHttpConsumerPullWithProxyTest {

    protected static final Participant SOKRATES = new Participant(SOKRATES_NAME, SOKRATES_BPN, sokratesConfiguration());
    protected static final Participant PLATO = new Participant(PLATO_NAME, PLATO_BPN, platoConfiguration());

    private static final Duration ASYNC_TIMEOUT = ofSeconds(45);
    private static final Duration ASYNC_POLL_INTERVAL = ofSeconds(1);
    MockWebServer server = new MockWebServer();

    @Test
    void transferData_privateBackend() throws IOException, InterruptedException {
        var assetId = "api-asset-1";
        var url = server.url("/mock/api");
        server.start();

        var authCodeHeaderName = "test-authkey";
        var authCode = "test-authcode";
        var dataAddress = Json.createObjectBuilder()
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "contentType", "application/json")
                .add(EDC_NAMESPACE + "baseUrl", url.toString())
                .add(EDC_NAMESPACE + "authKey", authCodeHeaderName)
                .add(EDC_NAMESPACE + "authCode", authCode)
                .build();

        PLATO.createAsset(assetId, Json.createObjectBuilder().build(), dataAddress);

        PLATO.createPolicy(businessPartnerNumberPolicy("policy-1", SOKRATES.getBpn()));
        PLATO.createPolicy(businessPartnerNumberPolicy("policy-2", SOKRATES.getBpn()));
        PLATO.createContractDefinition(assetId, "def-1", "policy-1", "policy-2");
        var negotiationId = SOKRATES.negotiateContract(PLATO, assetId);

        // forward declarations of our actual values
        var transferProcessId = new AtomicReference<String>();
        var dataRequestId = UUID.randomUUID().toString();
        var contractAgreementId = new AtomicReference<String>();
        var edr = new AtomicReference<EndpointDataReference>();

        // wait for the successful contract negotiation
        await().pollInterval(ASYNC_POLL_INTERVAL)
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var negotiationState = SOKRATES.getNegotiationState(negotiationId);
                    assertThat(negotiationState).isEqualTo(ContractNegotiationStates.FINALIZED.toString());

                    var agreementId = SOKRATES.getContractAgreementId(negotiationId);
                    assertThat(agreementId).isNotNull();
                    contractAgreementId.set(agreementId);

                    var tpId = SOKRATES.requestTransfer(dataRequestId, contractAgreementId.get(), assetId, PLATO, createProxyRequest());
                    transferProcessId.set(tpId);
                    assertThat(transferProcessId).isNotNull();
                });

        // wait until transfer process completes
        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var tpState = SOKRATES.getTransferProcessState(transferProcessId.get());
                    assertThat(tpState).isNotNull().isEqualTo(TransferProcessStates.COMPLETED.toString());
                });

        // wait until EDC is available on the consumer side
        server.enqueue(new MockResponse().setBody("test response").setResponseCode(200));
        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    edr.set(SOKRATES.getDataReference(dataRequestId));
                    assertThat(edr).isNotNull();
                });

        // pull data out of provider's backend service:
        // Cons-DP -> Prov-DP -> Prov-backend
        assertThat(SOKRATES.pullData(edr.get(), Map.of())).isEqualTo("test response");
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
