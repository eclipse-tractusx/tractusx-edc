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

package org.eclipse.tractusx.edc.tests.transfer;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.PostgresExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.verify.VerificationTimes;

import java.util.Map;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.inForceDatePolicy;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.memoryRuntime;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class TransferPullEndToEndTest {

    abstract static class Tests extends HttpConsumerPullBaseTest {

        protected static final TransferParticipant CONSUMER = TransferParticipant.Builder.newInstance()
                .name(CONSUMER_NAME)
                .id(CONSUMER_BPN)
                .build();
        protected static final TransferParticipant PROVIDER = TransferParticipant.Builder.newInstance()
                .name(PROVIDER_NAME)
                .id(PROVIDER_BPN)
                .build();

        @Override
        public TractusxParticipantBase provider() {
            return PROVIDER;
        }

        @Override
        public TractusxParticipantBase consumer() {
            return CONSUMER;
        }


        @Test
        void transferData_withSuspendResume() {
            var assetId = "api-asset-1";

            var requestDefinition = request().withMethod("GET").withPath(MOCK_BACKEND_PATH);

            Map<String, Object> dataAddress = Map.of(
                    "baseUrl", privateBackendUrl,
                    "type", "HttpData",
                    "contentType", "application/json"
            );

            PROVIDER.createAsset(assetId, Map.of(), dataAddress);

            var accessPolicyId = PROVIDER.createPolicyDefinition(createAccessPolicy(CONSUMER.getBpn()));
            var contractPolicyId = PROVIDER.createPolicyDefinition(createContractPolicy(CONSUMER.getBpn()));
            PROVIDER.createContractDefinition(assetId, "def-1", accessPolicyId, contractPolicyId);
            var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER).withTransferType("HttpData-PULL")
                    .withDestination(httpDataDestination()).execute();

            CONSUMER.waitForTransferProcess(transferProcessId, TransferProcessStates.STARTED);

            // wait until EDC is available on the consumer side
            server.when(requestDefinition).respond(response().withStatusCode(200).withBody("test response"));

            var edr = CONSUMER.edrs().waitForEdr(transferProcessId);

            // consumer can fetch data with a valid token
            var data = CONSUMER.data().pullData(edr, Map.of());
            assertThat(data).isNotNull().isEqualTo("test response");

            server.verify(requestDefinition, VerificationTimes.exactly(1));

            CONSUMER.suspendTransfer(transferProcessId, "reason");
            CONSUMER.waitForTransferProcess(transferProcessId, TransferProcessStates.SUSPENDED);

            // consumer cannot fetch data with the prev token (suspended)
            CONSUMER.data().pullDataRequest(edr, Map.of()).statusCode(403);
            server.verify(requestDefinition, VerificationTimes.exactly(1));

            CONSUMER.resumeTransfer(transferProcessId);
            CONSUMER.waitForTransferProcess(transferProcessId, TransferProcessStates.STARTED);

            var newEdr = CONSUMER.edrs().waitForEdr(transferProcessId);

            // consumer can now re-fetch data with a new EDR token
            data = CONSUMER.data().pullData(newEdr, Map.of());
            assertThat(data).isNotNull().isEqualTo("test response");

            server.verify(requestDefinition, VerificationTimes.exactly(2));

            // consumer cannot fetch data with the prev token (suspended) after the transfer process has been resumed
            CONSUMER.data().pullDataRequest(edr, Map.of()).statusCode(403);
            server.verify(requestDefinition, VerificationTimes.exactly(2));

        }

        @Test
        void transferData_withTerminate() {
            var assetId = "api-asset-1";

            var requestDefinition = request().withMethod("GET").withPath(MOCK_BACKEND_PATH);

            Map<String, Object> dataAddress = Map.of(
                    "baseUrl", privateBackendUrl,
                    "type", "HttpData",
                    "contentType", "application/json"
            );

            PROVIDER.createAsset(assetId, Map.of(), dataAddress);

            var accessPolicyId = PROVIDER.createPolicyDefinition(createAccessPolicy(CONSUMER.getBpn()));
            var contractPolicyId = PROVIDER.createPolicyDefinition(inForcePolicy());
            PROVIDER.createContractDefinition(assetId, "def-1", accessPolicyId, contractPolicyId);
            var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER).withTransferType("HttpData-PULL")
                    .withDestination(httpDataDestination()).execute();

            CONSUMER.waitForTransferProcess(transferProcessId, TransferProcessStates.STARTED);

            // wait until EDC is available on the consumer side
            server.when(requestDefinition).respond(response().withStatusCode(200).withBody("test response"));

            var edr = CONSUMER.edrs().waitForEdr(transferProcessId);

            // consumer can fetch data with a valid token
            var data = CONSUMER.data().pullData(edr, Map.of());
            assertThat(data).isNotNull().isEqualTo("test response");

            server.verify(requestDefinition, VerificationTimes.exactly(1));

            CONSUMER.waitForTransferProcess(transferProcessId, TransferProcessStates.TERMINATED);

            // consumer cannot fetch data with the prev token (suspended)
            var body = CONSUMER.data().pullDataRequest(edr, Map.of()).statusCode(403).extract().body().asString();
            server.verify(requestDefinition, VerificationTimes.exactly(1));

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

        protected JsonObject inForcePolicy() {
            return inForceDatePolicy("gteq", "contractAgreement+0s", "lteq", "contractAgreement+10s");
        }

    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        protected static final RuntimeExtension CONSUMER_RUNTIME = memoryRuntime(CONSUMER.getName(), CONSUMER.getBpn(), CONSUMER.getConfiguration());

        @RegisterExtension
        protected static final RuntimeExtension PROVIDER_RUNTIME = memoryRuntime(PROVIDER.getName(), PROVIDER.getBpn(), PROVIDER.getConfiguration());

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
