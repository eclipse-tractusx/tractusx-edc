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

package org.eclipse.tractusx.edc.compatibility.tests.fixtures;

import org.eclipse.edc.iam.decentralizedclaims.sts.spi.service.StsAccountService;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.tractusx.edc.tests.participant.DataspaceIssuer;

import java.util.Base64;

public class DcpHelperFunctions {
    public static void configureParticipantContext(BaseParticipant participant, IdentityHubParticipant identityHubParticipant, RuntimeExtension identityHubRuntime) {
        var participantContextService = identityHubRuntime.getService(ParticipantContextService.class);

        var participantKey = participant.getKeyPairJwk();
        var key = KeyDescriptor.Builder.newInstance()
                .keyId(participant.getFullKeyId())
                .publicKeyJwk(participantKey.toPublicJWK().toJSONObject())
                .privateKeyAlias(participant.getPrivateKeyAlias())
                .build();

        var service = new Service();
        service.setId("#credential-service");
        service.setType("CredentialService");
        service.setServiceEndpoint(identityHubParticipant.getResolutionApi() + "/v1/participants/" + toBase64(participant.getDid()));

        var participantManifest = ParticipantManifest.Builder.newInstance()
                .participantContextId(participant.getDid())
                .did(participant.getDid())
                .key(key)
                .serviceEndpoint(service)
                .active(true)
                .build();

        participantContextService.createParticipantContext(participantManifest);

        var vault = identityHubRuntime.getService(Vault.class);
        vault.storeSecret(participant.getPrivateKeyAlias(), participant.getPrivateKeyAsString());
    }

    public static void configureParticipantContext(DataspaceIssuer issuer, IdentityHubParticipant identityHubParticipant, RuntimeExtension identityHubRuntime) {
        var participantContextService = identityHubRuntime.getService(ParticipantContextService.class);

        var participantKey = issuer.getKeyPairAsJwk();
        var key = KeyDescriptor.Builder.newInstance()
                .keyId(issuer.getFullKeyId())
                .publicKeyJwk(participantKey.toPublicJWK().toJSONObject())
                .privateKeyAlias(issuer.getPrivateKeyAlias())
                .build();

        var service = new Service();
        service.setId("#credential-service");
        service.setType("CredentialService");
        service.setServiceEndpoint(identityHubParticipant.getResolutionApi() + "/v1/participants/" + toBase64(issuer.didUrl()));

        var participantManifest = ParticipantManifest.Builder.newInstance()
                .participantContextId(issuer.didUrl())
                .did(issuer.didUrl())
                .key(key)
                .serviceEndpoint(service)
                .active(true)
                .build();

        participantContextService.createParticipantContext(participantManifest);

        var vault = identityHubRuntime.getService(Vault.class);
        vault.storeSecret(issuer.getPrivateKeyAlias(), issuer.getPrivateKeyAsString());
    }

    public static void configureParticipant(BaseParticipant participant, DataspaceIssuer issuer, IdentityHubParticipant identityHubParticipant, RuntimeExtension identityHubRuntime) {
        configureParticipantContext(participant, identityHubParticipant, identityHubRuntime);

        var accountService = identityHubRuntime.getService(StsAccountService.class);
        var vault = identityHubRuntime.getService(Vault.class);
        var credentialStore = identityHubRuntime.getService(CredentialStore.class);

        var credentials = issuer.issueCredentials(participant.getDid(), participant.getId());

        credentials.forEach(credentialStore::create);

        accountService.findById(participant.getDid())
                .onSuccess(account -> {
                    vault.storeSecret(account.getSecretAlias(), "clientSecret");
                });

    }

    static String toBase64(String s) {
        return Base64.getUrlEncoder().encodeToString(s.getBytes());
    }

}
