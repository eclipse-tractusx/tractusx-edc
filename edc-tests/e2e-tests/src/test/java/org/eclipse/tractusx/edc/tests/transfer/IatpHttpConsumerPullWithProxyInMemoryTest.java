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
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.identityhub.spi.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.model.participant.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.tractusx.edc.did.DidExampleResolver;
import org.eclipse.tractusx.edc.lifecycle.ParticipantRuntime;
import org.eclipse.tractusx.edc.lifecycle.tx.iatp.DataspaceIssuer;
import org.eclipse.tractusx.edc.lifecycle.tx.iatp.IatpParticipant;
import org.eclipse.tractusx.edc.lifecycle.tx.iatp.SecureTokenService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.TX_CREDENTIAL_NAMESPACE;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.frameworkPolicy;

@EndToEndTest
// temporarily disabled waiting for an upstream fix
@Disabled
public class IatpHttpConsumerPullWithProxyInMemoryTest extends AbstractHttpConsumerPullWithProxyTest {

    protected static final DataspaceIssuer DATASPACE_ISSUER_PARTICIPANT = new DataspaceIssuer();
    protected static final SecureTokenService STS_PARTICIPANT = new SecureTokenService();

    protected static final IatpParticipant PLATO_IATP = new IatpParticipant(PLATO, STS_PARTICIPANT.stsUri());

    @RegisterExtension
    protected static final ParticipantRuntime PLATO_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:iatp:runtime-memory-iatp-ih",
            PLATO.getName(),
            PLATO.getBpn(),
            PLATO_IATP.iatpConfiguration(SOKRATES)
    );

    protected static final IatpParticipant SOKRATES_IATP = new IatpParticipant(SOKRATES, STS_PARTICIPANT.stsUri());

    @RegisterExtension
    protected static final ParticipantRuntime SOKRATES_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:iatp:runtime-memory-iatp-ih",
            SOKRATES.getName(),
            SOKRATES.getBpn(),
            SOKRATES_IATP.iatpConfiguration(PLATO)
    );

    @RegisterExtension
    protected static final ParticipantRuntime STS_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:iatp:runtime-memory-sts",
            STS_PARTICIPANT.getName(),
            STS_PARTICIPANT.getBpn(),
            STS_PARTICIPANT.stsConfiguration(SOKRATES_IATP, PLATO_IATP)
    );

    @BeforeAll
    static void prepare() {

        // create the DIDs cache
        var dids = new HashMap<String, DidDocument>();
        dids.put(DATASPACE_ISSUER_PARTICIPANT.didUrl(), DATASPACE_ISSUER_PARTICIPANT.didDocument());
        dids.put(SOKRATES_IATP.didUrl(), SOKRATES_IATP.didDocument());
        dids.put(PLATO_IATP.didUrl(), PLATO_IATP.didDocument());

        configureParticipant(SOKRATES_IATP, SOKRATES_RUNTIME, dids);
        configureParticipant(PLATO_IATP, PLATO_RUNTIME, dids);

    }

    private static void configureParticipant(IatpParticipant participant, ParticipantRuntime runtime, Map<String, DidDocument> dids) {
        STS_RUNTIME.getContext().getService(Vault.class).storeSecret(participant.verificationId(), participant.privateKey());
        var participantContextService = runtime.getContext().getService(ParticipantContextService.class);
        var vault = runtime.getContext().getService(Vault.class);
        var didResolverRegistry = runtime.getContext().getService(DidResolverRegistry.class);
        var didResolver = new DidExampleResolver();
        dids.forEach(didResolver::addCached);
        didResolverRegistry.register(didResolver);

        var key = KeyDescriptor.Builder.newInstance()
                .keyId(participant.verificationId())
                .publicKeyPem(participant.publicKey())
                .build();

        var participantManifest = ParticipantManifest.Builder.newInstance()
                .participantId(participant.getBpn())
                .did(participant.didUrl())
                .key(key)
                .build();

        participantContextService.createParticipantContext(participantManifest);
        vault.storeSecret(participant.verificationId(), participant.privateKey());

        storeCredentials(participant, runtime);
    }

    private static void storeCredentials(IatpParticipant participant, ParticipantRuntime runtime) {
        var credentialStore = runtime.getContext().getService(CredentialStore.class);
        var jsonLd = runtime.getContext().getService(JsonLd.class);
        issueCredentials(participant, jsonLd).forEach(credentialStore::create);
    }

    private static List<VerifiableCredentialResource> issueCredentials(IatpParticipant participant, JsonLd jsonLd) {

        if (participant.getBpn().startsWith("PLATO")) {
            return List.of(
                    DATASPACE_ISSUER_PARTICIPANT.issueMembershipCredential(participant, jsonLd));
        } else {
            return List.of(
                    DATASPACE_ISSUER_PARTICIPANT.issueMembershipCredential(participant, jsonLd),
                    DATASPACE_ISSUER_PARTICIPANT.issueFrameworkCredential(participant, jsonLd, "PcfCredential"));
        }

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
