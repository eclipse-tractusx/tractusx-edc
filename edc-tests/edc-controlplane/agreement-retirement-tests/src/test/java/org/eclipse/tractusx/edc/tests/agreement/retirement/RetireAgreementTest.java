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

package org.eclipse.tractusx.edc.tests.agreement.retirement;

//import jakarta.json.Json;
//import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
//import org.eclipse.edc.policy.model.Operator;
//import org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.PostgresExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
//import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;

import java.io.IOException;
//import java.util.Map;
//import java.util.UUID;

//import static org.assertj.core.api.Assertions.assertThat;
//import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
//import static org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase.ASYNC_POLL_INTERVAL;
//import static org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase.ASYNC_TIMEOUT;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.memoryRuntime;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;

public class RetireAgreementTest {

    protected static final TransferParticipant CONSUMER = TransferParticipant.Builder.newInstance()
            .name(CONSUMER_NAME)
            .id(CONSUMER_BPN)
            .build();

    protected static final TransferParticipant PROVIDER = TransferParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_BPN)
            .build();


    abstract static class Tests {

        ClientAndServer server;

        @BeforeEach
        void setup() {
            server = ClientAndServer.startClientAndServer("localhost", getFreePort());
        }

        /*@Test
        @DisplayName("Verify all existing TPs related to an agreement are terminated upon its retirement")
        void retireAgreement_shouldCloseTransferProcesses() {

            var assetId = "api-asset-1";

            Map<String, Object> dataAddress = Map.of(
                    "name", "transfer-test",
                    "baseUrl", "https://mock-url.com",
                    "type", "HttpData",
                    "contentType", "application/json"
            );

            PROVIDER.createAsset(assetId, Map.of(), dataAddress);

            PROVIDER.storeBusinessPartner(CONSUMER.getBpn(), "test-group1");
            var accessPolicy = PROVIDER.createPolicyDefinition(PolicyHelperFunctions.bpnGroupPolicy(Operator.IS_ALL_OF, "test-group1"));
            var policy = PolicyHelperFunctions.frameworkPolicy(Map.of());
            var contractPolicy = PROVIDER.createPolicyDefinition(policy);
            PROVIDER.createContractDefinition(assetId, "def-1", accessPolicy, contractPolicy);

            var edrsApi = CONSUMER.edrs();

            edrsApi.negotiateEdr(PROVIDER, assetId, Json.createArrayBuilder().build());

            await().pollInterval(ASYNC_POLL_INTERVAL)
                    .atMost(ASYNC_TIMEOUT)
                    .untilAsserted(() -> {
                        var edrCaches = CONSUMER.edrs().getEdrEntriesByAssetId(assetId);
                        assertThat(edrCaches).hasSize(1);
                    });

            var edrCaches = CONSUMER.edrs().getEdrEntriesByAssetId(assetId);

            var agreementId = edrCaches.get(0).asJsonObject().getString("agreementId");

            var transferProcessId = edrCaches.get(0).asJsonObject().getString("transferProcessId");

            var response = PROVIDER.retireProviderAgreement(agreementId);
            response.statusCode(204);

            // verify existing TP on consumer retires

            CONSUMER.waitForTransferProcess(transferProcessId, TransferProcessStates.TERMINATED);

            // verify no new TP can start for same contract agreement

            var privateProperties = Json.createObjectBuilder().build();
            var dataDestination = Json.createObjectBuilder().add("type", "HttpData").build();

            var failedTransferId = CONSUMER.initiateTransfer(PROVIDER, agreementId, privateProperties, dataDestination, "HttpData-PULL");

            CONSUMER.waitForTransferProcess(failedTransferId, TransferProcessStates.TERMINATED);


        }*/

        /*@Test
        void retireAgreement_shouldFail_whenAgreementDoesNotExist() {
            PROVIDER.retireProviderAgreement(UUID.randomUUID().toString()).statusCode(404);
        }*/

        @AfterEach
        void teardown() throws IOException {
            server.stop();
        }
    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        protected static final RuntimeExtension CONSUMER_RUNTIME = memoryRuntime(CONSUMER.getName(), CONSUMER.getBpn(), CONSUMER::getConfig);

        @RegisterExtension
        protected static final RuntimeExtension PROVIDER_RUNTIME = memoryRuntime(PROVIDER.getName(), PROVIDER.getBpn(), PROVIDER::getConfig);

    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @RegisterExtension
        @Order(0)
        private static final PostgresExtension POSTGRES = new PostgresExtension(CONSUMER.getName(), PROVIDER.getName());

        @RegisterExtension
        protected static final RuntimeExtension CONSUMER_RUNTIME = pgRuntime(CONSUMER, POSTGRES);

        @RegisterExtension
        protected static final RuntimeExtension PROVIDER_RUNTIME = pgRuntime(PROVIDER, POSTGRES);

    }
}
