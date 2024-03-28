/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
import okhttp3.mockwebserver.MockWebServer;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.tractusx.edc.tests.KeycloakDispatcher;
import org.eclipse.tractusx.edc.tests.MiwDispatcher;
import org.eclipse.tractusx.edc.tests.MiwParticipant;
import org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase;
import org.eclipse.tractusx.edc.tests.runtimes.ParticipantRuntime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.Map;

import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PLATO_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.frameworkPolicy;


@EndToEndTest
public class SsiHttpConsumerPullWithProxyInMemoryTest extends HttpConsumerPullBaseTest {

    public static final String SUMMARY_VC_TEMPLATE = "summary-vc.json";
    protected static final MiwParticipant SOKRATES = MiwParticipant.Builder.newInstance()
            .name(SOKRATES_NAME)
            .id(SOKRATES_BPN)
            .build();
    protected static final MiwParticipant PLATO = MiwParticipant.Builder.newInstance()
            .name(PLATO_NAME)
            .id(PLATO_BPN)
            .build();
    @RegisterExtension
    protected static final ParticipantRuntime SOKRATES_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:runtime-memory-ssi",
            SOKRATES.getName(),
            SOKRATES.getBpn(),
            SOKRATES.getConfiguration()
    );
    @RegisterExtension
    protected static final ParticipantRuntime PLATO_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:runtime-memory-ssi",
            PLATO.getName(),
            PLATO.getBpn(),
            PLATO.getConfiguration()
    );

    private static MockWebServer sokratesOauthServer;
    private static MockWebServer platoOauthServer;
    private static MockWebServer miwPlatoServer;
    private static MockWebServer miwSokratesServer;

    @BeforeAll
    static void prepare() throws IOException {
        miwSokratesServer = new MockWebServer();
        miwPlatoServer = new MockWebServer();
        sokratesOauthServer = new MockWebServer();
        platoOauthServer = new MockWebServer();

        var credentialSubjectId = "did:web:example.com";

        miwSokratesServer.start(SOKRATES.miwEndpoint().getPort());
        miwSokratesServer.setDispatcher(new MiwDispatcher(SOKRATES_BPN, SUMMARY_VC_TEMPLATE, credentialSubjectId, PLATO.getProtocolEndpoint().getUrl().toString()));

        miwPlatoServer.start(PLATO.miwEndpoint().getPort());
        miwPlatoServer.setDispatcher(new MiwDispatcher(PLATO_BPN, SUMMARY_VC_TEMPLATE, credentialSubjectId, SOKRATES.getProtocolEndpoint().getUrl().toString()));

        sokratesOauthServer.start(SOKRATES.authTokenEndpoint().getPort());
        sokratesOauthServer.setDispatcher(new KeycloakDispatcher());

        platoOauthServer.start(PLATO.authTokenEndpoint().getPort());
        platoOauthServer.setDispatcher(new KeycloakDispatcher());
    }

    @AfterAll
    static void unwind() throws IOException {
        miwSokratesServer.shutdown();
        miwPlatoServer.shutdown();
        sokratesOauthServer.shutdown();
        platoOauthServer.shutdown();
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
    protected JsonObject createAccessPolicy(String bpn) {
        return frameworkPolicy(Map.of(TX_NAMESPACE + "Dismantler", "active"));
    }
}
