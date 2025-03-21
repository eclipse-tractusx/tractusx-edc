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

import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.PostgresExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATED;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.inForceDatePolicy;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase.ASYNC_TIMEOUT;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.memoryRuntime;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;

public class PolicyMonitorEndToEndTest {

    protected static final TransferParticipant CONSUMER = TransferParticipant.Builder.newInstance()
            .name(CONSUMER_NAME)
            .id(CONSUMER_BPN)
            .build();


    protected static final TransferParticipant PROVIDER = TransferParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_BPN)
            .build();

    abstract static class Tests {

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

            var policy = inForceDatePolicy("gteq", "contractAgreement+0s", "lteq", "contractAgreement+10s");
            var policyId = PROVIDER.createPolicyDefinition(policy);
            PROVIDER.createContractDefinition(assetId, UUID.randomUUID().toString(), policyId, policyId);


            var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER).withTransferType("HttpData-PULL").execute();
            await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> {
                var state = CONSUMER.getTransferProcessState(transferProcessId);
                assertThat(state).isEqualTo(STARTED.name());
            });

            await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> {
                var state = CONSUMER.getTransferProcessState(transferProcessId);
                assertThat(state).isEqualTo(TERMINATED.name());
            });
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
