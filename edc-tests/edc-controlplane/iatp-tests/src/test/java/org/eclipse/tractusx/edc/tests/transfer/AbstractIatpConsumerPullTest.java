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
import org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase;
import org.eclipse.tractusx.edc.tests.transfer.iatp.harness.DataspaceIssuer;
import org.eclipse.tractusx.edc.tests.transfer.iatp.harness.IatpParticipant;
import org.eclipse.tractusx.edc.tests.transfer.iatp.harness.StsParticipant;

import java.net.URI;
import java.util.Map;

import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PLATO_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.frameworkPolicy;

public abstract class AbstractIatpConsumerPullTest extends HttpConsumerPullBaseTest {

    protected static final URI DIM_URI = URI.create("http://localhost:" + getFreePort());
    protected static final DataspaceIssuer DATASPACE_ISSUER_PARTICIPANT = new DataspaceIssuer();
    protected static final StsParticipant STS = StsParticipant.Builder.newInstance()
            .id("STS")
            .name("STS")
            .build();
    protected static final IatpParticipant SOKRATES = IatpParticipant.Builder.newInstance()
            .name(SOKRATES_NAME)
            .id(SOKRATES_BPN)
            .stsUri(STS.stsUri())
            .stsClientId(SOKRATES_BPN)
            .stsClientSecret("client_secret")
            .trustedIssuer(DATASPACE_ISSUER_PARTICIPANT.didUrl())
            .dimUri(DIM_URI)
            .did(did(SOKRATES_NAME))
            .build();
    protected static final IatpParticipant PLATO = IatpParticipant.Builder.newInstance()
            .name(PLATO_NAME)
            .id(PLATO_BPN)
            .stsUri(STS.stsUri())
            .stsClientId(PLATO_BPN)
            .stsClientSecret("client_secret")
            .trustedIssuer(DATASPACE_ISSUER_PARTICIPANT.didUrl())
            .dimUri(DIM_URI)
            .did(did(PLATO_NAME))
            .build();

    private static String did(String name) {
        return "did:example:" + name.toLowerCase();
    }


    @Override
    public TractusxParticipantBase plato() {
        return PLATO;
    }

    @Override
    public TractusxParticipantBase sokrates() {
        return SOKRATES;
    }

    @Override
    protected JsonObject createContractPolicy(String bpn) {
        return frameworkPolicy(Map.of(CX_POLICY_NS + "Membership", "active"));
    }

}
