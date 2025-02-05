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

package org.eclipse.tractusx.edc.dast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.security.token.jwt.CryptoConverter;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Extension("Dast Seed Extension")
public class DastSeedExtension implements ServiceExtension {

    public static final String MEMBERSHIP_CREDENTIAL = "MembershipCredential";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PARTICIPANT_ID = "participant";
    private static final String DID = "did:example:participant";
    private static final String KEY = "key1";
    private static final String METHOD_ID = DID + "#" + KEY;
    private static final String PRIVATE_METHOD_ID = "private." + DID + "#" + KEY;
    @Inject
    private ParticipantContextService participantContextService;
    @Inject
    private DidResolverRegistry resolverRegistry;
    @Inject
    private CredentialStore credentialStore;
    @Inject
    private Vault vault;

    private static JsonArray context() {
        return Json.createArrayBuilder()
                .add("https://www.w3.org/2018/credentials/v1")
                .add("https://w3id.org/security/suites/jws-2020/v1")
                .add("https://w3id.org/catenax/credentials")
                .add("https://w3id.org/vc/status-list/2021/v1")
                .build();
    }

    private static JsonArray types(String type) {
        return Json.createArrayBuilder()
                .add("VerifiableCredential")
                .add(type)
                .build();
    }

    @Override
    public void prepare() {
        var keyPair = generateKeyPair();
        var participantKey = getKeyPairAsJwk(keyPair, METHOD_ID);

        vault.storeSecret(METHOD_ID, participantKey.toPublicJWK().toJSONString());
        vault.storeSecret(PRIVATE_METHOD_ID, participantKey.toJSONString());
        vault.storeSecret("public_key_alias", participantKey.toPublicJWK().toJSONString());

        var exampleResolver = new DidExampleResolver();
        resolverRegistry.register(exampleResolver);
        var didDocument = boostrap(participantContextService, participantKey);
        exampleResolver.addCached(DID, didDocument);
    }

    public String createJwtVc(JsonObject verifiableCredential, String participantDid, JWK jwk) {

        try {
            var vc = MAPPER.readValue(verifiableCredential.toString(), new TypeReference<Map<String, Object>>() {
            });
            return signJwt(jwk.toECKey(), participantDid, participantDid, "", Map.of("vc", vc));

        } catch (JsonProcessingException e) {
            throw new EdcException(e);
        }

    }

    private JsonObject membershipSubject(String did, String id) {
        return Json.createObjectBuilder()
                .add("type", MEMBERSHIP_CREDENTIAL)
                .add("holderIdentifier", id)
                .add("id", did)
                .build();

    }

    private JsonObjectBuilder createVcBuilder(String issuer, String type, JsonObject subjectSupplier) {
        return Json.createObjectBuilder()
                .add("@context", context())
                .add("type", types(type))
                .add("credentialSubject", subjectSupplier)
                .add("issuer", issuer)
                .add("issuanceDate", Instant.now().toString());
    }

    private JWK getKeyPairAsJwk(KeyPair keyPair, String kid) {
        var jwk = CryptoConverter.createJwk(keyPair).toJSONObject();
        jwk.put("kid", kid);
        return CryptoConverter.create(jwk);
    }

    private DidDocument boostrap(ParticipantContextService participantContextService, JWK participantKey) {

        var key = KeyDescriptor.Builder.newInstance()
                .keyId(KEY)
                .publicKeyJwk(participantKey.toPublicJWK().toJSONObject())
                .privateKeyAlias(PRIVATE_METHOD_ID)
                .active(true)
                .build();

        var participantManifest = ParticipantManifest.Builder.newInstance()
                .participantId(DID)
                .active(true)
                .did(DID)
                .key(key)
                .build();

        participantContextService.createParticipantContext(participantManifest);

        credentialStore.create(generateMembershipCredential(participantKey));

        return generateDidDocument(participantKey);
    }

    private DidDocument generateDidDocument(JWK jwk) {

        var service = new Service();
        service.setId("#credential-service");
        service.setType("CredentialService");
        service.setServiceEndpoint("http://edc-runtime:8989/api/resolution/v1/participants/" + toBase64(DID));

        var verificationMethod = VerificationMethod.Builder.newInstance()
                .id(METHOD_ID)
                .controller(DID)
                .type("JsonWebKey2020")
                .publicKeyJwk(jwk.toPublicJWK().toJSONObject())
                .build();

        return DidDocument.Builder.newInstance()
                .id(DID)
                .service(List.of(service))
                .authentication(List.of("#" + KEY))
                .verificationMethod(List.of(verificationMethod))
                .build();


    }

    private String toBase64(String s) {
        return Base64.getUrlEncoder().encodeToString(s.getBytes());
    }

    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("EC", new BouncyCastleProvider());
            gen.initialize(new ECGenParameterSpec("secp256r1"));
            return gen.generateKeyPair();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new EdcException(e);
        }
    }

    private VerifiableCredentialResource generateMembershipCredential(JWK jwk) {
        var subject = CredentialSubject.Builder.newInstance()
                .claim("holderIdentifier", PARTICIPANT_ID)
                .build();
        var credential = VerifiableCredential.Builder.newInstance()
                .type(MEMBERSHIP_CREDENTIAL)
                .credentialSubject(subject)
                .issuer(new Issuer(DID, Map.of()))
                .issuanceDate(Instant.now())
                .build();

        var vcJson = createVc(DID, MEMBERSHIP_CREDENTIAL, membershipSubject(DID, PARTICIPANT_ID));
        var rawVc = createJwtVc(vcJson, DID, jwk);
        return VerifiableCredentialResource.Builder.newInstance()
                .issuerId(DID)
                .participantId(DID)
                .holderId(DID)
                .credential(new VerifiableCredentialContainer(rawVc, CredentialFormat.VC1_0_JWT, credential))
                .build();
    }

    private JsonObject createVc(String issuer, String type, JsonObject subjectSupplier) {
        return createVcBuilder(issuer, type, subjectSupplier)
                .build();
    }

    private String signJwt(ECKey privateKey, String issuerId, String subject, String audience, Map<String, Object> claims) {
        try {
            var signer = new ECDSASigner(privateKey.toECPrivateKey());
            var now = Date.from(Instant.now());

            var claimsSet = new JWTClaimsSet.Builder()
                    .issuer(issuerId)
                    .subject(subject)
                    .issueTime(now)
                    .audience(audience)
                    .notBeforeTime(now)
                    .claim("jti", UUID.randomUUID().toString())
                    .expirationTime(Date.from(Instant.now().plusSeconds(300L)));

            Objects.requireNonNull(claimsSet);
            claims.forEach(claimsSet::claim);
            var signedJwt = new SignedJWT((new JWSHeader.Builder(JWSAlgorithm.ES256)).keyID(privateKey.getKeyID()).build(), claimsSet.build());
            signedJwt.sign(signer);
            return signedJwt.serialize();
        } catch (JOSEException e) {
            throw new EdcException(e);
        }
    }
}
