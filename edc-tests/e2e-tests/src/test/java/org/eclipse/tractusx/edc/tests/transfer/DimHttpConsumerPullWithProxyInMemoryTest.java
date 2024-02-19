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
import okhttp3.mockwebserver.MockWebServer;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.identitytrust.sts.embedded.EmbeddedSecureTokenService;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.eclipse.tractusx.edc.lifecycle.ParticipantRuntime;
import org.eclipse.tractusx.edc.lifecycle.tx.iatp.DataspaceIssuer;
import org.eclipse.tractusx.edc.lifecycle.tx.iatp.IatpDimParticipant;
import org.eclipse.tractusx.edc.lifecycle.tx.iatp.SecureTokenService;
import org.eclipse.tractusx.edc.token.DimDispatcher;
import org.eclipse.tractusx.edc.token.KeycloakDispatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.URI;
import java.security.PrivateKey;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.tractusx.edc.helpers.IatpHelperFunctions.configureParticipant;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.TX_CREDENTIAL_NAMESPACE;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.frameworkPolicy;

@EndToEndTest
public class DimHttpConsumerPullWithProxyInMemoryTest extends AbstractHttpConsumerPullWithProxyTest {

    protected static final URI DIM_URI = URI.create("http://localhost:" + getFreePort());
    protected static final DataspaceIssuer DATASPACE_ISSUER_PARTICIPANT = new DataspaceIssuer();
    protected static final SecureTokenService STS_PARTICIPANT = new SecureTokenService();
    protected static final IatpDimParticipant PLATO_IATP = new IatpDimParticipant(PLATO, STS_PARTICIPANT.stsUri(), DIM_URI);

    @RegisterExtension
    protected static final ParticipantRuntime PLATO_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:iatp:runtime-memory-iatp-dim-ih",
            PLATO.getName(),
            PLATO.getBpn(),
            PLATO_IATP.iatpConfiguration(SOKRATES)
    );
    protected static final IatpDimParticipant SOKRATES_IATP = new IatpDimParticipant(SOKRATES, STS_PARTICIPANT.stsUri(), DIM_URI);

    @RegisterExtension
    protected static final ParticipantRuntime SOKRATES_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:iatp:runtime-memory-iatp-dim-ih",
            SOKRATES.getName(),
            SOKRATES.getBpn(),
            SOKRATES_IATP.iatpConfiguration(PLATO)
    );
    private static MockWebServer oauthServer;
    private static MockWebServer dimDispatcher;

    @BeforeAll
    static void prepare() throws IOException {

        var tokenGeneration = new JwtGenerationService();

        var generatorServices = Map.of(
                SOKRATES_IATP.didUrl(), tokenServiceFor(tokenGeneration, SOKRATES_IATP),
                PLATO_IATP.didUrl(), tokenServiceFor(tokenGeneration, PLATO_IATP));

        oauthServer = new MockWebServer();
        oauthServer.start(STS_PARTICIPANT.stsUri().getPort());
        oauthServer.setDispatcher(new KeycloakDispatcher(STS_PARTICIPANT.stsUri().getPath() + "/token"));

        dimDispatcher = new MockWebServer();
        dimDispatcher.start(DIM_URI.getPort());
        dimDispatcher.setDispatcher(new DimDispatcher("/iatp", generatorServices));

        // create the DIDs cache
        var dids = new HashMap<String, DidDocument>();
        dids.put(DATASPACE_ISSUER_PARTICIPANT.didUrl(), DATASPACE_ISSUER_PARTICIPANT.didDocument());
        dids.put(SOKRATES_IATP.didUrl(), SOKRATES_IATP.didDocument());
        dids.put(PLATO_IATP.didUrl(), PLATO_IATP.didDocument());

        configureParticipant(DATASPACE_ISSUER_PARTICIPANT, SOKRATES_IATP, SOKRATES_RUNTIME, dids, null);
        configureParticipant(DATASPACE_ISSUER_PARTICIPANT, PLATO_IATP, PLATO_RUNTIME, dids, null);

    }

    @AfterAll
    static void unwind() throws IOException {
        oauthServer.shutdown();
        dimDispatcher.shutdown();
    }

    private static EmbeddedSecureTokenService tokenServiceFor(TokenGenerationService tokenGenerationService, IatpDimParticipant iatpDimParticipant) {
        return new EmbeddedSecureTokenService(tokenGenerationService, privateKeySupplier(iatpDimParticipant), publicIdSupplier(iatpDimParticipant), Clock.systemUTC(), 60 * 60);
    }

    private static Supplier<PrivateKey> privateKeySupplier(IatpDimParticipant participant) {
        return () -> participant.getKeyPair().getPrivate();
    }

    private static Supplier<String> publicIdSupplier(IatpDimParticipant participant) {
        return participant::verificationId;
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
