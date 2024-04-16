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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.ParticipantRuntime;
import org.eclipse.tractusx.edc.tests.runtimes.PgParticipantRuntime;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.verify.VerificationTimes;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.inForceDatePolicy;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PLATO_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.eclipse.tractusx.edc.tests.helpers.TransferProcessHelperFunctions.createProxyRequest;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.memoryRuntime;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class TransferPullEndToEndTest {

    abstract static class Tests extends HttpConsumerPullBaseTest {

        protected static final TransferParticipant SOKRATES = TransferParticipant.Builder.newInstance()
                .name(SOKRATES_NAME)
                .id(SOKRATES_BPN)
                .build();
        protected static final TransferParticipant PLATO = TransferParticipant.Builder.newInstance()
                .name(PLATO_NAME)
                .id(PLATO_BPN)
                .build();

        @Override
        public TractusxParticipantBase plato() {
            return PLATO;
        }

        @Override
        public TractusxParticipantBase sokrates() {
            return SOKRATES;
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

            PLATO.createAsset(assetId, Map.of(), dataAddress);

            var accessPolicyId = PLATO.createPolicyDefinition(createAccessPolicy(SOKRATES.getBpn()));
            var contractPolicyId = PLATO.createPolicyDefinition(createContractPolicy(SOKRATES.getBpn()));
            PLATO.createContractDefinition(assetId, "def-1", accessPolicyId, contractPolicyId);
            var transferProcessId = SOKRATES.requestAsset(PLATO, assetId, Json.createObjectBuilder().build(), createProxyRequest(), "HttpData-PULL");

            SOKRATES.waitForTransferProcess(transferProcessId, TransferProcessStates.STARTED);

            // wait until EDC is available on the consumer side
            server.when(requestDefinition).respond(response().withStatusCode(200).withBody("test response"));

            var edr = SOKRATES.edrs().waitForEdr(transferProcessId);

            // consumer can fetch data with a valid token
            var data = SOKRATES.data().pullData(edr, Map.of());
            assertThat(data).isNotNull().isEqualTo("test response");

            server.verify(requestDefinition, VerificationTimes.exactly(1));

            SOKRATES.suspendTransfer(transferProcessId, "reason");
            SOKRATES.waitForTransferProcess(transferProcessId, TransferProcessStates.SUSPENDED);

            // consumer cannot fetch data with the prev token (suspended)
            SOKRATES.data().pullDataRequest(edr, Map.of()).statusCode(403);
            server.verify(requestDefinition, VerificationTimes.exactly(1));

            SOKRATES.resumeTransfer(transferProcessId);
            SOKRATES.waitForTransferProcess(transferProcessId, TransferProcessStates.STARTED);

            var newEdr = SOKRATES.edrs().waitForEdr(transferProcessId);

            // consumer can now re-fetch data with a new EDR token
            data = SOKRATES.data().pullData(newEdr, Map.of());
            assertThat(data).isNotNull().isEqualTo("test response");

            server.verify(requestDefinition, VerificationTimes.exactly(2));

            // consumer cannot fetch data with the prev token (suspended) after the transfer process has been resumed
            SOKRATES.data().pullDataRequest(edr, Map.of()).statusCode(403);
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

            PLATO.createAsset(assetId, Map.of(), dataAddress);

            var accessPolicyId = PLATO.createPolicyDefinition(createAccessPolicy(SOKRATES.getBpn()));
            var contractPolicyId = PLATO.createPolicyDefinition(inForcePolicy());
            PLATO.createContractDefinition(assetId, "def-1", accessPolicyId, contractPolicyId);
            var transferProcessId = SOKRATES.requestAsset(PLATO, assetId, Json.createObjectBuilder().build(), createProxyRequest(), "HttpData-PULL");

            SOKRATES.waitForTransferProcess(transferProcessId, TransferProcessStates.STARTED);

            // wait until EDC is available on the consumer side
            server.when(requestDefinition).respond(response().withStatusCode(200).withBody("test response"));

            var edr = SOKRATES.edrs().waitForEdr(transferProcessId);

            // consumer can fetch data with a valid token
            var data = SOKRATES.data().pullData(edr, Map.of());
            assertThat(data).isNotNull().isEqualTo("test response");

            server.verify(requestDefinition, VerificationTimes.exactly(1));

            SOKRATES.waitForTransferProcess(transferProcessId, TransferProcessStates.TERMINATED);

            // consumer cannot fetch data with the prev token (suspended)
            var body = SOKRATES.data().pullDataRequest(edr, Map.of()).statusCode(403).extract().body().asString();
            server.verify(requestDefinition, VerificationTimes.exactly(1));

        }
        
        protected JsonObject inForcePolicy() {
            return inForceDatePolicy("gteq", "contractAgreement+0s", "lteq", "contractAgreement+10s");
        }

    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        protected static final ParticipantRuntime SOKRATES_RUNTIME = memoryRuntime(SOKRATES.getName(), SOKRATES.getBpn(), SOKRATES.getConfiguration());

        @RegisterExtension
        protected static final ParticipantRuntime PLATO_RUNTIME = memoryRuntime(PLATO.getName(), PLATO.getBpn(), PLATO.getConfiguration());

    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @RegisterExtension
        protected static final PgParticipantRuntime SOKRATES_RUNTIME = pgRuntime(SOKRATES.getName(), SOKRATES.getBpn(), SOKRATES.getConfiguration());

        @RegisterExtension
        protected static final PgParticipantRuntime PLATO_RUNTIME = pgRuntime(PLATO.getName(), PLATO.getBpn(), PLATO.getConfiguration());

    }
}