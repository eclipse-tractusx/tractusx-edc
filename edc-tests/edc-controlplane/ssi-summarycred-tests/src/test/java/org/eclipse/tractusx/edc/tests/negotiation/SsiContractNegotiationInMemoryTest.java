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

package org.eclipse.tractusx.edc.tests.negotiation;

import okhttp3.mockwebserver.MockWebServer;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.tractusx.edc.tests.KeycloakDispatcher;
import org.eclipse.tractusx.edc.tests.MiwDispatcher;
import org.eclipse.tractusx.edc.tests.runtimes.ParticipantRuntime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;

import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.SOKRATES_BPN;


@EndToEndTest
public class SsiContractNegotiationInMemoryTest extends AbstractContractNegotiateTest {
    public static final String SUMMARY_VC_TEMPLATE = "summary-vc-no-dismantler.json";

    @RegisterExtension
    protected static final ParticipantRuntime PLATO_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:runtime-memory-ssi",
            PLATO.getName(),
            PLATO.getBpn(),
            PLATO_SSI.ssiConfiguration(PLATO)
    );

    @RegisterExtension
    protected static final ParticipantRuntime SOKRATES_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:runtime-memory-ssi",
            SOKRATES.getName(),
            SOKRATES.getBpn(),
            SOKRATES_SSI.ssiConfiguration(SOKRATES)
    );
    private static MockWebServer miwSokratesServer;
    private static MockWebServer miwPlatoServer;
    private static MockWebServer sokratesOauthServer;
    private static MockWebServer platoOauthServer;


    @BeforeAll
    static void setup() throws IOException {
        miwSokratesServer = new MockWebServer();
        miwPlatoServer = new MockWebServer();
        sokratesOauthServer = new MockWebServer();
        platoOauthServer = new MockWebServer();


        var credentialSubjectId = "did:web:example.com";

        miwSokratesServer.start(SOKRATES_SSI.miwEndpoint().getPort());
        miwSokratesServer.setDispatcher(new MiwDispatcher(SOKRATES_BPN, SUMMARY_VC_TEMPLATE, credentialSubjectId, PLATO.getProtocolEndpoint().getUrl().toString()));

        miwPlatoServer.start(PLATO_SSI.miwEndpoint().getPort());
        miwPlatoServer.setDispatcher(new MiwDispatcher(PLATO_BPN, SUMMARY_VC_TEMPLATE, credentialSubjectId, SOKRATES.getProtocolEndpoint().getUrl().toString()));

        sokratesOauthServer.start(SOKRATES_SSI.authTokenEndpoint().getPort());
        sokratesOauthServer.setDispatcher(new KeycloakDispatcher());

        platoOauthServer.start(PLATO_SSI.authTokenEndpoint().getPort());
        platoOauthServer.setDispatcher(new KeycloakDispatcher());
    }

    @AfterAll
    static void teardown() throws IOException {
        miwSokratesServer.shutdown();
        miwPlatoServer.shutdown();
        sokratesOauthServer.shutdown();
        platoOauthServer.shutdown();

    }
}
