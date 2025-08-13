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

package org.eclipse.tractusx.edc.tests.edrv2;

import jakarta.json.Json;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationAgreed;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationInitiated;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationRequested;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationVerified;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessInitiated;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessProvisioned;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessRequested;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.tractusx.edc.tests.helpers.EdrNegotiationHelperFunctions;
import org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions;
import org.eclipse.tractusx.edc.tests.helpers.ReceivedEvent;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.PostgresExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_DID;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_DID;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.helpers.EdrNegotiationHelperFunctions.createEvent;
import static org.eclipse.tractusx.edc.tests.helpers.Functions.readEvent;
import static org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase.ASYNC_POLL_INTERVAL;
import static org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase.ASYNC_TIMEOUT;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;
import static org.mockserver.model.HttpRequest.request;

@EndToEndTest
public class NegotiateEdrTest {

    private static final TransferParticipant CONSUMER = TransferParticipant.Builder.newInstance()
            .name(CONSUMER_NAME)
            .id(CONSUMER_DID)
            .bpn(CONSUMER_BPN)
            .build();

    private static final TransferParticipant PROVIDER = TransferParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_DID)
            .bpn(PROVIDER_BPN)
            .build();

    @RegisterExtension
    @Order(0)
    private static final PostgresExtension POSTGRES = new PostgresExtension(CONSUMER.getName(), PROVIDER.getName());

    @RegisterExtension
    private static final RuntimeExtension CONSUMER_RUNTIME = pgRuntime(CONSUMER, POSTGRES);

    @RegisterExtension
    private static final RuntimeExtension PROVIDER_RUNTIME = pgRuntime(PROVIDER, POSTGRES);

    private ClientAndServer server;

    @BeforeEach
    void setup() {
        server = ClientAndServer.startClientAndServer("localhost", getFreePort());
    }

    @Test
    @DisplayName("Verify that the callbacks are invoked when negotiating an EDR")
    void negotiateEdr_shouldInvokeCallbacks() {

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

        var url = "http://%s:%d%s".formatted("localhost", server.getPort(), "/mock/api");

        var assetId = "api-asset-1";

        var authCodeHeaderName = "test-authkey";
        var authCode = "test-authcode";
        Map<String, Object> dataAddress = Map.of(
                "name", "transfer-test",
                "baseUrl", url,
                "type", "HttpData",
                "contentType", "application/json",
                "authKey", authCodeHeaderName,
                "authCode", authCode
        );

        PROVIDER.createAsset(assetId, Map.of(), dataAddress);

        PROVIDER.storeBusinessPartner(CONSUMER.getBpn(), "test-group1", "test-group2");
        var accessPolicy = PROVIDER.createPolicyDefinition(PolicyHelperFunctions.bpnGroupPolicy(Operator.IS_NONE_OF, "forbidden-policy"));
        var contractPolicy = PROVIDER.createPolicyDefinition(PolicyHelperFunctions.bpnGroupPolicy(Operator.IS_ALL_OF, "test-group1", "test-group2"));
        PROVIDER.createContractDefinition(assetId, "def-1", accessPolicy, contractPolicy);


        var events = new ArrayList<ReceivedEvent>();
        server.when(request().withPath("/mock/api"))
                .respond(request -> {
                    var event = readEvent(request);
                    events.add(event);
                    return HttpResponse.response().withStatusCode(200);
                });


        var callbacks = Json.createArrayBuilder()
                .add(EdrNegotiationHelperFunctions.createCallback(url, true, Set.of("contract.negotiation", "transfer.process")))
                .build();

        var contractNegotiationId = CONSUMER.edrs().negotiateEdr(PROVIDER, assetId, callbacks);

        await().pollInterval(ASYNC_POLL_INTERVAL)
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    assertThat(expectedEvents).usingRecursiveFieldByFieldElementComparator().containsAll(events);
                });
        await().pollInterval(ASYNC_POLL_INTERVAL)
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var edrCaches = CONSUMER.edrs().getEdrEntriesByAssetId(assetId);
                    assertThat(edrCaches).hasSize(1);
                });

        var edrCaches = CONSUMER.edrs().getEdrEntriesByAssetId(assetId);

        assertThat(edrCaches).hasSize(1);

        assertThat(CONSUMER.edrs().getEdrEntriesByContractNegotiationId(contractNegotiationId)).hasSize(1);

        assertThat(edrCaches).hasSize(1);

        var transferProcessId = edrCaches.get(0).asJsonObject().getString("transferProcessId");
        var cnId = edrCaches.get(0).asJsonObject().getString("contractNegotiationId");
        var agreementId = edrCaches.get(0).asJsonObject().getString("agreementId");

        assertThat(cnId).isEqualTo(contractNegotiationId);
        assertThat(CONSUMER.edrs().getEdrEntriesByAgreementId(agreementId)).hasSize(1);


        var edr = CONSUMER.edrs().getEdr(transferProcessId);

        assertThat(edr.getJsonString("type").getString()).isEqualTo("https://w3id.org/idsa/v4.1/HTTP");
        assertThat(edr.getJsonString("endpoint").getString()).isNotNull();
        assertThat(edr.getJsonString("endpointType").getString()).isEqualTo(edr.getJsonString("type").getString());
        assertThat(edr.getJsonString("authorization").getString()).isNotNull();

    }

    @AfterEach
    void teardown() {
        server.stop();
    }

}
