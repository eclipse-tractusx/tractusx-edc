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
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.security.signature.jws2020.JsonWebKeyPair;
import org.eclipse.edc.security.signature.jws2020.Jws2020ProofDraft;
import org.eclipse.edc.security.signature.jws2020.Jws2020SignatureSuite;
import org.eclipse.edc.verifiablecredentials.linkeddata.LdpIssuer;
import org.eclipse.tractusx.edc.tests.IdentityParticipant;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import static org.eclipse.edc.jsonld.util.JacksonJsonLd.createObjectMapper;
import static org.eclipse.tractusx.edc.tests.transfer.iatp.harness.IatpHelperFunctions.createVc;
import static org.eclipse.tractusx.edc.tests.transfer.iatp.harness.IatpHelperFunctions.frameworkAgreementSubject;
import static org.eclipse.tractusx.edc.tests.transfer.iatp.harness.IatpHelperFunctions.membershipSubject;
import static org.mockito.Mockito.mock;

/**
 * Dataspace issuer configurations
 */
public class DataspaceIssuer extends IdentityParticipant {

    public static final String DATASPACE_ISSUER = "did:example:dataspace_issuer";
    public static final URI ASSERTION_METHOD = URI.create("https://w3id.org/security#assertionMethod");
    private static final ObjectMapper MAPPER = createObjectMapper();
    private static final String KEY_ID = "#key1";
    private final Jws2020SignatureSuite jws2020suite = new Jws2020SignatureSuite(MAPPER);
    private final DidDocument didDocument;

    public DataspaceIssuer() {
        didDocument = generateDidDocument();
    }

    public String didUrl() {
        return DATASPACE_ISSUER;
    }

    public DidDocument didDocument() {
        return didDocument;
    }

    public String verificationId() {
        return DATASPACE_ISSUER + "#" + getKeyId();
    }

    public VerifiableCredentialResource issueCredential(String did, String bpn, JsonLd jsonLd, String type, Supplier<CredentialSubject> credentialSubjectSupplier, JsonObject subjectSupplier) {
        var credential = VerifiableCredential.Builder.newInstance()
                .type(type)
                .credentialSubject(credentialSubjectSupplier.get())
                .issuer(new Issuer(didUrl(), Map.of()))
                .issuanceDate(Instant.now())
                .build();

        var vcJson = createVc(didUrl(), type, subjectSupplier);
        var rawVc = createJwtVc(vcJson, did);
        return VerifiableCredentialResource.Builder.newInstance()
                .issuerId(didUrl())
                .participantContextId(did)
                .holderId(bpn)
                .credential(new VerifiableCredentialContainer(rawVc, CredentialFormat.VC1_0_JWT, credential))
                .build();

    }

    public VerifiableCredentialResource issueMembershipCredential(String did, String bpn, JsonLd jsonLd) {
        return issueCredential(did, bpn, jsonLd, "MembershipCredential", () -> CredentialSubject.Builder.newInstance()
                        .claim("holderIdentifier", bpn)
                        .build(),
                membershipSubject(did, bpn));
    }

    public VerifiableCredentialResource issueDismantlerCredential(String did, String bpn, JsonLd jsonLd) {
        return issueCredential(did, bpn, jsonLd, "DismantlerCredential", () -> CredentialSubject.Builder.newInstance()
                        .id(did)
                        .claim("holderIdentifier", bpn)
                        .claim("activityType", "vehicleDismantle")
                        .claim("allowedVehicleBrands", List.of("Moskvich", "Lada"))
                        .build(),
                Json.createObjectBuilder()
                        .add("type", "DismantlerCredential")
                        .add("holderIdentifier", bpn)
                        .add("activityType", "vehicleDismantle")
                        .add("allowedVehicleBrands", Json.createArrayBuilder().add("Moskvich").add("Lada").build())
                        .add("id", did)
                        .build());
    }

    public VerifiableCredentialResource issueFrameworkCredential(String did, String bpn, JsonLd jsonLd, String credentialType) {
        return issueCredential(did, bpn, jsonLd, credentialType, () -> CredentialSubject.Builder.newInstance()
                        .claim("holderIdentifier", bpn)
                        .build(),
                frameworkAgreementSubject(did, bpn, credentialType));

    }

    @Override
    public String getFullKeyId() {
        return verificationId();
    }

    public String createLdpVc(JsonLd jsonLd, JsonObject verifiableCredential) {
        var issuer = LdpIssuer.Builder.newInstance()
                .jsonLd(jsonLd)
                .monitor(mock())
                .build();

        var proofDraft = Jws2020ProofDraft.Builder.newInstance()
                .proofPurpose(ASSERTION_METHOD)
                .verificationMethod(new JsonWebKeyPair(URI.create(verificationId()), null, null, null))
                .created(Instant.now())
                .mapper(MAPPER)
                .build();

        var key = getKeyPairAsJwk();

        var result = issuer.signDocument(jws2020suite, verifiableCredential, createKeyPair(key, verificationId()), proofDraft).orElseThrow(err -> new RuntimeException(err.getFailureDetail()));

        try {
            return MAPPER.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String createJwtVc(JsonObject verifiableCredential, String participantDid) {

        try {
            var vc = MAPPER.readValue(verifiableCredential.toString(), new TypeReference<Map<String, Object>>() {
            });
            var key = getKeyPairAsJwk();
            return signJwt(key.toECKey(), didUrl(), participantDid, "", Map.of("vc", vc));

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    private String signJwt(ECKey privateKey, String issuerId, String subject, String audience, Map<String, Object> claims) {
        try {
            var signer = new ECDSASigner(privateKey.toECPrivateKey());
            var now = java.sql.Date.from(Instant.now());
            var claimsSet = new JWTClaimsSet.Builder()
                    .issuer(issuerId)
                    .subject(subject)
                    .issueTime(now)
                    .audience(audience)
                    .notBeforeTime(now)
                    .claim("jti", UUID.randomUUID().toString())
                    .expirationTime(java.sql.Date.from(Instant.now().plusSeconds(300L)));

            Objects.requireNonNull(claimsSet);
            claims.forEach(claimsSet::claim);
            var signedJwt = new SignedJWT((new JWSHeader.Builder(JWSAlgorithm.ES256)).keyID(privateKey.getKeyID()).build(), claimsSet.build());
            signedJwt.sign(signer);
            return signedJwt.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    private com.apicatalog.ld.signature.key.KeyPair createKeyPair(JWK jwk, String id) {
        var type = URI.create("https://w3id.org/security#JsonWebKey2020");
        return new JsonWebKeyPair(URI.create(id), type, null, jwk);
    }

    private DidDocument generateDidDocument() {


        var jwk = getKeyPairAsJwk();
        var verificationMethod = VerificationMethod.Builder.newInstance()
                .id(verificationId())
                .controller(didUrl())
                .type("JsonWebKey2020")
                .publicKeyJwk(jwk.toPublicJWK().toJSONObject())
                .build();

        return DidDocument.Builder.newInstance()
                .id(didUrl())
                .authentication(List.of(KEY_ID))
                .verificationMethod(List.of(verificationMethod))
                .build();

    }
}
