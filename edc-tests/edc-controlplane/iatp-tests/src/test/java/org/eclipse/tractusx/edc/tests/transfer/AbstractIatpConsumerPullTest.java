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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase;
import org.eclipse.tractusx.edc.tests.transfer.iatp.harness.DataspaceIssuer;
import org.eclipse.tractusx.edc.tests.transfer.iatp.harness.IatpParticipant;
import org.eclipse.tractusx.edc.tests.transfer.iatp.harness.StatusList2021;
import org.eclipse.tractusx.edc.tests.transfer.iatp.harness.StsParticipant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockserver.verify.VerificationTimes;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.frameworkPolicy;
import static org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase.ASYNC_TIMEOUT;
import static org.eclipse.tractusx.edc.tests.transfer.iatp.harness.IatpHelperFunctions.createVcBuilder;
import static org.eclipse.tractusx.edc.tests.transfer.iatp.harness.IatpHelperFunctions.membershipSubject;
import static org.hamcrest.Matchers.not;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public abstract class AbstractIatpConsumerPullTest extends HttpConsumerPullBaseTest {

    protected static final URI DIM_URI = URI.create("http://localhost:" + getFreePort());
    protected static final DataspaceIssuer DATASPACE_ISSUER_PARTICIPANT = new DataspaceIssuer();
    protected static final StsParticipant STS = StsParticipant.Builder.newInstance()
            .id("STS")
            .name("STS")
            .build();
    protected static final IatpParticipant CONSUMER = IatpParticipant.Builder.newInstance()
            .name(CONSUMER_NAME)
            .id(CONSUMER_BPN)
            .stsUri(STS.stsUri())
            .stsClientId(CONSUMER_BPN)
            .stsClientSecret("client_secret")
            .trustedIssuer(DATASPACE_ISSUER_PARTICIPANT.didUrl())
            .dimUri(DIM_URI)
            .did(did(CONSUMER_NAME))
            .build();
    protected static final IatpParticipant PROVIDER = IatpParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_BPN)
            .stsUri(STS.stsUri())
            .stsClientId(PROVIDER_BPN)
            .stsClientSecret("client_secret")
            .trustedIssuer(DATASPACE_ISSUER_PARTICIPANT.didUrl())
            .dimUri(DIM_URI)
            .did(did(PROVIDER_NAME))
            .build();

    private static String did(String name) {
        return "did:example:" + name.toLowerCase();
    }


    @Override
    public TractusxParticipantBase provider() {
        return PROVIDER;
    }

    @Override
    public TractusxParticipantBase consumer() {
        return CONSUMER;
    }

    @DisplayName("Contract policy is fulfilled")
    @ParameterizedTest(name = "{1}")
    @ArgumentsSource(ValidContractPolicyProvider.class)
    void transferData_whenContractPolicyFulfilled(JsonObject contractPolicy, String description) {
        var assetId = "api-asset-1";

        var authCodeHeaderName = "test-authkey";
        var authCode = "test-authcode";

        Map<String, Object> dataAddress = Map.of(
                "baseUrl", privateBackendUrl,
                "type", "HttpData",
                "contentType", "application/json",
                "authKey", authCodeHeaderName,
                "authCode", authCode
        );

        PROVIDER.createAsset(assetId, Map.of(), dataAddress);

        var accessPolicyId = PROVIDER.createPolicyDefinition(createAccessPolicy(CONSUMER.getBpn()));
        var contractPolicyId = PROVIDER.createPolicyDefinition(contractPolicy);
        PROVIDER.createContractDefinition(assetId, "def-1", accessPolicyId, contractPolicyId);
        var transferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER).withTransferType("HttpData-PULL")
                .withDestination(httpDataDestination()).execute();

        var edr = new AtomicReference<JsonObject>();

        // wait until transfer process completes
        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var tpState = CONSUMER.getTransferProcessState(transferProcessId);
                    assertThat(tpState).isNotNull().isEqualTo(TransferProcessStates.STARTED.toString());
                });

        // wait until EDC is available on the consumer side
        server.when(request().withMethod("GET").withPath(MOCK_BACKEND_PATH)).respond(response().withStatusCode(200).withBody("test response"));
        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    edr.set(CONSUMER.edrs().getEdr(transferProcessId));
                    assertThat(edr).isNotNull();
                });

        // pull data out of provider's backend service:
        // Prov-DP -> Prov-backend
        assertThat(consumer().data().pullData(edr.get(), Map.of())).isEqualTo("test response");

        server.verify(request()
                .withPath(MOCK_BACKEND_PATH)
                .withHeader("Edc-Contract-Agreement-Id")
                .withHeader("Edc-Bpn", consumer().getBpn())
                .withMethod("GET"), VerificationTimes.exactly(1));
    }

    @DisplayName("Contract policy is NOT fulfilled")
    @ParameterizedTest(name = "{1}")
    @ArgumentsSource(InvalidContractPolicyProvider.class)
    void transferData_whenContractPolicyNotFulfilled(JsonObject contractPolicy, String description) {
        var assetId = "api-asset-1";

        var authCodeHeaderName = "test-authkey";
        var authCode = "test-authcode";

        Map<String, Object> dataAddress = Map.of(
                "baseUrl", privateBackendUrl,
                "type", "HttpData",
                "contentType", "application/json",
                "authKey", authCodeHeaderName,
                "authCode", authCode
        );

        PROVIDER.createAsset(assetId, Map.of(), dataAddress);

        var accessPolicyId = PROVIDER.createPolicyDefinition(createAccessPolicy(CONSUMER.getBpn()));
        var contractPolicyId = PROVIDER.createPolicyDefinition(contractPolicy);
        PROVIDER.createContractDefinition(assetId, "def-1", accessPolicyId, contractPolicyId);
        var negotiationId = CONSUMER.initContractNegotiation(PROVIDER, assetId);

        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var contractNegotiationState = CONSUMER.getContractNegotiationState(negotiationId);
                    assertThat(contractNegotiationState).isEqualTo("TERMINATED");
                });
    }

    @DisplayName("Expect the Catalog request to fail if a credential is expired")
    @Test
    void catalogRequest_whenCredentialExpired() {
        //update the membership credential to an expirationDate that is in the past
        var store = consumerRuntime().getService(CredentialStore.class);

        var existingCred = store.query(QuerySpec.Builder.newInstance().filter(new Criterion("verifiableCredential.credential.type", "contains", "MembershipCredential")).build())
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()))
                .stream().findFirst()
                .orElseThrow(RuntimeException::new);

        var expirationDate = Instant.now().minus(1, ChronoUnit.DAYS);
        var newCred = VerifiableCredential.Builder.newInstance()
                .id(existingCred.getVerifiableCredential().credential().getId())
                .types(existingCred.getVerifiableCredential().credential().getType())
                .credentialSubjects(existingCred.getVerifiableCredential().credential().getCredentialSubject())
                .issuer(existingCred.getVerifiableCredential().credential().getIssuer())
                .issuanceDate(existingCred.getVerifiableCredential().credential().getIssuanceDate())
                .expirationDate(expirationDate)
                .build();

        var did = CONSUMER.getDid();
        var bpn = CONSUMER.getBpn();
        var newRawVc = createVcBuilder(DATASPACE_ISSUER_PARTICIPANT.didUrl(), "MembershipCredential", membershipSubject(did, bpn));
        newRawVc.add("expirationDate", expirationDate.toString());

        var newVcString = DATASPACE_ISSUER_PARTICIPANT.createJwtVc(newRawVc.build(), did);

        store.update(VerifiableCredentialResource.Builder.newInstance()
                        .id(existingCred.getId())
                        .issuerId(DATASPACE_ISSUER_PARTICIPANT.didUrl())
                        .participantId(did)
                        .holderId(bpn)
                        .credential(new VerifiableCredentialContainer(newVcString, CredentialFormat.VC1_0_JWT, newCred))
                        .build())
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));


        // verify the failed catalog request
        try {
            CONSUMER.getCatalog(PROVIDER)
                    .log().ifError()
                    .statusCode(not(200));
        } finally {
            // restore the non-expired cred
            store.update(existingCred);
        }
    }

    @DisplayName("Expect the Catalog request to fail if a credential is revoked")
    @Test
    void catalogRequest_whenCredentialRevoked() {
        //update the membership credential to contain a `credentialStatus` with a revocation
        var store = consumerRuntime().getService(CredentialStore.class);
        var jsonLd = consumerRuntime().getService(JsonLd.class);
        var port = getFreePort();

        var existingCred = store.query(QuerySpec.Builder.newInstance().filter(new Criterion("verifiableCredential.credential.type", "contains", "MembershipCredential")).build())
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()))
                .stream().findFirst()
                .orElseThrow(RuntimeException::new);

        var newCred = VerifiableCredential.Builder.newInstance()
                .id(existingCred.getVerifiableCredential().credential().getId())
                .types(existingCred.getVerifiableCredential().credential().getType())
                .credentialSubjects(existingCred.getVerifiableCredential().credential().getCredentialSubject())
                .credentialStatus(new CredentialStatus("http://localhost:%s/status/list/7#12345".formatted(port), "StatusList2021",
                        Map.of("statusPurpose", "revocation",
                                "statusListIndex", "12345",
                                "statusListCredential", "http://localhost:%d/status/list/7".formatted(port)
                        )
                ))
                .issuer(existingCred.getVerifiableCredential().credential().getIssuer())
                .issuanceDate(existingCred.getVerifiableCredential().credential().getIssuanceDate())
                .build();

        var did = CONSUMER.getDid();
        var bpn = CONSUMER.getBpn();
        var newRawVc = createVcBuilder(DATASPACE_ISSUER_PARTICIPANT.didUrl(), "MembershipCredential", membershipSubject(did, bpn));
        newRawVc.add("credentialStatus", Json.createObjectBuilder()
                .add("id", "http://localhost:%d/status/list/7#12345".formatted(port))
                .add("type", "StatusList2021Entry")
                .add("statusPurpose", "revocation")
                .add("statusListIndex", "12345")
                .add("statusListCredential", "http://localhost:%d/status/list/7".formatted(port))
                .build());

        var newVcString = DATASPACE_ISSUER_PARTICIPANT.createJwtVc(newRawVc.build(), did);

        store.update(VerifiableCredentialResource.Builder.newInstance()
                        .id(existingCred.getId())
                        .issuerId(DATASPACE_ISSUER_PARTICIPANT.didUrl())
                        .participantId(did)
                        .holderId(bpn)
                        .credential(new VerifiableCredentialContainer(newVcString, CredentialFormat.VC1_0_JWT, newCred))
                        .build())
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

        // return a StatusListCredential, where the credential's status is "revocation"
        try (var revocationServer = startClientAndServer(port)) {
            var slCred = StatusList2021.create(DATASPACE_ISSUER_PARTICIPANT.didUrl(), "revocation")
                    .withStatus(12345, true);
            revocationServer.when(request().withPath("/status/list/7")).respond(response().withBody(slCred.toJsonObject().toString()));

            // verify the failed catalog request
            CONSUMER.getCatalog(PROVIDER)
                    .log().ifValidationFails()
                    .statusCode(not(200));
        } finally {
            // restore the original credential without credentialStatus
            store.update(existingCred);
        }
    }

    @Override
    protected JsonObject createContractPolicy(String bpn) {
        return frameworkPolicy(Map.of(CX_POLICY_NS + "Membership", "active"));
    }

    protected abstract RuntimeExtension consumerRuntime();

    protected abstract RuntimeExtension providerRuntime();

    private JsonObject httpDataDestination() {
        return createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "properties", createObjectBuilder()
                        .add(EDC_NAMESPACE + "baseUrl", "http://localhost:8080")
                        .build())
                .build();
    }

    private static class ValidContractPolicyProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    Arguments.of(frameworkPolicy(Map.of(CX_POLICY_NS + "Membership", "active")), "MembershipCredential"),
                    Arguments.of(frameworkPolicy(Map.of(CX_POLICY_NS + "FrameworkAgreement.pcf", "active")), "PCF Use Case (legacy notation)"),
                    Arguments.of(frameworkPolicy(Map.of(CX_POLICY_NS + "FrameworkAgreement", "Pcf")), "PCF Use Case (new notation)"),
                    Arguments.of(frameworkPolicy(Map.of(CX_POLICY_NS + "FrameworkAgreement", "DataExchangeGovernance:1.0.0")), "DataExchangeGovernance use case"),
                    Arguments.of(frameworkPolicy(Map.of(CX_POLICY_NS + "Dismantler", "active")), "Dismantler Credential"),
                    Arguments.of(frameworkPolicy(Map.of(CX_POLICY_NS + "Dismantler.activityType", "vehicleDismantle")), "Dismantler Cred (activity type)"),
                    Arguments.of(frameworkPolicy(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.IS_ANY_OF, List.of("Moskvich", "Tatra")), "Dismantler allowedBrands (IS_ANY_OF, one intersects)"),
                    Arguments.of(frameworkPolicy(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.EQ, List.of("Moskvich", "Lada")), "Dismantler allowedBrands (EQ, exact match)"),
                    Arguments.of(frameworkPolicy(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.IS_NONE_OF, List.of("Yugo", "Tatra")), "Dismantler allowedBrands (IS_NONE_OF, no intersect)"),
                    Arguments.of(frameworkPolicy(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.IN, List.of("Moskvich", "Tatra", "Yugo", "Lada")), "Dismantler allowedBrands (IN, fully contained)")
            );
        }
    }

    private static class InvalidContractPolicyProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    Arguments.of(frameworkPolicy(Map.of(CX_POLICY_NS + "FrameworkAgreement.sustainability", "active")), "Sustainability Use Case (legacy notation)"),
                    Arguments.of(frameworkPolicy(Map.of(CX_POLICY_NS + "FrameworkAgreement", "traceability")), "Traceability Use Case (new notation)"),
                    Arguments.of(frameworkPolicy(Map.of(CX_POLICY_NS + "Dismantler.activityType", "vehicleScrap")), "Dismantler activityType does not match"),
                    Arguments.of(frameworkPolicy(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.NEQ, List.of("Moskvich", "Lada")), "Dismantler allowedBrands (NEQ, but is equal)"),
                    Arguments.of(frameworkPolicy(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.IS_NONE_OF, List.of("Yugo", "Lada")), "Dismantler allowedBrands (IS_NONE_OF, but is one contains)"),
                    Arguments.of(frameworkPolicy(CX_POLICY_NS + "Dismantler.allowedBrands", Operator.IN, List.of("Moskvich", "Tatra", "Yugo")), "Dismantler allowedBrands (IN, but not subset)")
            );
        }
    }
}
