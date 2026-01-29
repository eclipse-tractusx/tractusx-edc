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

package org.eclipse.tractusx.edc.tests.participant;

import org.eclipse.edc.iam.decentralizedclaims.sts.spi.model.StsAccount;
import org.eclipse.edc.iam.decentralizedclaims.sts.spi.store.StsAccountStore;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.tractusx.edc.tests.runtimes.KeyPool;

import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static org.eclipse.edc.util.io.Ports.getFreePort;

public class IatpParticipant extends TractusxIatpParticipantBase {

    protected final LazySupplier<URI> csService = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/api/resolution"));
    protected LazySupplier<URI> dimUri;
    protected LazySupplier<URI> credentialServiceUri;

    private DidDocument didDocument;

    public DidDocument getDidDocument() {
        return didDocument;
    }

    public String verificationId() {
        return did + "#" + getKeyId();
    }

    @Override
    public Config getConfig() {
        var settings = new HashMap<String, String>();
        settings.put("web.http.credentials.port", String.valueOf(csService.get().getPort()));
        settings.put("web.http.credentials.path", csService.get().getPath());
        if (dimUri != null) {
            settings.put("tx.edc.iam.sts.dim.url", dimUri.get().toString());
        }
        return super.getConfig().merge(ConfigFactory.fromMap(settings));
    }

    public void configureParticipant(DataspaceIssuer issuer, RuntimeExtension runtimeExtension) {
        runtimeExtension.getService(Vault.class).storeSecret(getDid(), getPrivateKeyAlias(), getPrivateKeyAsString());

        try {
            // runtime has CredentialStore, DIM tests cases
            var credentialStore = runtimeExtension.getService(CredentialStore.class);
            issueCredentials(issuer).forEach(credentialStore::create);
        } catch (EdcException e) {
            // runtime has no CredentialStore, STS tests cases
        }
    }

    public void configureParticipant(DataspaceIssuer issuer, RuntimeExtension runtimeExtension, RuntimeExtension stsRuntimeExtension) {
        configureParticipant(issuer, runtimeExtension);

        var credentialStore = stsRuntimeExtension.getService(CredentialStore.class);
        issueCredentials(issuer).forEach(credentialStore::create);

        stsRuntimeExtension.getService(Vault.class).storeSecret(verificationId(), getPrivateKeyAsString());
        stsRuntimeExtension.getService(Vault.class).storeSecret(getPrivateKeyAlias(), getPrivateKeyAsString());

        var participantManifest = ParticipantManifest.Builder.newInstance()
                .participantContextId(getDid())
                .did(getDid())
                .build();
        var participantContextService = stsRuntimeExtension.getService(ParticipantContextService.class);
        var createParticipantContextResponse = participantContextService.createParticipantContext(participantManifest)
                .orElseThrow(f -> new EdcException("cannot create participant context: " + f.getFailureDetail()));

        runtimeExtension.getService(Vault.class).storeSecret("client_secret_alias", createParticipantContextResponse.clientSecret());

        stsRuntimeExtension.getService(KeyPairService.class).addKeyPair(getDid(), createKeyDescriptor(), true)
                .orElseThrow(f -> new EdcException("Cannot store key pair: " + f.getFailureDetail()));

        KeyPool.register(getFullKeyId(), getKeyPair());

        var account = StsAccount.Builder.newInstance()
                .id(getId())
                .participantContextId(getDid())
                .name(getName())
                .clientId(getDid())
                .did(getDid())
                .secretAlias("client_secret_alias")
                .build();
        stsRuntimeExtension.getService(StsAccountStore.class).create(account);
    }

    private List<VerifiableCredentialResource> issueCredentials(DataspaceIssuer issuer) {
        return List.of(
                issuer.issueMembershipCredential(getDid(), getBpn()),
                issuer.issueDismantlerCredential(getDid(), getBpn()),
                issuer.issueFrameworkCredential(getDid(), getBpn(), "BpnCredential"),
                issuer.issueFrameworkCredential(getDid(), getBpn(), "DataExchangeGovernanceCredential")
        );
    }

    public KeyDescriptor createKeyDescriptor() {
        return KeyDescriptor.Builder.newInstance()
                .keyId(getFullKeyId())
                .privateKeyAlias(getPrivateKeyAlias())
                .publicKeyJwk(getKeyPairAsJwk().toPublicJWK().toJSONObject())
                .build();
    }

    public static class Builder extends TractusxIatpParticipantBase.Builder<IatpParticipant, Builder> {

        protected Builder() {
            this(new IatpParticipant());
        }

        protected Builder(IatpParticipant participant) {
            super(participant);
        }

        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public IatpParticipant build() {
            super.build();
            participant.didDocument = generateDidDocument();
            return participant;
        }

        public Builder dimUri(LazySupplier<URI> dimUri) {
            participant.dimUri = dimUri;
            return self();
        }

        public Builder credentialServiceUri(LazySupplier<URI> credentialServiceUri) {
            participant.credentialServiceUri = credentialServiceUri;
            return self();
        }

        private DidDocument generateDidDocument() {
            var service = new Service();
            service.setId("#credential-service");
            service.setType("CredentialService");
            var credentialServiceBaseUri = Objects.requireNonNullElse(participant.credentialServiceUri, participant.csService);
            service.setServiceEndpoint(credentialServiceBaseUri.get() + "/v1/participants/" + toBase64(participant.did));

            var ecKey = participant.getKeyPairAsJwk();

            var verificationMethod = VerificationMethod.Builder.newInstance()
                    .id(participant.verificationId())
                    .controller(participant.did)
                    .type("JsonWebKey2020")
                    .publicKeyJwk(ecKey.toPublicJWK().toJSONObject())
                    .build();

            return DidDocument.Builder.newInstance()
                    .id(participant.did)
                    .service(List.of(service))
                    .authentication(List.of("#key-1"))
                    .verificationMethod(List.of(verificationMethod))
                    .build();
        }

        private String toBase64(String s) {
            return Base64.getUrlEncoder().encodeToString(s.getBytes());
        }
    }
}
