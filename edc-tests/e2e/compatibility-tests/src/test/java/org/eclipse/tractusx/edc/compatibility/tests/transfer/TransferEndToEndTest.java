/*******************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2025 Cofinity-X GmbH
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
 ******************************************************************************/

package org.eclipse.tractusx.edc.compatibility.tests.transfer;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.iam.AudienceResolver;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.tractusx.edc.compatibility.tests.CompatibilityTest;
import org.eclipse.tractusx.edc.compatibility.tests.fixtures.IdentityHubParticipant;
import org.eclipse.tractusx.edc.compatibility.tests.fixtures.RemoteParticipant;
import org.eclipse.tractusx.edc.compatibility.tests.fixtures.RemoteParticipantExtension;
import org.eclipse.tractusx.edc.compatibility.tests.fixtures.Runtimes;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.eclipse.tractusx.edc.tests.participant.DataspaceIssuer;
import org.eclipse.tractusx.edc.tests.participant.IatpParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.PostgresExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDED;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.compatibility.tests.fixtures.DcpHelperFunctions.configureParticipant;
import static org.eclipse.tractusx.edc.compatibility.tests.fixtures.DcpHelperFunctions.configureParticipantContext;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.inForceDatePolicyLegacy;

@CompatibilityTest
public class TransferEndToEndTest {

    protected static final IdentityHubParticipant IDENTITY_HUB_PARTICIPANT = IdentityHubParticipant.Builder.newInstance()
            .name("identity-hub")
            .id("identity-hub")
            .build();

    protected static final DataspaceIssuer ISSUER = DataspaceIssuer.Builder.newInstance().id("issuer").name("issuer")
            .did(IDENTITY_HUB_PARTICIPANT.didFor("issuer"))
            .build();

    protected static final RemoteParticipant REMOTE_PARTICIPANT = RemoteParticipant.Builder.newInstance()
            .name("remote")
            .id("remote")
            .stsUri(IDENTITY_HUB_PARTICIPANT.getSts())
            .did(IDENTITY_HUB_PARTICIPANT.didFor("remote"))
            .trustedIssuer(ISSUER.didUrl())
            .build();

    static final IatpParticipant LOCAL_PARTICIPANT = IatpParticipant.Builder.newInstance()
            .name("local")
            .id("local")
            .stsUri(IDENTITY_HUB_PARTICIPANT.getSts())
            .did(IDENTITY_HUB_PARTICIPANT.didFor("local"))
            .bpn("local")
            .trustedIssuer(ISSUER.didUrl())
            .build();

    private static final Map<String, String> DIDS = Map.of(
            LOCAL_PARTICIPANT.getId(), LOCAL_PARTICIPANT.getDid(),
            REMOTE_PARTICIPANT.getId(), REMOTE_PARTICIPANT.getDid()
    );

    @Order(0)
    @RegisterExtension
    private static final PostgresExtension POSTGRES = new PostgresExtension(LOCAL_PARTICIPANT.getName(), REMOTE_PARTICIPANT.getName());

    @Order(1)
    @RegisterExtension
    static final RuntimeExtension LOCAL_CONNECTOR = new RuntimePerClassExtension(
            Runtimes.SNAPSHOT_CONNECTOR.create("local-connector")
                    .configurationProvider(() -> POSTGRES.getConfig(LOCAL_PARTICIPANT.getName()))
                    .configurationProvider(LOCAL_PARTICIPANT::iatpConfig)
                    .registerServiceMock(BdrsClient.class, new BdrsClient() {
                        @Override
                        public String resolveDid(String bpn) {
                            return DIDS.get(bpn);
                        }

                        @Override
                        public String resolveBpn(String did) {
                            return DIDS.entrySet().stream()
                                    .filter(entry -> entry.getValue().equals(did))
                                    .findFirst().orElseThrow().getKey();
                        }
                    })
                    .registerServiceMock(AudienceResolver.class, message -> Result
                            .success(DIDS.get(message.getCounterPartyId()))));

    @Order(2)
    @RegisterExtension
    static final RuntimeExtension LOCAL_IDENTITY_HUB = new RuntimePerClassExtension(
            Runtimes.IDENTITY_HUB.create("local-identity-hub")
                    .configurationProvider(IDENTITY_HUB_PARTICIPANT::getConfig));

    @Order(3)
    @RegisterExtension
    static final RemoteParticipantExtension REMOTE_PARTICIPANT_EXTENSION = new RemoteParticipantExtension(REMOTE_PARTICIPANT, LOCAL_PARTICIPANT, POSTGRES);

    @RegisterExtension
    static WireMockExtension providerDataSource = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @BeforeAll
    static void beforeAll() {
        configureParticipant(LOCAL_PARTICIPANT, ISSUER, IDENTITY_HUB_PARTICIPANT, LOCAL_IDENTITY_HUB);
        configureParticipant(REMOTE_PARTICIPANT, ISSUER, IDENTITY_HUB_PARTICIPANT, LOCAL_IDENTITY_HUB);
        configureParticipantContext(ISSUER, IDENTITY_HUB_PARTICIPANT, LOCAL_IDENTITY_HUB);

        var vault = LOCAL_CONNECTOR.getService(Vault.class);
        vault.storeSecret(LOCAL_PARTICIPANT.getPrivateKeyAlias(), LOCAL_PARTICIPANT.getPrivateKeyAsString());
        vault.storeSecret(LOCAL_PARTICIPANT.getFullKeyId(), LOCAL_PARTICIPANT.getPublicKeyAsString());
        vault.storeSecret("client_secret_alias", "clientSecret");
    }

    @ParameterizedTest
    @ArgumentsSource(ParticipantsArgProvider.class)
    void httpPullTransfer(IatpParticipant consumer, IatpParticipant provider, String protocol) {
        consumer.setProtocol(protocol);
        provider.setProtocol(protocol);
        providerDataSource.stubFor(any(anyUrl()).willReturn(ok("data")));
        var assetId = UUID.randomUUID().toString();
        var usagePolicy = inForceDatePolicyLegacy("gteq", "contractAgreement+0s", "lteq", "contractAgreement+5s");
        createResourcesOnProvider(provider, assetId, usagePolicy, httpSourceDataAddress());

        var transferProcessId = consumer.requestAssetFrom(assetId, provider)
                .withTransferType("HttpData-PULL")
                .execute();

        consumer.awaitTransferToBeInState(transferProcessId, STARTED);

        var edr = await().atMost(consumer.getTimeout())
                .until(() -> consumer.edrs().getEdr(transferProcessId), Objects::nonNull);

        // Do the transfer
        var msg = UUID.randomUUID().toString();
        var data = consumer.data().pullData(edr, Map.of("message", msg));
        assertThat(data).isNotNull().isEqualTo("data");

        // checks that the EDR is gone once the contract expires
        await().atMost(consumer.getTimeout())
                .untilAsserted(() -> assertThatThrownBy(() -> consumer.edrs().getEdr(transferProcessId)));

        // checks that transfer fails
        await().atMost(consumer.getTimeout()).untilAsserted(() -> assertThatThrownBy(() -> consumer.data().pullData(edr, Map.of("message", msg))));

        providerDataSource.verify(getRequestedFor(urlPathEqualTo("/source")));
    }

    @ParameterizedTest
    @ArgumentsSource(ParticipantsArgProvider.class)
    void suspendAndResume_httpPull_dataTransfer(IatpParticipant consumer, IatpParticipant provider, String protocol) {
        consumer.setProtocol(protocol);
        provider.setProtocol(protocol);
        providerDataSource.stubFor(any(anyUrl()).willReturn(ok("data")));
        var assetId = UUID.randomUUID().toString();
        createResourcesOnProvider(provider, assetId, PolicyFixtures.noConstraintPolicy(), httpSourceDataAddress());

        var transferProcessId = consumer.requestAssetFrom(assetId, provider)
                .withTransferType("HttpData-PULL")
                .execute();

        consumer.awaitTransferToBeInState(transferProcessId, STARTED);

        var edr = await().atMost(consumer.getTimeout()).until(() -> consumer.edrs().getEdr(transferProcessId), Objects::nonNull);

        var msg = UUID.randomUUID().toString();
        var data = consumer.data().pullData(edr, Map.of("message", msg));
        assertThat(data).isNotNull().isEqualTo("data");

        consumer.suspendTransfer(transferProcessId, "supension");

        consumer.awaitTransferToBeInState(transferProcessId, SUSPENDED);

        // checks that the EDR is gone once the transfer has been suspended
        await().atMost(consumer.getTimeout()).untilAsserted(() -> assertThatThrownBy(() -> consumer.edrs().getEdr(transferProcessId)));
        // checks that transfer fails
        await().atMost(consumer.getTimeout()).untilAsserted(() -> assertThatThrownBy(() -> consumer.data().pullData(edr, Map.of("message", msg))));

        consumer.resumeTransfer(transferProcessId);

        // check that transfer is available again
        consumer.awaitTransferToBeInState(transferProcessId, STARTED);
        var secondEdr = await().atMost(consumer.getTimeout()).until(() -> consumer.edrs().getEdr(transferProcessId), Objects::nonNull);
        var secondMessage = UUID.randomUUID().toString();
        data = consumer.data().pullData(secondEdr, Map.of("message", secondMessage));
        assertThat(data).isNotNull().isEqualTo("data");

        providerDataSource.verify(getRequestedFor(urlPathEqualTo("/source")));
    }

    protected void createResourcesOnProvider(IatpParticipant provider, String assetId, JsonObject contractPolicy, Map<String, Object> dataAddressProperties) {
        provider.createAsset(assetId, Map.of("description", "description"), dataAddressProperties);
        var contractPolicyId = provider.createPolicyDefinition(contractPolicy);
        var noConstraintPolicyId = provider.createPolicyDefinition(noConstraintPolicy());

        provider.createContractDefinition(assetId, UUID.randomUUID().toString(), noConstraintPolicyId, contractPolicyId);
    }

    private @NotNull Map<String, Object> httpSourceDataAddress() {
        return Map.of(
                EDC_NAMESPACE + "name", "transfer-test",
                EDC_NAMESPACE + "baseUrl", "http://localhost:" + providerDataSource.getPort() + "/source",
                EDC_NAMESPACE + "type", "HttpData",
                EDC_NAMESPACE + "proxyQueryParams", "true"
        );
    }

    private static class ParticipantsArgProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(REMOTE_PARTICIPANT, LOCAL_PARTICIPANT, "dataspace-protocol-http"),
                    Arguments.of(LOCAL_PARTICIPANT, REMOTE_PARTICIPANT, "dataspace-protocol-http")
            );
        }
    }
}
