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
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.identitytrust.spi.model.PresentationResponseMessage;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.VerifiablePresentationService;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.tractusx.edc.tests.transfer.iatp.harness.IatpParticipant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.bnpPolicy;
import static org.eclipse.tractusx.edc.tests.transfer.iatp.harness.IatpHelperFunctions.configureParticipant;
import static org.eclipse.tractusx.edc.tests.transfer.iatp.runtime.Runtimes.iatpRuntime;
import static org.eclipse.tractusx.edc.tests.transfer.iatp.runtime.Runtimes.stsRuntime;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@EndToEndTest
public class CredentialSpoofTest implements IatpParticipants {

    private static final IatpParticipant CONSUMER = IatpParticipants.participant(CONSUMER_NAME, CONSUMER_BPN);
    private static final IatpParticipant PROVIDER = IatpParticipants.participant(PROVIDER_NAME, PROVIDER_BPN);
    private static final IatpParticipant MALICIOUS_ACTOR = IatpParticipants.participant("MALICIOUS", "MALICIOUS-BPN");

    @RegisterExtension
    protected static final RuntimeExtension MALICIOUS_ACTOR_RUNTIME = iatpRuntime(MALICIOUS_ACTOR.getName(), MALICIOUS_ACTOR.getKeyPair(), () -> MALICIOUS_ACTOR.iatpConfig(PROVIDER, CONSUMER));
    @RegisterExtension
    protected static final RuntimeExtension CONSUMER_RUNTIME = iatpRuntime(CONSUMER.getName(), CONSUMER.getKeyPair(), () -> CONSUMER.iatpConfig(PROVIDER, MALICIOUS_ACTOR));
    @RegisterExtension
    protected static final RuntimeExtension PROVIDER_RUNTIME = iatpRuntime(PROVIDER.getName(), PROVIDER.getKeyPair(), () -> PROVIDER.iatpConfig(CONSUMER, MALICIOUS_ACTOR));
    @RegisterExtension
    protected static final RuntimeExtension STS_RUNTIME = stsRuntime(STS.getName(), STS.getKeyPair(), () -> STS.stsConfig(CONSUMER, PROVIDER, MALICIOUS_ACTOR));

    private static final Integer MOCKED_CS_SERVICE_PORT = getFreePort();
    protected ClientAndServer server;

    private static DidDocument maliciousActorDidDocument(DidDocument didDocument) {
        var service = new Service();
        service.setId("#credential-service");
        service.setType("CredentialService");
        service.setServiceEndpoint("http://%s:%d".formatted("localhost", MOCKED_CS_SERVICE_PORT));
        return DidDocument.Builder.newInstance()
                .id(didDocument.getId())
                .verificationMethod(didDocument.getVerificationMethod())
                .service(List.of(service))
                .build();
    }

    @BeforeEach
    void setup() {
        server = ClientAndServer.startClientAndServer("localhost", getFreePort(), MOCKED_CS_SERVICE_PORT);

        // create the DIDs cache
        var dids = new HashMap<String, DidDocument>();
        dids.put(DATASPACE_ISSUER_PARTICIPANT.didUrl(), DATASPACE_ISSUER_PARTICIPANT.didDocument());
        dids.put(CONSUMER.getDid(), CONSUMER.getDidDocument());
        dids.put(PROVIDER.getDid(), PROVIDER.getDidDocument());
        dids.put(MALICIOUS_ACTOR.getDid(), maliciousActorDidDocument(MALICIOUS_ACTOR.getDidDocument()));

        configureParticipant(DATASPACE_ISSUER_PARTICIPANT, CONSUMER, CONSUMER_RUNTIME, dids, STS_RUNTIME);
        configureParticipant(DATASPACE_ISSUER_PARTICIPANT, PROVIDER, PROVIDER_RUNTIME, dids, STS_RUNTIME);
        configureParticipant(DATASPACE_ISSUER_PARTICIPANT, MALICIOUS_ACTOR, MALICIOUS_ACTOR_RUNTIME, dids, STS_RUNTIME);
    }

    @AfterEach
    void shutdown() {
        server.stop();
    }

    @Test
    @DisplayName("Malicious actor should not impersonate a consumer by creating a VP with the consumer membership credential")
    void shouldNotImpersonateConsumer_withWrappedConsumerCredential() {
        var assetId = "api-asset-1";

        Map<String, Object> dataAddress = Map.of(
                "baseUrl", "http://mock",
                "type", "HttpData",
                "contentType", "application/json"
        );

        var presentationService = MALICIOUS_ACTOR_RUNTIME.getService(VerifiablePresentationService.class);

        withMock((membershipCredential) -> presentationService.createPresentation(MALICIOUS_ACTOR.getDid(), List.of(membershipCredential.getVerifiableCredential()), null, PROVIDER.getDid()));

        PROVIDER.createAsset(assetId, Map.of(), dataAddress);

        var policy = createAccessPolicy(CONSUMER.getBpn());
        var accessPolicyId = PROVIDER.createPolicyDefinition(policy);
        var contractPolicyId = PROVIDER.createPolicyDefinition(policy);
        PROVIDER.createContractDefinition(assetId, "def-1", accessPolicyId, contractPolicyId);

        MALICIOUS_ACTOR.getCatalog(PROVIDER)
                .log().ifError()
                .statusCode(502);
    }

    @Test
    @DisplayName("Malicious actor should not impersonate a consumer sending a valid consumer VP")
    void shouldNotImpersonateConsumer_withConsumerPresentation() {
        var assetId = "api-asset-1";


        Map<String, Object> dataAddress = Map.of(
                "baseUrl", "http://mock",
                "type", "HttpData",
                "contentType", "application/json"
        );

        var presentationService = CONSUMER_RUNTIME.getService(VerifiablePresentationService.class);

        withMock((membershipCredential) -> presentationService.createPresentation(CONSUMER.getDid(), List.of(membershipCredential.getVerifiableCredential()), null, PROVIDER.getDid()));

        PROVIDER.createAsset(assetId, Map.of(), dataAddress);

        var policy = createAccessPolicy(CONSUMER.getBpn());
        var accessPolicyId = PROVIDER.createPolicyDefinition(policy);
        var contractPolicyId = PROVIDER.createPolicyDefinition(policy);
        PROVIDER.createContractDefinition(assetId, "def-1", accessPolicyId, contractPolicyId);

        MALICIOUS_ACTOR.getCatalog(PROVIDER)
                .log().ifError()
                .statusCode(502);

    }

    void withMock(Function<VerifiableCredentialResource, Result<PresentationResponseMessage>> response) {

        var store = CONSUMER_RUNTIME.getService(CredentialStore.class);

        var sokratesMembershipCredential = store.query(QuerySpec.max()).getContent()
                .stream().filter(c -> c.getVerifiableCredential().credential().getType().contains("MembershipCredential"))
                .findFirst()
                .orElseThrow();

        var transformerRegistry = MALICIOUS_ACTOR_RUNTIME.getService(TypeTransformerRegistry.class);
        var jsonLd = MALICIOUS_ACTOR_RUNTIME.getService(JsonLd.class);


        server.when(request().withMethod("POST").withPath("/presentations/query")).respond((request -> {
            var json = response.apply(sokratesMembershipCredential)
                    .compose(presentation -> transformerRegistry.transform(presentation, JsonObject.class))
                    .compose(jsonLd::compact)
                    .orElseThrow(failure -> new EdcException(failure.getFailureDetail()));

            return response().withStatusCode(200).withBody(json.toString());
        }));
    }

    protected JsonObject createAccessPolicy(String bpn) {
        return bnpPolicy(bpn);
    }

}
