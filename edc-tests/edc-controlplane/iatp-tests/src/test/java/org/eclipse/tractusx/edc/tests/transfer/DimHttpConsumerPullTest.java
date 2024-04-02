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

import okhttp3.mockwebserver.MockWebServer;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.identitytrust.sts.embedded.EmbeddedSecureTokenService;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.eclipse.tractusx.edc.tests.transfer.iatp.dispatchers.DimDispatcher;
import org.eclipse.tractusx.edc.tests.transfer.iatp.dispatchers.KeycloakDispatcher;
import org.eclipse.tractusx.edc.tests.transfer.iatp.harness.IatpParticipant;
import org.eclipse.tractusx.edc.tests.transfer.iatp.runtime.IatpParticipantRuntime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.security.PrivateKey;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.eclipse.tractusx.edc.tests.transfer.iatp.harness.IatpHelperFunctions.configureParticipant;
import static org.eclipse.tractusx.edc.tests.transfer.iatp.runtime.Runtimes.dimRuntime;

@EndToEndTest
public class DimHttpConsumerPullTest extends AbstractIatpConsumerPullTest {


    @RegisterExtension
    protected static final IatpParticipantRuntime SOKRATES_RUNTIME = dimRuntime(SOKRATES.getName(), SOKRATES.iatpConfiguration(PLATO), SOKRATES.getKeyPair());

    @RegisterExtension
    protected static final IatpParticipantRuntime PLATO_RUNTIME = dimRuntime(PLATO.getName(), PLATO.iatpConfiguration(SOKRATES), PLATO.getKeyPair());
    private static MockWebServer oauthServer;
    private static MockWebServer dimDispatcher;

    @BeforeAll
    static void prepare() throws IOException {

        var tokenGeneration = new JwtGenerationService();

        var generatorServices = Map.of(
                SOKRATES.getDid(), tokenServiceFor(tokenGeneration, SOKRATES),
                PLATO.getDid(), tokenServiceFor(tokenGeneration, PLATO));

        oauthServer = new MockWebServer();
        oauthServer.start(STS.stsUri().getPort());
        oauthServer.setDispatcher(new KeycloakDispatcher(STS.stsUri().getPath() + "/token"));

        dimDispatcher = new MockWebServer();
        dimDispatcher.start(DIM_URI.getPort());
        dimDispatcher.setDispatcher(new DimDispatcher(generatorServices));

        // create the DIDs cache
        var dids = new HashMap<String, DidDocument>();
        dids.put(DATASPACE_ISSUER_PARTICIPANT.didUrl(), DATASPACE_ISSUER_PARTICIPANT.didDocument());
        dids.put(SOKRATES.getDid(), SOKRATES.getDidDocument());
        dids.put(PLATO.getDid(), PLATO.getDidDocument());

        configureParticipant(DATASPACE_ISSUER_PARTICIPANT, SOKRATES, SOKRATES_RUNTIME, dids, null);
        configureParticipant(DATASPACE_ISSUER_PARTICIPANT, PLATO, PLATO_RUNTIME, dids, null);

    }

    @AfterAll
    static void unwind() throws IOException {
        oauthServer.shutdown();
        dimDispatcher.shutdown();
    }

    private static EmbeddedSecureTokenService tokenServiceFor(TokenGenerationService tokenGenerationService, IatpParticipant iatpDimParticipant) {
        return new EmbeddedSecureTokenService(tokenGenerationService, privateKeySupplier(iatpDimParticipant), publicIdSupplier(iatpDimParticipant), Clock.systemUTC(), 60 * 60);
    }

    private static Supplier<PrivateKey> privateKeySupplier(IatpParticipant participant) {
        return () -> participant.getKeyPair().getPrivate();
    }

    private static Supplier<String> publicIdSupplier(IatpParticipant participant) {
        return participant::verificationId;
    }


}
