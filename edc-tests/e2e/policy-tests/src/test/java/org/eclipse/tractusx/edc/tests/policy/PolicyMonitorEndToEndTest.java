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

package org.eclipse.tractusx.edc.tests.policy;

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.PostgresExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_DID;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.DSP_2025;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.DSP_2025_PATH;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_DID;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.emptyPolicy;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.inForceDateUsagePolicy;
import static org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase.ASYNC_TIMEOUT;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;

@EndToEndTest
public class PolicyMonitorEndToEndTest {

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
            .build();

    @RegisterExtension
    @Order(0)
    private static final PostgresExtension POSTGRES = new PostgresExtension(CONSUMER.getName(), PROVIDER.getName());

    @RegisterExtension
    private static final RuntimeExtension CONSUMER_RUNTIME = pgRuntime(CONSUMER, POSTGRES);

    @RegisterExtension
    private static final RuntimeExtension PROVIDER_RUNTIME = pgRuntime(PROVIDER, POSTGRES);
    
    @BeforeEach
    void setup() {
        CONSUMER.setJsonLd(CONSUMER_RUNTIME.getService(JsonLd.class));
    }

    @Test
    void shouldTerminateTransfer_whenPolicyExpires() {
        var assetId = UUID.randomUUID().toString();

        Map<String, Object> dataAddress = Map.of(
                "name", "transfer-test",
                "baseUrl", "http://localhost:8080",
                "type", "HttpData",
                "contentType", "application/json",
                "proxyQueryParams", "true"
        );
        PROVIDER.createAsset(assetId, Map.of(), dataAddress);

        var accessPolicy = emptyPolicy();
        var accessPolicyId = PROVIDER.createPolicyDefinition(accessPolicy);
        var usagePolicy = inForceDateUsagePolicy("gteq", "contractAgreement+0s", "lteq", "contractAgreement+10s");
        var usagePolicyId = PROVIDER.createPolicyDefinition(usagePolicy);
        PROVIDER.createContractDefinition(assetId, UUID.randomUUID().toString(), accessPolicyId, usagePolicyId);


        var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                .withTransferType("HttpData-PULL")
                .withDestination(httpDataDestination())
                .execute();
        await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(STARTED.name());
        });

        await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(TERMINATED.name());
        });
    }

    private JsonObject httpDataDestination() {
        return createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "properties", createObjectBuilder()
                        .add(EDC_NAMESPACE + "baseUrl", "http://localhost:8080")
                        .build())
                .build();
    }

}
