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

package org.eclipse.tractusx.edc.tests.transfer;

import jakarta.json.JsonObject;
import okhttp3.mockwebserver.MockWebServer;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.tractusx.edc.lifecycle.ParticipantRuntime;
import org.eclipse.tractusx.edc.token.KeycloakDispatcher;
import org.eclipse.tractusx.edc.token.MiwDispatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.Map;

import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.TX_NAMESPACE;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.frameworkPolicy;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_BPN;

@EndToEndTest
public class SsiHttpConsumerPullWithProxyInMemoryTest extends AbstractHttpConsumerPullWithProxyTest {

    public static final String SUMMARY_VC_TEMPLATE = "summary-vc.json";
    @RegisterExtension
    protected static final ParticipantRuntime SOKRATES_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:runtime-memory-ssi",
            SOKRATES.getName(),
            SOKRATES.getBpn(),
            SOKRATES.ssiConfiguration()
    );
    @RegisterExtension
    protected static final ParticipantRuntime PLATO_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:runtime-memory-ssi",
            PLATO.getName(),
            PLATO.getBpn(),
            PLATO.ssiConfiguration()
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

    @BeforeEach
    void setup() throws IOException {
        super.setup();

    }

    @Override
    protected JsonObject createTestPolicy(String bpn) {
        return frameworkPolicy(Map.of(TX_NAMESPACE + "Dismantler", "active"));
    }
}
