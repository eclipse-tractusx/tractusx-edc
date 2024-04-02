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

import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.ParticipantRuntime;
import org.eclipse.tractusx.edc.tests.runtimes.PgParticipantRuntime;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PLATO_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.memoryRuntime;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;

public class TransferPushEndToEndTest {

    abstract static class Tests extends ProviderPushBaseTest {

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