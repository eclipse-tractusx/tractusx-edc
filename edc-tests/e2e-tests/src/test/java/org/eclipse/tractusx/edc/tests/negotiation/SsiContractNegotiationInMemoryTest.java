/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.tests.negotiation;

import okhttp3.mockwebserver.MockWebServer;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.tractusx.edc.lifecycle.ParticipantRuntime;
import org.eclipse.tractusx.edc.token.KeycloakDispatcher;
import org.eclipse.tractusx.edc.token.MiwDispatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;

import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_BPN;

@EndToEndTest
public class SsiContractNegotiationInMemoryTest extends AbstractContractNegotiateTest {
    public static final String SUMMARY_VC_TEMPLATE = "summary-vc-no-dismantler.json";

    @RegisterExtension
    protected static final ParticipantRuntime PLATO_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:runtime-memory-ssi",
            PLATO.getName(),
            PLATO.getBpn(),
            PLATO.ssiConfiguration()
    );

    @RegisterExtension
    protected static final ParticipantRuntime SOKRATES_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:runtime-memory-ssi",
            SOKRATES.getName(),
            SOKRATES.getBpn(),
            SOKRATES.ssiConfiguration()
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

        miwSokratesServer.start(SOKRATES.miwEndpoint().getPort());
        miwSokratesServer.setDispatcher(new MiwDispatcher(SOKRATES_BPN, SUMMARY_VC_TEMPLATE, credentialSubjectId, PLATO.protocolEndpoint().toString()));

        miwPlatoServer.start(PLATO.miwEndpoint().getPort());
        miwPlatoServer.setDispatcher(new MiwDispatcher(PLATO_BPN, SUMMARY_VC_TEMPLATE, credentialSubjectId, SOKRATES.protocolEndpoint().toString()));

        sokratesOauthServer.start(SOKRATES.authTokenEndpoint().getPort());
        sokratesOauthServer.setDispatcher(new KeycloakDispatcher());

        platoOauthServer.start(PLATO.authTokenEndpoint().getPort());
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
