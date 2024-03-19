/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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
 */

package org.eclipse.tractusx.edc.tests.edrv2;

import jakarta.json.Json;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationAgreed;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationInitiated;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationRequested;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationVerified;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessInitiated;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessProvisioned;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessRequested;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration;
import org.eclipse.tractusx.edc.tests.TxParticipant;
import org.eclipse.tractusx.edc.tests.helpers.EdrNegotiationHelperFunctions;
import org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.tractusx.edc.tests.TxParticipant.ASYNC_POLL_INTERVAL;
import static org.eclipse.tractusx.edc.tests.TxParticipant.ASYNC_TIMEOUT;
import static org.eclipse.tractusx.edc.tests.helpers.EdrNegotiationHelperFunctions.createEvent;
import static org.eclipse.tractusx.edc.tests.helpers.Functions.waitForEvent;

public abstract class AbstractNegotiateEdrTest {

    protected static final TxParticipant SOKRATES = TxParticipant.Builder.newInstance()
            .name(TestRuntimeConfiguration.SOKRATES_NAME)
            .id(TestRuntimeConfiguration.SOKRATES_BPN)
            .build();

    protected static final TxParticipant PLATO = TxParticipant.Builder.newInstance()
            .name(TestRuntimeConfiguration.PLATO_NAME)
            .id(TestRuntimeConfiguration.PLATO_BPN)
            .build();

    MockWebServer server;

    @BeforeEach
    void setup() {
        server = new MockWebServer();
    }

    @Test
    @DisplayName("Verify that the callbacks are invoked when negotiating an EDR")
    void negotiateEdr_shouldInvokeCallbacks() throws IOException {

        var expectedEvents = List.of(
                createEvent(ContractNegotiationInitiated.class),
                createEvent(ContractNegotiationRequested.class),
                createEvent(ContractNegotiationAgreed.class),
                createEvent(ContractNegotiationFinalized.class),
                createEvent(ContractNegotiationVerified.class),
                createEvent(TransferProcessInitiated.class),
                createEvent(TransferProcessProvisioned.class),
                createEvent(TransferProcessRequested.class),
                createEvent(TransferProcessStarted.class));

        var assetId = "api-asset-1";
        var url = server.url("/mock/api");
        server.start();

        var authCodeHeaderName = "test-authkey";
        var authCode = "test-authcode";
        Map<String, Object> dataAddress = Map.of(
                "name", "transfer-test",
                "baseUrl", url.toString(),
                "type", "HttpData",
                "contentType", "application/json",
                "authKey", authCodeHeaderName,
                "authCode", authCode
        );

        PLATO.createAsset(assetId, Map.of(), dataAddress);

        PLATO.storeBusinessPartner(SOKRATES.getBpn(), "test-group1", "test-group2");
        var accessPolicy = PLATO.createPolicyDefinition(PolicyHelperFunctions.bpnGroupPolicy(Operator.IS_NONE_OF, "forbidden-policy"));
        var contractPolicy = PLATO.createPolicyDefinition(PolicyHelperFunctions.bpnGroupPolicy(Operator.IS_ALL_OF, "test-group1", "test-group2"));
        PLATO.createContractDefinition(assetId, "def-1", accessPolicy, contractPolicy);


        expectedEvents.forEach(event -> server.enqueue(new MockResponse()));

        var callbacks = Json.createArrayBuilder()
                .add(EdrNegotiationHelperFunctions.createCallback(url.toString(), true, Set.of("contract.negotiation", "transfer.process")))
                .build();

        var contractNegotiationId = SOKRATES.edrs().negotiateEdr(PLATO, assetId, callbacks);

        var events = expectedEvents.stream()
                .map(receivedEvent -> waitForEvent(server))
                .collect(Collectors.toList());


        await().pollInterval(ASYNC_POLL_INTERVAL)
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var edrCaches = SOKRATES.edrs().getEdrEntriesByAssetId(assetId);
                    assertThat(edrCaches).hasSize(1);
                });

        assertThat(expectedEvents).usingRecursiveFieldByFieldElementComparator().containsAll(events);

        var edrCaches = SOKRATES.edrs().getEdrEntriesByAssetId(assetId);

        assertThat(edrCaches).hasSize(1);

        assertThat(SOKRATES.edrs().getEdrEntriesByContractNegotiationId(contractNegotiationId)).hasSize(1);

        assertThat(edrCaches).hasSize(1);

        var transferProcessId = edrCaches.get(0).asJsonObject().getString("transferProcessId");
        var cnId = edrCaches.get(0).asJsonObject().getString("contractNegotiationId");
        var agreementId = edrCaches.get(0).asJsonObject().getString("agreementId");

        assertThat(cnId).isEqualTo(contractNegotiationId);
        assertThat(SOKRATES.edrs().getEdrEntriesByAgreementId(agreementId)).hasSize(1);


        var edr = SOKRATES.edrs().getEdr(transferProcessId);

        assertThat(edr.getJsonString("type").getString()).isEqualTo("https://w3id.org/idsa/v4.1/HTTP");
        assertThat(edr.getJsonString("endpoint").getString()).isNotNull();
        assertThat(edr.getJsonString("endpointType").getString()).isEqualTo(edr.getJsonString("type").getString());
        assertThat(edr.getJsonString("authorization").getString()).isNotNull();

    }

    @AfterEach
    void teardown() throws IOException {
        server.shutdown();
    }

}
