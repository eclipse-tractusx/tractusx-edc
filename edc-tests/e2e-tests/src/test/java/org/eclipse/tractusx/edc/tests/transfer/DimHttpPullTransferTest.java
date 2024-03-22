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

import org.eclipse.tractusx.edc.lifecycle.DimParticipant;
import org.eclipse.tractusx.edc.lifecycle.ParticipantRuntime;
import org.eclipse.tractusx.edc.tag.DimIntegrationTest;
import org.eclipse.tractusx.edc.tests.TractusxParticipantBase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.eclipse.tractusx.edc.helpers.DimHelper.configureParticipant;
import static org.eclipse.tractusx.edc.lifecycle.Runtimes.dimRuntime;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PLATO_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.SOKRATES_NAME;

@DimIntegrationTest
@Disabled
public class DimHttpPullTransferTest extends HttpConsumerPullBaseTest {

    protected static final DimParticipant SOKRATES = configureParticipant(SOKRATES_NAME);
    protected static final DimParticipant PLATO = configureParticipant(PLATO_NAME);

    @RegisterExtension
    protected static final ParticipantRuntime PLATO_RUNTIME = dimRuntime(PLATO.getName(), PLATO.iatpConfiguration(SOKRATES));

    @RegisterExtension
    protected static final ParticipantRuntime SOKRATES_RUNTIME = dimRuntime(SOKRATES.getName(), SOKRATES.iatpConfiguration(PLATO));

    @Override
    protected TractusxParticipantBase plato() {
        return PLATO;
    }

    @Override
    protected TractusxParticipantBase sokrates() {
        return SOKRATES;
    }
}
