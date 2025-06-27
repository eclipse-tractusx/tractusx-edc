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

package org.eclipse.tractusx.edc.tests.transfer.iatp.harness;

import org.eclipse.edc.connector.controlplane.test.system.utils.LazySupplier;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.tractusx.edc.tests.participant.TractusxIatpParticipantBase;

import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

import static org.eclipse.edc.util.io.Ports.getFreePort;

public class IatpParticipant extends TractusxIatpParticipantBase {

    protected final LazySupplier<URI> csService = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/api/resolution"));
    protected LazySupplier<URI> dimUri;

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
        var participantContextService = runtimeExtension.getService(ParticipantContextService.class);
        var vault = runtimeExtension.getService(Vault.class);

        var participantKey = getKeyPairAsJwk();
        var key = KeyDescriptor.Builder.newInstance()
                .keyId(getKeyId())
                .publicKeyJwk(participantKey.toPublicJWK().toJSONObject())
                .privateKeyAlias(getPrivateKeyAlias())
                .build();

        var participantManifest = ParticipantManifest.Builder.newInstance()
                .participantId(getDid())
                .did(getDid())
                .key(key)
                .build();

        participantContextService.createParticipantContext(participantManifest);
        vault.storeSecret(getPrivateKeyAlias(), getPrivateKeyAsString());

        var credentialStore = runtimeExtension.getService(CredentialStore.class);
        issueCredentials(issuer).forEach(credentialStore::create);
    }

    public void configureParticipant(DataspaceIssuer issuer, RuntimeExtension runtimeExtension, RuntimeExtension stsRuntimeExtension) {
        configureParticipant(issuer, runtimeExtension);

        stsRuntimeExtension.getService(Vault.class).storeSecret(verificationId(), getPrivateKeyAsString());
        stsRuntimeExtension.getService(Vault.class).storeSecret(getPrivateKeyAlias(), getPrivateKeyAsString());
        var account = StsAccount.Builder.newInstance()
                .id(getId())
                .name(getName())
                .clientId(getDid())
                .did(getDid())
                .privateKeyAlias(getPrivateKeyAlias())
                .publicKeyReference(getFullKeyId())
                .secretAlias("client_secret_alias")
                .build();
        stsRuntimeExtension.getService(StsAccountStore.class).create(account);
    }

    private List<VerifiableCredentialResource> issueCredentials(DataspaceIssuer issuer) {
        return List.of(
                issuer.issueMembershipCredential(getDid(), getBpn()),
                issuer.issueDismantlerCredential(getDid(), getBpn()),
                issuer.issueFrameworkCredential(getDid(), getBpn(), "PcfCredential"),
                issuer.issueFrameworkCredential(getDid(), getBpn(), "BpnCredential"),
                issuer.issueFrameworkCredential(getDid(), getBpn(), "DataExchangeGovernanceCredential")
        );
    }

    public static class Builder extends TractusxIatpParticipantBase.Builder<IatpParticipant, Builder> {

        protected Builder() {
            super(new IatpParticipant());
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

        private DidDocument generateDidDocument() {
            var service = new Service();
            service.setId("#credential-service");
            service.setType("CredentialService");
            service.setServiceEndpoint(participant.csService.get() + "/v1/participants/" + toBase64(participant.did));

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
                    .authentication(List.of("#key1"))
                    .verificationMethod(List.of(verificationMethod))
                    .build();
        }

        private String toBase64(String s) {
            return Base64.getUrlEncoder().encodeToString(s.getBytes());
        }

    }
}
