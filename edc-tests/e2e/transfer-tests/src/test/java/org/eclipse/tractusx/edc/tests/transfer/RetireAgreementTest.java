/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2025 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
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
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.PostgresExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_2025_09_NS;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_DID;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.DSP_2025;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.DSP_2025_PATH;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_DID;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase.ASYNC_POLL_INTERVAL;
import static org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase.ASYNC_TIMEOUT;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;

@EndToEndTest
public class RetireAgreementTest {

    private static final TransferParticipant CONSUMER = TransferParticipant.Builder.newInstance()
            .name(CONSUMER_NAME)
            .id(CONSUMER_DID)
            .bpn(CONSUMER_BPN)
            .protocol(DSP_2025)
            .protocolVersionPath(DSP_2025_PATH)
            .build();

    private static final TransferParticipant PROVIDER = TransferParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_DID)
            .bpn(PROVIDER_BPN)
            .protocol(DSP_2025)
            .protocolVersionPath(DSP_2025_PATH)
            .enableEventSubscription()
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
        CONSUMER.setJsonLd(CONSUMER_RUNTIME.getService(JsonLd.class));
    }

    @Test
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
        var policy = PolicyHelperFunctions.frameworkPolicy(Map.of(), CX_POLICY_2025_09_NS + "access");
        var contractPolicy = PROVIDER.createPolicyDefinition(policy);
        PROVIDER.createContractDefinition(assetId, "def-1", accessPolicy, contractPolicy);

        var edrsApi = CONSUMER.edrs();

        edrsApi.negotiateEdr(PROVIDER, assetId, Json.createArrayBuilder().build());

        var edrCache = await().pollInterval(ASYNC_POLL_INTERVAL)
                .atMost(ASYNC_TIMEOUT)
                .until(() -> CONSUMER.edrs().getEdrEntriesByAssetId(assetId), it -> it.size() == 1)
                .get(0).asJsonObject();

        var agreementId = edrCache.getString("agreementId");
        var transferProcessId = edrCache.getString("transferProcessId");

        var response = PROVIDER.retireProviderAgreement(agreementId);
        response.statusCode(204);

        var event = PROVIDER.waitForEvent("ContractAgreementRetired");
        assertThat(event).isNotNull();

        // verify existing TP on consumer retires

        CONSUMER.waitForTransferProcess(transferProcessId, TransferProcessStates.TERMINATED);

        // verify no new TP can start for same contract agreement

        var privateProperties = Json.createObjectBuilder().build();
        var dataDestination = Json.createObjectBuilder().add("type", "HttpData").build();

        var failedTransferId = CONSUMER.initiateTransfer(PROVIDER, agreementId, privateProperties, dataDestination, "HttpData-PULL");

        CONSUMER.waitForTransferProcess(failedTransferId, TransferProcessStates.TERMINATED);
    }

    @Test
    void retireAgreement_shouldFail_whenAgreementDoesNotExist() {
        PROVIDER.retireProviderAgreement(UUID.randomUUID().toString()).statusCode(404);
    }

    @AfterEach
    void teardown() {
        server.stop();
    }

}
