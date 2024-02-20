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

package org.eclipse.tractusx.edc.helpers;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.identityhub.spi.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.model.participant.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.tractusx.edc.did.DidExampleResolver;
import org.eclipse.tractusx.edc.lifecycle.ParticipantRuntime;
import org.eclipse.tractusx.edc.lifecycle.tx.iatp.DataspaceIssuer;
import org.eclipse.tractusx.edc.lifecycle.tx.iatp.IatpParticipant;
import org.testcontainers.shaded.org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.io.StringWriter;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class IatpHelperFunctions {

    /**
     * Returns the Pem representation of a {@link Key}
     *
     * @param key The input key
     * @return The pem encoded key
     */
    public static String toPemEncoded(Key key) {
        var writer = new StringWriter();
        try (var jcaPEMWriter = new JcaPEMWriter(writer)) {
            jcaPEMWriter.writeObject(key);
        } catch (IOException e) {
            throw new EdcException("Unable to convert private in PEM format ", e);
        }
        return writer.toString();
    }


    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("EC", new BouncyCastleProvider());
            gen.initialize(new ECGenParameterSpec("secp256r1"));
            return gen.generateKeyPair();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonObject createVc(String issuer, String type, Supplier<JsonObject> subjectSupplier) {
        return Json.createObjectBuilder()
                .add("@context", context())
                .add("type", types(type))
                .add("credentialSubject", subjectSupplier.get())
                .add("issuer", issuer)
                .add("issuanceDate", Instant.now().toString())
                .build();
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
                .build();
    }

    public static void configureParticipant(DataspaceIssuer issuer, IatpParticipant participant, ParticipantRuntime runtime, Map<String, DidDocument> dids, ParticipantRuntime stsRuntime) {

        if (stsRuntime != null) {
            stsRuntime.getContext().getService(Vault.class).storeSecret(participant.verificationId(), participant.privateKey());
        }
        var participantContextService = runtime.getContext().getService(ParticipantContextService.class);
        var vault = runtime.getContext().getService(Vault.class);
        var didResolverRegistry = runtime.getContext().getService(DidResolverRegistry.class);
        var didResolver = new DidExampleResolver();
        dids.forEach(didResolver::addCached);
        didResolverRegistry.register(didResolver);

        var key = KeyDescriptor.Builder.newInstance()
                .keyId(participant.keyId())
                .publicKeyPem(participant.publicKey())
                .privateKeyAlias(participant.keyId())
                .build();

        var participantManifest = ParticipantManifest.Builder.newInstance()
                .participantId(participant.didUrl())
                .did(participant.didUrl())
                .key(key)
                .build();

        participantContextService.createParticipantContext(participantManifest);
        vault.storeSecret(participant.keyId(), participant.privateKey());

        storeCredentials(issuer, participant, runtime);
    }

    private static void storeCredentials(DataspaceIssuer issuer, IatpParticipant participant, ParticipantRuntime runtime) {
        var credentialStore = runtime.getContext().getService(CredentialStore.class);
        var jsonLd = runtime.getContext().getService(JsonLd.class);
        issueCredentials(issuer, participant, jsonLd).forEach(credentialStore::create);
    }

    private static List<VerifiableCredentialResource> issueCredentials(DataspaceIssuer issuer, IatpParticipant participant, JsonLd jsonLd) {
        return List.of(
                issuer.issueMembershipCredential(participant, jsonLd),
                issuer.issueDismantlerCredential(participant, jsonLd),
                issuer.issueFrameworkCredential(participant, jsonLd, "PcfCredential"));

    }
}
