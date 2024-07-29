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

package org.eclipse.tractusx.edc.tests.catalog;

import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.stream.Collectors;

import static io.restassured.http.ContentType.JSON;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase.ASYNC_POLL_INTERVAL;
import static org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase.ASYNC_TIMEOUT;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.memoryRuntime;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

public class FederatedCatalogTest {

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
        @DisplayName("Consumer gets cached catalog with provider entry")
        void requestCatalog_fulfillsPolicy_shouldReturnOffer() {

            // arrange
            PROVIDER.createAsset("test-asset");
            var ap = PROVIDER.createPolicyDefinition(noConstraintPolicy());
            var cp = PROVIDER.createPolicyDefinition(noConstraintPolicy());
            PROVIDER.createContractDefinition("test-asset", "test-def", ap, cp);


            await().pollInterval(ASYNC_POLL_INTERVAL)
                    .atMost(ASYNC_TIMEOUT)
                    .untilAsserted(() -> {
                        CONSUMER.getFederatedCatalog()
                                .log().ifValidationFails()
                                .statusCode(200)
                                .contentType(JSON)
                                .body("size()", is(1))
                                .body("[0].'dcat:dataset'.'@id'", equalTo("test-asset"));
                    });
        }
    }

    static class TestTargetNodeDirectory implements TargetNodeDirectory {

        private final List<TransferParticipant> participants;

        TestTargetNodeDirectory(List<TransferParticipant> participants) {
            this.participants = participants;
        }

        @Override
        public List<TargetNode> getAll() {
            return participants.stream()
                    .map(p -> new TargetNode(p.getDid(), p.getBpn(), p.getProtocolEndpoint().getUrl().toString(), List.of("dataspace-protocol-http")))
                    .collect(Collectors.toList());
        }

        @Override
        public void insert(TargetNode node) {

        }
    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        protected static final RuntimeExtension CONSUMER_RUNTIME = memoryRuntime(CONSUMER.getName(), CONSUMER.getBpn(), CONSUMER.getConfiguration());

        @RegisterExtension
        protected static final RuntimeExtension PROVIDER_RUNTIME = memoryRuntime(PROVIDER.getName(), PROVIDER.getBpn(), PROVIDER.getConfiguration());


        static {
            CONSUMER_RUNTIME.registerServiceMock(TargetNodeDirectory.class, new TestTargetNodeDirectory(List.of(PROVIDER)));
        }
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @RegisterExtension
        protected static final RuntimeExtension CONSUMER_RUNTIME = pgRuntime(CONSUMER.getName(), CONSUMER.getBpn(), CONSUMER.getConfiguration());

        @RegisterExtension
        protected static final RuntimeExtension PROVIDER_RUNTIME = pgRuntime(PROVIDER.getName(), PROVIDER.getBpn(), PROVIDER.getConfiguration());

        static {
            CONSUMER_RUNTIME.registerServiceMock(TargetNodeDirectory.class, new TestTargetNodeDirectory(List.of(PROVIDER)));
        }
    }

}
