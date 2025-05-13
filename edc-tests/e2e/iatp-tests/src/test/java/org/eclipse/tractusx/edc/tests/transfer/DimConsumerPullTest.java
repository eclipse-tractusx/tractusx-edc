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

import org.eclipse.edc.connector.controlplane.test.system.utils.LazySupplier;
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
import org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase;
import org.eclipse.tractusx.edc.tests.transfer.extension.BdrsServerExtension;
import org.eclipse.tractusx.edc.tests.transfer.extension.DidServerExtension;
import org.eclipse.tractusx.edc.tests.transfer.iatp.dispatchers.DimDispatcher;
import org.eclipse.tractusx.edc.tests.transfer.iatp.harness.DataspaceIssuer;
import org.eclipse.tractusx.edc.tests.transfer.iatp.harness.IatpParticipant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.net.URI;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.transfer.iatp.harness.IatpHelperFunctions.configureParticipant;
import static org.eclipse.tractusx.edc.tests.transfer.iatp.runtime.Runtimes.dimRuntime;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;

@EndToEndTest
public class DimConsumerPullTest extends AbstractIatpConsumerPullTest {

    @RegisterExtension
    private static final DidServerExtension DID_SERVER = new DidServerExtension();

    private static final DataspaceIssuer DATASPACE_ISSUER_PARTICIPANT = new DataspaceIssuer(DID_SERVER.didFor("issuer"));
    private static final LazySupplier<URI> DIM_URI = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort()));

    private static final IatpParticipant CONSUMER = IatpParticipant.Builder.newInstance()
            .name(CONSUMER_NAME)
            .id(CONSUMER_BPN)
            .stsUri(STS.stsUri())
            .stsClientId(CONSUMER_BPN)
            .trustedIssuer(DATASPACE_ISSUER_PARTICIPANT.didUrl())
            .dimUri(DIM_URI)
            .did(DID_SERVER.didFor(CONSUMER_NAME))
            .build();
    private static final IatpParticipant PROVIDER = IatpParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_BPN)
            .stsUri(STS.stsUri())
            .stsClientId(PROVIDER_BPN)
            .trustedIssuer(DATASPACE_ISSUER_PARTICIPANT.didUrl())
            .dimUri(DIM_URI)
            .did(DID_SERVER.didFor(PROVIDER_NAME))
            .build();

    @RegisterExtension
    private static final BdrsServerExtension BDRS_SERVER_EXTENSION = new BdrsServerExtension(DATASPACE_ISSUER_PARTICIPANT.didUrl());

    @RegisterExtension
    private static final RuntimeExtension CONSUMER_RUNTIME = dimRuntime(CONSUMER.getName(), CONSUMER.getKeyPair(),
            () -> CONSUMER.iatpConfig().merge(BDRS_SERVER_EXTENSION.getConfig()));
    @RegisterExtension
    private static final RuntimeExtension PROVIDER_RUNTIME = dimRuntime(PROVIDER.getName(), PROVIDER.getKeyPair(),
            () -> PROVIDER.iatpConfig().merge(BDRS_SERVER_EXTENSION.getConfig()));

    private static final TypeManager MAPPER = new JacksonTypeManager();
    private static ClientAndServer oauthServer;
    private static ClientAndServer dimServer;

    @BeforeAll
    static void prepare() {
        DID_SERVER.register(CONSUMER_NAME, CONSUMER.getDidDocument());
        DID_SERVER.register(PROVIDER_NAME, PROVIDER.getDidDocument());
        DID_SERVER.register("issuer", DATASPACE_ISSUER_PARTICIPANT.didDocument());

        BDRS_SERVER_EXTENSION.addMapping(CONSUMER.getBpn(), CONSUMER.getDid());
        BDRS_SERVER_EXTENSION.addMapping(PROVIDER.getBpn(), PROVIDER.getDid());

        var consumerTokenGeneration = new JwtGenerationService(new DefaultJwsSignerProvider(CONSUMER_RUNTIME.getService(PrivateKeyResolver.class)));
        var providerTokenGeneration = new JwtGenerationService(new DefaultJwsSignerProvider(PROVIDER_RUNTIME.getService(PrivateKeyResolver.class)));

        var generatorServices = Map.of(
                CONSUMER.getDid(), tokenServiceFor(consumerTokenGeneration, CONSUMER),
                PROVIDER.getDid(), tokenServiceFor(providerTokenGeneration, PROVIDER));

        var stsUri = STS.stsUri().get();
        oauthServer = ClientAndServer.startClientAndServer(stsUri.getPort());

        oauthServer.when(request().withMethod("POST").withPath(stsUri.getPath() + "/token"))
                .respond(HttpResponse.response(MAPPER.writeValueAsString(Map.of("access_token", "token"))));

        dimServer = ClientAndServer.startClientAndServer(DIM_URI.get().getPort());
        dimServer.when(request().withMethod("POST")).respond(new DimDispatcher(generatorServices));
    }

    @AfterAll
    static void unwind() {
        oauthServer.stop();
        dimServer.stop();
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
        configureParticipant(DATASPACE_ISSUER_PARTICIPANT, CONSUMER, CONSUMER_RUNTIME, null);
        configureParticipant(DATASPACE_ISSUER_PARTICIPANT, PROVIDER, PROVIDER_RUNTIME, null);
    }

    @Override
    protected RuntimeExtension consumerRuntime() {
        return CONSUMER_RUNTIME;
    }

    @Override
    protected RuntimeExtension providerRuntime() {
        return PROVIDER_RUNTIME;
    }

    @Override
    protected DataspaceIssuer dataspaceIssuer() {
        return DATASPACE_ISSUER_PARTICIPANT;
    }

    @Override
    public TractusxParticipantBase provider() {
        return PROVIDER;
    }

    @Override
    public TractusxParticipantBase consumer() {
        return CONSUMER;
    }
}
