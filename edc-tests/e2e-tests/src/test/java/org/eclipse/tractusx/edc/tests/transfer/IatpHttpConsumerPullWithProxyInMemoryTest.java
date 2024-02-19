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
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.tractusx.edc.lifecycle.ParticipantRuntime;
import org.eclipse.tractusx.edc.lifecycle.tx.iatp.DataspaceIssuer;
import org.eclipse.tractusx.edc.lifecycle.tx.iatp.IatpParticipant;
import org.eclipse.tractusx.edc.lifecycle.tx.iatp.SecureTokenService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.tractusx.edc.helpers.IatpHelperFunctions.configureParticipant;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.TX_CREDENTIAL_NAMESPACE;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.frameworkPolicy;

@EndToEndTest
public class IatpHttpConsumerPullWithProxyInMemoryTest extends AbstractHttpConsumerPullWithProxyTest {

    protected static final DataspaceIssuer DATASPACE_ISSUER_PARTICIPANT = new DataspaceIssuer();
    protected static final SecureTokenService STS_PARTICIPANT = new SecureTokenService();

    protected static final IatpParticipant PLATO_IATP = new IatpParticipant(PLATO, STS_PARTICIPANT.stsUri());

    @RegisterExtension
    protected static final ParticipantRuntime PLATO_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:iatp:runtime-memory-iatp-ih",
            PLATO.getName(),
            PLATO.getBpn(),
            PLATO_IATP.iatpConfiguration(SOKRATES)
    );

    protected static final IatpParticipant SOKRATES_IATP = new IatpParticipant(SOKRATES, STS_PARTICIPANT.stsUri());

    @RegisterExtension
    protected static final ParticipantRuntime SOKRATES_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:iatp:runtime-memory-iatp-ih",
            SOKRATES.getName(),
            SOKRATES.getBpn(),
            SOKRATES_IATP.iatpConfiguration(PLATO)
    );

    @RegisterExtension
    protected static final ParticipantRuntime STS_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:iatp:runtime-memory-sts",
            STS_PARTICIPANT.getName(),
            STS_PARTICIPANT.getBpn(),
            STS_PARTICIPANT.stsConfiguration(SOKRATES_IATP, PLATO_IATP)
    );

    @BeforeAll
    static void prepare() {

        // create the DIDs cache
        var dids = new HashMap<String, DidDocument>();
        dids.put(DATASPACE_ISSUER_PARTICIPANT.didUrl(), DATASPACE_ISSUER_PARTICIPANT.didDocument());
        dids.put(SOKRATES_IATP.didUrl(), SOKRATES_IATP.didDocument());
        dids.put(PLATO_IATP.didUrl(), PLATO_IATP.didDocument());

        configureParticipant(DATASPACE_ISSUER_PARTICIPANT, SOKRATES_IATP, SOKRATES_RUNTIME, dids, STS_RUNTIME);
        configureParticipant(DATASPACE_ISSUER_PARTICIPANT, PLATO_IATP, PLATO_RUNTIME, dids, STS_RUNTIME);

    }


    @BeforeEach
    void setup() throws IOException {
        super.setup();
    }

    @Override
    protected JsonObject createContractPolicy(String bpn) {
        return frameworkPolicy(Map.of(TX_CREDENTIAL_NAMESPACE + "Membership", "active"));
    }

}
