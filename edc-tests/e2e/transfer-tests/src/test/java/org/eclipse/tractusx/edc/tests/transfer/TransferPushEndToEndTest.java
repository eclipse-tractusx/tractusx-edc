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

import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.PostgresExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_DID;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_DID;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;

@EndToEndTest
public class TransferPushEndToEndTest {

    abstract static class Tests extends ProviderPushBaseTest {
        protected static final TransferParticipant CONSUMER = TransferParticipant.Builder.newInstance()
                .name(CONSUMER_NAME)
                .id(CONSUMER_DID)
                .bpn(CONSUMER_BPN)
                .build();
        protected static final TransferParticipant PROVIDER = TransferParticipant.Builder.newInstance()
                .name(PROVIDER_NAME)
                .id(PROVIDER_DID)
                .bpn(PROVIDER_BPN)
                .build();

        @Override
        public TractusxParticipantBase provider() {
            return PROVIDER;
        }

        @Override
        public TractusxParticipantBase consumer() {
            return CONSUMER;
        }
    }

    @Nested
    @EndToEndTest
    class Dsp08to08 extends Tests {

        @RegisterExtension
        @Order(0)
        private static final PostgresExtension POSTGRES = new PostgresExtension(CONSUMER.getName(), PROVIDER.getName());

        @RegisterExtension
        static final RuntimeExtension CONSUMER_RUNTIME = pgRuntime(CONSUMER, POSTGRES);

        @RegisterExtension
        private static final RuntimeExtension PROVIDER_RUNTIME = pgRuntime(PROVIDER, POSTGRES);

        @Override
        public RuntimeExtension providerRuntime() {
            return PROVIDER_RUNTIME;
        }

        @Override
        public RuntimeExtension consumerRuntime() {
            return CONSUMER_RUNTIME;
        }


        @BeforeAll
        static void beforeAll() {
            CONSUMER.setProtocol("dataspace-protocol-http");
            PROVIDER.setProtocol("dataspace-protocol-http");
            PROVIDER.setId(PROVIDER.getBpn());
        }
        
        @AfterAll
        static void afterAll() {
            PROVIDER.setId(PROVIDER.getDid());
        }
    }

    @Nested
    @EndToEndTest
    class Dsp2025to2025 extends Tests {

        @RegisterExtension
        @Order(0)
        private static final PostgresExtension POSTGRES = new PostgresExtension(CONSUMER.getName(), PROVIDER.getName());

        @RegisterExtension
        static final RuntimeExtension CONSUMER_RUNTIME = pgRuntime(CONSUMER, POSTGRES);

        @RegisterExtension
        private static final RuntimeExtension PROVIDER_RUNTIME = pgRuntime(PROVIDER, POSTGRES);

        @Override
        public RuntimeExtension providerRuntime() {
            return PROVIDER_RUNTIME;
        }

        @Override
        public RuntimeExtension consumerRuntime() {
            return CONSUMER_RUNTIME;
        }

        @BeforeAll
        static void beforeAll() {
            CONSUMER.setJsonLd(CONSUMER_RUNTIME.getService(JsonLd.class));
            CONSUMER.setProtocol("dataspace-protocol-http:2025-1", "/2025-1");
            PROVIDER.setProtocol("dataspace-protocol-http:2025-1", "/2025-1");
        }
    }
}
