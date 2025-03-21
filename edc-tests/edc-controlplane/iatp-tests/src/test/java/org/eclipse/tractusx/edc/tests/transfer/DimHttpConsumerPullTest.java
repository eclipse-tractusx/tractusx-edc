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
import org.eclipse.edc.iam.identitytrust.sts.service.EmbeddedSecureTokenService;
import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsAccountService;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.keys.spi.PrivateKeyResolver;
import org.eclipse.edc.security.token.jwt.DefaultJwsSignerProvider;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.tractusx.edc.tests.transfer.iatp.dispatchers.DimDispatcher;
import org.eclipse.tractusx.edc.tests.transfer.iatp.harness.IatpParticipant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.tractusx.edc.tests.transfer.iatp.harness.IatpHelperFunctions.configureParticipant;
import static org.eclipse.tractusx.edc.tests.transfer.iatp.runtime.Runtimes.dimRuntime;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;

@EndToEndTest
public class DimHttpConsumerPullTest extends AbstractIatpConsumerPullTest {

    @RegisterExtension
    protected static final RuntimeExtension CONSUMER_RUNTIME = dimRuntime(CONSUMER.getName(), CONSUMER.getKeyPair(), () -> CONSUMER.iatpConfig(PROVIDER));
    @RegisterExtension
    protected static final RuntimeExtension PROVIDER_RUNTIME = dimRuntime(PROVIDER.getName(), PROVIDER.getKeyPair(), () -> PROVIDER.iatpConfig(CONSUMER));
    private static final TypeManager MAPPER = new JacksonTypeManager();
    private static ClientAndServer oauthServer;
    private static ClientAndServer dimServer;

    @AfterAll
    static void unwind() {
        oauthServer.stop();
        dimServer.stop();
    }

    @BeforeAll
    static void prepare() {
        var consumerTokenGeneration = new JwtGenerationService(new DefaultJwsSignerProvider(CONSUMER_RUNTIME.getService(PrivateKeyResolver.class)));
        var providerTokenGeneration = new JwtGenerationService(new DefaultJwsSignerProvider(PROVIDER_RUNTIME.getService(PrivateKeyResolver.class)));

        var generatorServices = Map.of(
                CONSUMER.getDid(), tokenServiceFor(consumerTokenGeneration, CONSUMER),
                PROVIDER.getDid(), tokenServiceFor(providerTokenGeneration, PROVIDER));

        oauthServer = ClientAndServer.startClientAndServer(STS.stsUri().getPort());

        oauthServer.when(request().withMethod("POST").withPath(STS.stsUri().getPath() + "/token"))
                .respond(HttpResponse.response(MAPPER.writeValueAsString(Map.of("access_token", "token"))));

        dimServer = ClientAndServer.startClientAndServer(DIM_URI.getPort());
        dimServer.when(request().withMethod("POST")).respond(new DimDispatcher(generatorServices));
    }

    private static EmbeddedSecureTokenService tokenServiceFor(TokenGenerationService tokenGenerationService, IatpParticipant participant) {
        StsAccountService stsAccountService = mock();
        when(stsAccountService.findById(participant.getDid())).thenAnswer(i -> {
            var dummyId = UUID.randomUUID().toString();
            var account = StsAccount.Builder.newInstance()
                    .id(dummyId)
                    .clientId(participant.getDid())
                    .name(participant.getName())
                    .did(participant.getDid())
                    .secretAlias(dummyId)
                    .privateKeyAlias(participant.getPrivateKeyAlias())
                    .publicKeyReference(participant.verificationId())
                    .build();

            return ServiceResult.success(account);
        });
        return new EmbeddedSecureTokenService(
                new NoopTransactionContext(),
                60 * 60,
                tokenGenerationService,
                Clock.systemUTC(),
                stsAccountService
        );
    }

    // credentials etc get wiped after every, so the need to be created before every test
    @BeforeEach
    void setupParticipants() {
        // create the DIDs cache
        var dids = new HashMap<String, DidDocument>();
        dids.put(DATASPACE_ISSUER_PARTICIPANT.didUrl(), DATASPACE_ISSUER_PARTICIPANT.didDocument());
        dids.put(CONSUMER.getDid(), CONSUMER.getDidDocument());
        dids.put(PROVIDER.getDid(), PROVIDER.getDidDocument());

        configureParticipant(DATASPACE_ISSUER_PARTICIPANT, CONSUMER, CONSUMER_RUNTIME, dids, null);
        configureParticipant(DATASPACE_ISSUER_PARTICIPANT, PROVIDER, PROVIDER_RUNTIME, dids, null);
    }

    @Override
    protected RuntimeExtension consumerRuntime() {
        return CONSUMER_RUNTIME;
    }

    @Override
    protected RuntimeExtension providerRuntime() {
        return PROVIDER_RUNTIME;
    }
}
