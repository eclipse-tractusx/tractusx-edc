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

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.tractusx.edc.tests.transfer.iatp.runtime.IatpParticipantRuntime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;

import static org.eclipse.tractusx.edc.tests.transfer.iatp.harness.IatpHelperFunctions.configureParticipant;
import static org.eclipse.tractusx.edc.tests.transfer.iatp.runtime.Runtimes.iatpRuntime;
import static org.eclipse.tractusx.edc.tests.transfer.iatp.runtime.Runtimes.stsRuntime;

@EndToEndTest
public class StsHttpConsumerPullTest extends AbstractIatpConsumerPullTest {


    @RegisterExtension
    protected static final IatpParticipantRuntime SOKRATES_RUNTIME = iatpRuntime(SOKRATES.getName(), SOKRATES.iatpConfiguration(PLATO), SOKRATES.getKeyPair());

    @RegisterExtension
    protected static final IatpParticipantRuntime PLATO_RUNTIME = iatpRuntime(PLATO.getName(), PLATO.iatpConfiguration(SOKRATES), PLATO.getKeyPair());

    @RegisterExtension
    protected static final IatpParticipantRuntime STS_RUNTIME = stsRuntime(STS.getName(), STS.stsConfiguration(SOKRATES, PLATO), STS.getKeyPair());

    @BeforeAll
    static void prepare() {

        // create the DIDs cache
        var dids = new HashMap<String, DidDocument>();
        dids.put(DATASPACE_ISSUER_PARTICIPANT.didUrl(), DATASPACE_ISSUER_PARTICIPANT.didDocument());
        dids.put(SOKRATES.getDid(), SOKRATES.getDidDocument());
        dids.put(PLATO.getDid(), PLATO.getDidDocument());

        configureParticipant(DATASPACE_ISSUER_PARTICIPANT, SOKRATES, SOKRATES_RUNTIME, dids, STS_RUNTIME);
        configureParticipant(DATASPACE_ISSUER_PARTICIPANT, PLATO, PLATO_RUNTIME, dids, STS_RUNTIME);

    }

    @Override
    protected IatpParticipantRuntime sokratesRuntime() {
        return SOKRATES_RUNTIME;
    }

    @Override
    protected IatpParticipantRuntime platoRuntime() {
        return PLATO_RUNTIME;
    }
}
