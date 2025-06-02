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

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.security.Vault;

import java.time.Instant;
import java.util.List;

public class IatpHelperFunctions {

    public static JsonObject createVc(String issuer, String type, JsonObject subjectSupplier) {
        return createVcBuilder(issuer, type, subjectSupplier)
                .build();
    }

    public static JsonObjectBuilder createVcBuilder(String issuer, String type, JsonObject subjectSupplier) {
        return Json.createObjectBuilder()
                .add("@context", context())
                .add("type", types(type))
                .add("credentialSubject", subjectSupplier)
                .add("issuer", issuer)
                .add("issuanceDate", Instant.now().toString());
    }

    public static JsonObject membershipSubject(String did, String id) {
        return Json.createObjectBuilder()
                .add("type", "MembershipCredential")
                .add("holderIdentifier", id)
                .add("id", did)
                .build();

    }

    public static JsonObject frameworkAgreementSubject(String did, String id, String type) {
        return Json.createObjectBuilder()
                .add("type", type)
                .add("holderIdentifier", id)
                .add("contractVersion", "1.0.0")
                .add("contractTemplate", "https://public.catena-x.org/contracts/traceabilty.v1.pdf")
                .add("id", did)
                .build();

    }

    private static JsonArray types(String type) {
        return Json.createArrayBuilder()
                .add("VerifiableCredential")
                .add(type)
                .build();
    }

    private static JsonArray context() {
        return Json.createArrayBuilder()
                .add("https://www.w3.org/2018/credentials/v1")
                .add("https://w3id.org/security/suites/jws-2020/v1")
                .add("https://w3id.org/catenax/credentials")
                .add("https://w3id.org/vc/status-list/2021/v1")
                .build();
    }

    public static void configureParticipant(DataspaceIssuer issuer, IatpParticipant participant, RuntimeExtension runtimeExtension, RuntimeExtension stsRuntimeExtension) {
        var participantContextService = runtimeExtension.getService(ParticipantContextService.class);
        var vault = runtimeExtension.getService(Vault.class);

        var participantKey = participant.getKeyPairAsJwk();
        var key = KeyDescriptor.Builder.newInstance()
                .keyId(participant.getKeyId())
                .publicKeyJwk(participantKey.toPublicJWK().toJSONObject())
                .privateKeyAlias(participant.getPrivateKeyAlias())
                .build();

        var participantManifest = ParticipantManifest.Builder.newInstance()
                .participantId(participant.getDid())
                .did(participant.getDid())
                .key(key)
                .build();

        participantContextService.createParticipantContext(participantManifest);
        vault.storeSecret(participant.getPrivateKeyAlias(), participant.getPrivateKeyAsString());

        storeCredentials(issuer, participant, runtimeExtension);

        if (stsRuntimeExtension != null) {
            stsRuntimeExtension.getService(Vault.class).storeSecret(participant.verificationId(), participant.getPrivateKeyAsString());
            stsRuntimeExtension.getService(Vault.class).storeSecret(participant.getPrivateKeyAlias(), participant.getPrivateKeyAsString());
            var account = StsAccount.Builder.newInstance()
                    .id(participant.getId())
                    .name(participant.getName())
                    .clientId(participant.getDid())
                    .did(participant.getDid())
                    .privateKeyAlias(key.getPrivateKeyAlias())
                    .publicKeyReference(participant.getFullKeyId())
                    .secretAlias("client_secret_alias")
                    .build();
            stsRuntimeExtension.getService(StsAccountStore.class).create(account);
        }
    }

    private static void storeCredentials(DataspaceIssuer issuer, IatpParticipant participant, RuntimeExtension runtime) {
        var credentialStore = runtime.getService(CredentialStore.class);
        issueCredentials(issuer, participant).forEach(credentialStore::create);
    }

    private static List<VerifiableCredentialResource> issueCredentials(DataspaceIssuer issuer, IatpParticipant participant) {
        return List.of(
                issuer.issueMembershipCredential(participant.getDid(), participant.getBpn()),
                issuer.issueDismantlerCredential(participant.getDid(), participant.getBpn()),
                issuer.issueFrameworkCredential(participant.getDid(), participant.getBpn(), "PcfCredential"),
                issuer.issueFrameworkCredential(participant.getDid(), participant.getBpn(), "DataExchangeGovernanceCredential"));

    }
}
