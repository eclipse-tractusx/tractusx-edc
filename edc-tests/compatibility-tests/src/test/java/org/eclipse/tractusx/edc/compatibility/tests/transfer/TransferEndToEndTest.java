/*******************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2026 Cofinity-X GmbH
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
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.iam.AudienceResolver;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.tractusx.edc.compatibility.tests.CompatibilityTest;
import org.eclipse.tractusx.edc.compatibility.tests.fixtures.IdentityHubParticipant;
import org.eclipse.tractusx.edc.compatibility.tests.fixtures.LegacyRemoteParticipant;
import org.eclipse.tractusx.edc.compatibility.tests.fixtures.RemoteParticipant;
import org.eclipse.tractusx.edc.compatibility.tests.fixtures.RemoteParticipantExtension;
import org.eclipse.tractusx.edc.compatibility.tests.fixtures.Runtimes;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.eclipse.tractusx.edc.tests.participant.DataspaceIssuer;
import org.eclipse.tractusx.edc.tests.participant.DcpParticipant;
import org.eclipse.tractusx.edc.tests.participant.TractusxDcpParticipantBase;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.compatibility.tests.fixtures.DcpHelperFunctions.configureParticipant;
import static org.eclipse.tractusx.edc.compatibility.tests.fixtures.DcpHelperFunctions.configureParticipantContext;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.DSP_2025;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.ODRL_CONTEXT;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.inForceDatePolicy;
import static org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase.ASYNC_POLL_INTERVAL;
import static org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase.ASYNC_TIMEOUT;

@CompatibilityTest
public class TransferEndToEndTest {

    private static final String DSP_2025_PATH_SUFFIX = "/2025-1";

    protected static final IdentityHubParticipant IDENTITY_HUB_PARTICIPANT = IdentityHubParticipant.Builder.newInstance()
            .name("identity-hub")
            .id("identity-hub")
            .build();

    protected static final DataspaceIssuer ISSUER = DataspaceIssuer.Builder.newInstance().id("issuer").name("issuer")
            .did(IDENTITY_HUB_PARTICIPANT.didFor("issuer"))
            .build();

    protected static final RemoteParticipant REMOTE_PARTICIPANT = LegacyRemoteParticipant.Builder.newInstance()
            .name("remote")
            .id(IDENTITY_HUB_PARTICIPANT.didFor("remote"))
            .stsUri(IDENTITY_HUB_PARTICIPANT.getSts())
            .did(IDENTITY_HUB_PARTICIPANT.didFor("remote"))
            .bpn(IDENTITY_HUB_PARTICIPANT.bpnFor("remote"))
            .trustedIssuer(ISSUER.didUrl())
            .build();

    static final DcpParticipant LOCAL_PARTICIPANT = DcpParticipant.Builder.newInstance()
            .name("local")
            .id(IDENTITY_HUB_PARTICIPANT.bpnFor("local"))
            .stsUri(IDENTITY_HUB_PARTICIPANT.getSts())
            .did(IDENTITY_HUB_PARTICIPANT.didFor("local"))
            .bpn(IDENTITY_HUB_PARTICIPANT.bpnFor("local"))
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
                    .configurationProvider(LOCAL_PARTICIPANT::dcpConfig)
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
                    .registerServiceMock(AudienceResolver.class, message -> {
                        var audience = DIDS.get(message.getCounterPartyId());
                        return audience != null
                                ? Result.success(audience)
                                : Result.failure("No DID found for counter-party: " + message.getCounterPartyId());
                    }));

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

    static {
        addAudienceMapping(REMOTE_PARTICIPANT);
        addAudienceMapping(LOCAL_PARTICIPANT);
    }

    @BeforeAll
    static void beforeAll() {
        configureParticipant(LOCAL_PARTICIPANT, ISSUER, IDENTITY_HUB_PARTICIPANT, LOCAL_IDENTITY_HUB);
        configureParticipant(REMOTE_PARTICIPANT, ISSUER, IDENTITY_HUB_PARTICIPANT, LOCAL_IDENTITY_HUB);
        configureParticipantContext(ISSUER, IDENTITY_HUB_PARTICIPANT, LOCAL_IDENTITY_HUB);

        var vault = LOCAL_CONNECTOR.getService(Vault.class);
        vault.storeSecret(LOCAL_PARTICIPANT.getPrivateKeyAlias(), LOCAL_PARTICIPANT.getPrivateKeyAsString());
        vault.storeSecret(LOCAL_PARTICIPANT.getFullKeyId(), LOCAL_PARTICIPANT.getPublicKeyAsString());
        vault.storeSecret("client_secret_alias", "clientSecret");

        var remoteKey = "testing.edc.bdrs.remote-" + UUID.randomUUID().toString().substring(0, 8);
        System.setProperty(remoteKey + ".key", LOCAL_PARTICIPANT.getId());
        System.setProperty(remoteKey + ".value", LOCAL_PARTICIPANT.getDid());
    }

    private static void addAudienceMapping(TractusxDcpParticipantBase target) {
        // BPN to DID mapping
        var bpnKey = "testing.edc.bdrs." + UUID.randomUUID().toString().substring(0, 8);
        System.setProperty(bpnKey + ".key", target.getId());  // BPN
        System.setProperty(bpnKey + ".value", target.getDid());

        // DID to DID mapping (identity mapping for when counter-party is already a DID)
        var didKey = "testing.edc.bdrs." + UUID.randomUUID().toString().substring(0, 8);
        System.setProperty(didKey + ".key", target.getDid());  // DID
        System.setProperty(didKey + ".value", target.getDid());
    }

    @ParameterizedTest
    @ArgumentsSource(ParticipantsArgProvider.class)
    void httpPullTransfer(TractusxDcpParticipantBase consumer, TractusxDcpParticipantBase provider, String protocol) {
        consumer.setProtocol(protocol);
        provider.setProtocol(protocol);
        providerDataSource.stubFor(any(anyUrl()).willReturn(ok("data")));
        var assetId = UUID.randomUUID().toString();
        var now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        var usagePolicy = inForceDatePolicy("gteq", now.minusSeconds(60).toString(), "lteq", now.plusSeconds(20).toString());
        createResourcesOnProvider(provider, assetId, usagePolicy, httpSourceDataAddress());

        String transferProcessId;

        if (consumer instanceof LegacyRemoteParticipant legacyConsumer) {
            transferProcessId = executeLegacyTransfer(legacyConsumer, provider, assetId);
        } else {
            transferProcessId = consumer.requestAssetFrom(assetId, provider)
                    .withTransferType("HttpData-PULL")
                    .execute();
        }

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
    void suspendAndResume_httpPull_dataTransfer(TractusxDcpParticipantBase consumer, TractusxDcpParticipantBase provider, String protocol) {
        consumer.setProtocol(protocol);
        provider.setProtocol(protocol);
        providerDataSource.stubFor(any(anyUrl()).willReturn(ok("data")));
        var assetId = UUID.randomUUID().toString();
        createResourcesOnProvider(provider, assetId, noConstraintPolicy(), httpSourceDataAddress());

        String transferProcessId;
        if (consumer instanceof LegacyRemoteParticipant legacyConsumer) {
            transferProcessId = executeLegacyTransfer(legacyConsumer, provider, assetId);
        } else {
            transferProcessId = consumer.requestAssetFrom(assetId, provider)
                    .withTransferType("HttpData-PULL")
                    .execute();
        }

        consumer.awaitTransferToBeInState(transferProcessId, STARTED);

        var edr = await().atMost(consumer.getTimeout()).until(() -> consumer.edrs().getEdr(transferProcessId), Objects::nonNull);

        var msg = UUID.randomUUID().toString();
        var data = consumer.data().pullData(edr, Map.of("message", msg));
        assertThat(data).isNotNull().isEqualTo("data");

        consumer.suspendTransfer(transferProcessId, "suspension");

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

    protected void createResourcesOnProvider(TractusxDcpParticipantBase provider, String assetId, JsonObject contractPolicy, Map<String, Object> dataAddressProperties) {
        createAssetManagementContext(provider, assetId, Map.of("description", "description"), dataAddressProperties);
        var contractPolicyId = createPolicyDefinitionManagementContext(provider, contractPolicy);
        var noConstraintPolicyId = createPolicyDefinitionManagementContext(provider, noConstraintPolicy());

        createContractDefinitionManagementContext(provider, assetId, UUID.randomUUID().toString(), noConstraintPolicyId, contractPolicyId);
    }

    private String executeLegacyTransfer(LegacyRemoteParticipant legacyConsumer, TractusxDcpParticipantBase provider, String assetId) {
        var dataset = await().atMost(ASYNC_TIMEOUT)
                .pollInterval(ASYNC_POLL_INTERVAL)
                .ignoreExceptions()
                .until(() -> legacyConsumer.getDatasetForAssetWithDid(assetId, provider), Objects::nonNull);

        var catalogPolicy = dataset.getJsonArray("hasPolicy").get(0).asJsonObject();

        var counterPartyAddress = provider.getProtocolUrl();
        if (!counterPartyAddress.endsWith(DSP_2025_PATH_SUFFIX)) {
            counterPartyAddress = counterPartyAddress + DSP_2025_PATH_SUFFIX;
        }

        var providerId = legacyConsumer.getLastProviderParticipantId() != null
                ? legacyConsumer.getLastProviderParticipantId()
                : provider.getDid();

        var policy = createObjectBuilder(catalogPolicy)
                .add("target", assetId)
                .add("assigner", providerId)
                .build();

        var requestContext = createArrayBuilder()
                .add(ODRL_CONTEXT)
                .add("https://w3id.org/catenax/2025/9/policy/context.jsonld")
                .add(createObjectBuilder().add("@vocab", EDC_NAMESPACE))
                .build();

        var contractRequest = createObjectBuilder()
                .add("@context", requestContext)
                .add("@type", "ContractRequest")
                .add("counterPartyAddress", counterPartyAddress)
                .add("protocol", DSP_2025)
                .add("policy", policy)
                .build();

        var negotiationId = legacyConsumer.baseManagementRequest()
                .contentType(JSON)
                .body(contractRequest)
                .when()
                .post("/contractnegotiations")
                .then()
                .log().ifError()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("'@id'");

        await()
                .atMost(ASYNC_TIMEOUT)
                .pollInterval(ASYNC_POLL_INTERVAL)
                .until(() -> {
                    var state = legacyConsumer.getContractNegotiationState(negotiationId);
                    if ("TERMINATED".equals(state)) {
                        var errorDetail = legacyConsumer.baseManagementRequest()
                                .when()
                                .get("/contractnegotiations/{id}", negotiationId)
                                .then()
                                .statusCode(200)
                                .extract()
                                .jsonPath()
                                .getString("errorDetail");
                        throw new AssertionError("Contract negotiation " + negotiationId + " TERMINATED: " + errorDetail);
                    }
                    return "FINALIZED".equals(state);
                });

        var agreementId = legacyConsumer.baseManagementRequest()
                .when()
                .get("/contractnegotiations/{id}", negotiationId)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .get("contractAgreementId")
                .toString();

        var transferRequest = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder()
                        .add("@vocab", EDC_NAMESPACE)
                        .build())
                .add(TYPE, "TransferRequest")
                .add("counterPartyAddress", provider.getProtocolUrl())
                .add("contractId", agreementId)
                .add("assetId", assetId)
                .add("protocol", DSP_2025)
                .add("transferType", "HttpData-PULL")
                .build();

        return legacyConsumer.baseManagementRequest()
                .contentType(JSON)
                .body(transferRequest)
                .when()
                .post("/transferprocesses")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("'@id'");
    }

    private void createAssetManagementContext(TractusxDcpParticipantBase participant, String assetId, Map<String, Object> properties, Map<String, Object> dataAddressProperties) {
        var propertiesBuilder = createObjectBuilder();
        properties.forEach((key, value) -> propertiesBuilder.add(key, String.valueOf(value)));

        var dataAddressBuilder = createObjectBuilder().add(TYPE, "DataAddress");
        dataAddressProperties.forEach((key, value) -> dataAddressBuilder.add(key, String.valueOf(value)));

        var requestBody = createObjectBuilder()
                .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT))
                .add(ID, assetId)
                .add(TYPE, "Asset")
                .add(EDC_NAMESPACE + "properties", propertiesBuilder.build())
                .add(EDC_NAMESPACE + "dataAddress", dataAddressBuilder.build())
                .build();

        participant.baseManagementRequest()
                .basePath("/v3")
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post("/assets")
                .then()
                .log().ifValidationFails()
                .statusCode(200);
    }

    private String createPolicyDefinitionManagementContext(TractusxDcpParticipantBase participant, JsonObject policy) {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT))
                .add(ID, UUID.randomUUID().toString())
                .add(TYPE, "PolicyDefinition")
                .add(EDC_NAMESPACE + "policy", policy)
                .build();

        return participant.baseManagementRequest()
                .basePath("/v3")
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post("/policydefinitions")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().jsonPath().getString(ID);
    }

    private void createContractDefinitionManagementContext(TractusxDcpParticipantBase participant, String assetId, String definitionId, String accessPolicyId, String contractPolicyId) {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder()
                        .add("@vocab", EDC_NAMESPACE)
                        .build())
                .add(ID, definitionId)
                .add(TYPE, "ContractDefinition")
                .add("accessPolicyId", accessPolicyId)
                .add("contractPolicyId", contractPolicyId)
                .add("assetsSelector", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add(TYPE, "Criterion")
                                .add("operandLeft", "https://w3id.org/edc/v0.0.1/ns/id")
                                .add("operator", "=")
                                .add("operandRight", assetId)
                                .build())
                        .build())
                .build();

        participant.baseManagementRequest()
                .basePath("/v3")
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post("/contractdefinitions")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract().jsonPath().getString(ID);
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
                    Arguments.of(REMOTE_PARTICIPANT, LOCAL_PARTICIPANT, DSP_2025),
                    Arguments.of(LOCAL_PARTICIPANT, REMOTE_PARTICIPANT, DSP_2025)
            );
        }
    }
}
