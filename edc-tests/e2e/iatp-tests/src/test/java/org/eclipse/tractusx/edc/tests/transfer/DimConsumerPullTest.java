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

import com.github.tomakehurst.wiremock.WireMockServer;
import org.eclipse.edc.iam.decentralizedclaims.sts.service.EmbeddedSecureTokenService;
import org.eclipse.edc.iam.decentralizedclaims.sts.spi.model.StsAccount;
import org.eclipse.edc.iam.decentralizedclaims.sts.spi.service.StsAccountService;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.store.ParticipantContextStore;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.eclipse.edc.keys.spi.PrivateKeyResolver;
import org.eclipse.edc.security.token.jwt.DefaultJwsSignerProvider;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase;
import org.eclipse.tractusx.edc.tests.runtimes.KeyPool;
import org.eclipse.tractusx.edc.tests.transfer.extension.BdrsServerExtension;
import org.eclipse.tractusx.edc.tests.transfer.extension.DidServerExtension;
import org.eclipse.tractusx.edc.tests.transfer.iatp.dispatchers.DimDispatcher;
import org.eclipse.tractusx.edc.tests.transfer.iatp.harness.DataspaceIssuer;
import org.eclipse.tractusx.edc.tests.transfer.iatp.harness.IatpParticipant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.DSP_2025;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.DSP_2025_PATH;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.transfer.iatp.runtime.Runtimes.dimRuntime;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EndToEndTest
public class DimConsumerPullTest extends AbstractIatpConsumerPullTest {

    @RegisterExtension
    private static final DidServerExtension DID_SERVER = new DidServerExtension();

    private static final DataspaceIssuer DATASPACE_ISSUER_PARTICIPANT = new DataspaceIssuer(DID_SERVER.didFor("issuer"));
    private static final LazySupplier<URI> DIM_URI = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort()));

    private static final IatpParticipant CONSUMER = IatpParticipant.Builder.newInstance()
            .name(CONSUMER_NAME)
            .id(DID_SERVER.didFor(CONSUMER_NAME))
            .stsUri(STS.stsUri())
            .stsClientId(CONSUMER_BPN)
            .trustedIssuer(DATASPACE_ISSUER_PARTICIPANT.didUrl())
            .dimUri(DIM_URI)
            .bpn(CONSUMER_BPN)
            .protocol(DSP_2025)
            .protocolVersionPath(DSP_2025_PATH)
            .build();
    private static final IatpParticipant PROVIDER = IatpParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(DID_SERVER.didFor(PROVIDER_NAME))
            .stsUri(STS.stsUri())
            .stsClientId(PROVIDER_BPN)
            .trustedIssuer(DATASPACE_ISSUER_PARTICIPANT.didUrl())
            .dimUri(DIM_URI)
            .bpn(PROVIDER_BPN)
            .protocol(DSP_2025)
            .protocolVersionPath(DSP_2025_PATH)
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
    private static WireMockServer oauthServer;
    private static WireMockServer dimServer;

    @BeforeAll
    static void prepare() {
        KeyPool.register(DATASPACE_ISSUER_PARTICIPANT.getFullKeyId(), DATASPACE_ISSUER_PARTICIPANT.getKeyPair());
        DID_SERVER.register(CONSUMER_NAME, CONSUMER.getDidDocument());
        DID_SERVER.register(PROVIDER_NAME, PROVIDER.getDidDocument());
        DID_SERVER.register("issuer", DATASPACE_ISSUER_PARTICIPANT.didDocument());

        BDRS_SERVER_EXTENSION.addMapping(CONSUMER.getBpn(), CONSUMER.getDid());
        BDRS_SERVER_EXTENSION.addMapping(PROVIDER.getBpn(), PROVIDER.getDid());

        var consumerTokenGeneration = new JwtGenerationService(new DefaultJwsSignerProvider(CONSUMER_RUNTIME.getService(PrivateKeyResolver.class)));
        var providerTokenGeneration = new JwtGenerationService(new DefaultJwsSignerProvider(PROVIDER_RUNTIME.getService(PrivateKeyResolver.class)));

        var generatorServices = Map.of(
                CONSUMER.getDid(), tokenServiceFor(consumerTokenGeneration, CONSUMER, CONSUMER_RUNTIME),
                PROVIDER.getDid(), tokenServiceFor(providerTokenGeneration, PROVIDER, PROVIDER_RUNTIME));

        var stsUri = STS.stsUri().get();

        oauthServer = new WireMockServer(options().port(stsUri.getPort()));
        oauthServer.start();
        oauthServer.stubFor(post(urlPathEqualTo(stsUri.getPath() + "/token")).willReturn(aResponse().withStatus(200)
                .withBody(MAPPER.writeValueAsString(Map.of("access_token", "token")))));

        dimServer = new WireMockServer(options().port(DIM_URI.get().getPort()).extensions(new DimDispatcher(generatorServices)));
        dimServer.start();
        dimServer.stubFor(post(anyUrl()).willReturn(aResponse().withTransformers("dim-dispatcher")));
        
        CONSUMER.setJsonLd(CONSUMER_RUNTIME.getService(JsonLd.class));
    }

    @AfterAll
    static void unwind() {
        oauthServer.stop();
        dimServer.stop();
    }

    private static EmbeddedSecureTokenService tokenServiceFor(TokenGenerationService tokenGenerationService, IatpParticipant participant,
                                                              RuntimeExtension runtime) {
        StsAccountService stsAccountService = mock();
        when(stsAccountService.queryAccounts(any())).thenAnswer(i -> {
            var dummyId = UUID.randomUUID().toString();
            var account = StsAccount.Builder.newInstance()
                    .id(dummyId)
                    .participantContextId(participant.getDid())
                    .clientId(participant.getDid())
                    .name(participant.getName())
                    .did(participant.getDid())
                    .secretAlias(dummyId)
                    .build();

            return List.of(account);
        });

        var participantContextStore = runtime.getService(ParticipantContextStore.class);
        participantContextStore.create(ParticipantContext.Builder.newInstance()
                .participantContextId(participant.getDid())
                .did(participant.getDid())
                .apiTokenAlias(participant.getDid()).build());

        var keyPairService = runtime.getService(KeyPairService.class);
        var keyDescriptor = participant.createKeyDescriptor();
        KeyPool.register(participant.getFullKeyId(), participant.getKeyPair());

        keyPairService.addKeyPair(participant.getDid(), keyDescriptor, true)
                .orElseThrow(f -> new RuntimeException("Cannot add key pair: " + f.getFailureDetail()));

        return new EmbeddedSecureTokenService(
                new NoopTransactionContext(),
                60 * 60,
                tokenGenerationService,
                Clock.systemUTC(),
                stsAccountService,
                keyPairService
        );
    }

    // credentials etc get wiped after every, so the need to be created before every test
    @BeforeEach
    void setupParticipants() {
        CONSUMER.configureParticipant(DATASPACE_ISSUER_PARTICIPANT, CONSUMER_RUNTIME);
        PROVIDER.configureParticipant(DATASPACE_ISSUER_PARTICIPANT, PROVIDER_RUNTIME);
    }

    @Override
    protected RuntimeExtension credentialStoreRuntime() {
        return CONSUMER_RUNTIME;
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
