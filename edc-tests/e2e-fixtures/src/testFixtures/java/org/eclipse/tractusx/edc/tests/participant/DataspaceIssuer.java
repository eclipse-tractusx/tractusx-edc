/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2026 Cofinity-X GmbH
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import static org.eclipse.edc.jsonld.util.JacksonJsonLd.createObjectMapper;

/**
 * Dataspace issuer configurations
 */
public class DataspaceIssuer extends IdentityParticipant {

    private static final ObjectMapper MAPPER = createObjectMapper();
    private static final String KEY_ID = "#key-1";
    private DidDocument didDocument;
    private String did;

    public DataspaceIssuer() {
    }

    public DataspaceIssuer(String did) {
        this.did = did;
        didDocument = generateDidDocument();
    }

    public String didUrl() {
        return did;
    }

    public DidDocument didDocument() {
        return didDocument;
    }

    public String verificationId() {
        return did + "#" + getKeyId();
    }


    public VerifiableCredentialResource issueMembershipCredential(String did, String bpn) {
        return issueCredential(
                did, bpn, "MembershipCredential",
                () -> CredentialSubject.Builder.newInstance()
                        .id(did)
                        .claim("holderIdentifier", bpn)
                        .build(),
                membershipRawVc(did, bpn)
        );
    }

    public VerifiableCredentialResource issueDismantlerCredential(String did, String bpn) {
        return issueCredential(
                did, bpn, "DismantlerCredential",
                () -> CredentialSubject.Builder.newInstance()
                        .id(did)
                        .claim("holderIdentifier", bpn)
                        .claim("activityType", "vehicleDismantle")
                        .claim("allowedVehicleBrands", List.of("BMW", "Volkswagen"))
                        .build(),
                createVcBuilder("DismantlerCredential", Json.createObjectBuilder()
                        .add("type", "DismantlerCredential")
                        .add("holderIdentifier", bpn)
                        .add("activityType", "vehicleDismantle")
                        .add("allowedVehicleBrands", Json.createArrayBuilder().add("BMW").add("Volkswagen").build())
                        .add("id", did)
                        .build())
        );
    }

    public VerifiableCredentialResource issueFrameworkCredential(String did, String bpn, String credentialType) {
        var subject = Json.createObjectBuilder()
                .add("type", credentialType)
                .add("holderIdentifier", bpn)
                .add("contractVersion", "1.0")
                .add("contractTemplate", "https://public.catena-x.org/contracts/traceabilty.v1.pdf")
                .add("id", did)
                .build();

        return issueCredential(
                did, bpn, credentialType,
                () -> CredentialSubject.Builder.newInstance()
                        .id(did)
                        .claim("holderIdentifier", bpn)
                        .build(),
                createVcBuilder(credentialType, subject)
        );
    }

    @Override
    public String getFullKeyId() {
        return verificationId();
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

    public JsonObjectBuilder membershipRawVc(String did, String bpn) {
        var subject = Json.createObjectBuilder()
                .add("type", "MembershipCredential")
                .add("holderIdentifier", bpn)
                .add("id", did)
                .build();

        return createVcBuilder("MembershipCredential", subject);
    }

    public List<VerifiableCredentialResource> issueCredentials(String did, String bpn) {
        return List.of(
                issueMembershipCredential(did, bpn),
                issueDismantlerCredential(did, bpn),
                issueFrameworkCredential(did, bpn, "BpnCredential"),
                issueFrameworkCredential(did, bpn, "DataExchangeGovernanceCredential"));
    }

    private VerifiableCredentialResource issueCredential(String did, String bpn, String type, Supplier<CredentialSubject> credentialSubjectSupplier, JsonObjectBuilder vcBuilder) {
        var credential = VerifiableCredential.Builder.newInstance()
                .type(type)
                .credentialSubject(credentialSubjectSupplier.get())
                .issuer(new Issuer(didUrl(), Map.of()))
                .issuanceDate(Instant.now())
                .build();

        var vcJson = vcBuilder.build();
        var rawVc = createJwtVc(vcJson, did);
        return VerifiableCredentialResource.Builder.newInstance()
                .issuerId(didUrl())
                .participantContextId(did)
                .holderId(bpn)
                .credential(new VerifiableCredentialContainer(rawVc, CredentialFormat.VC1_0_JWT, credential))
                .build();
    }

    private JsonObjectBuilder createVcBuilder(String type, JsonObject subjectSupplier) {
        return Json.createObjectBuilder()
                .add("@context", Json.createArrayBuilder()
                        .add("https://www.w3.org/2018/credentials/v1")
                        .add("https://w3id.org/security/suites/jws-2020/v1")
                        .add("https://w3id.org/catenax/credentials")
                        .add("https://w3id.org/vc/status-list/2021/v1"))
                .add("type", Json.createArrayBuilder()
                        .add("VerifiableCredential")
                        .add(type))
                .add("credentialSubject", subjectSupplier)
                .add("issuer", didUrl())
                .add("issuanceDate", Instant.now().toString());
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

    public static class Builder extends IdentityParticipant.Builder<DataspaceIssuer, Builder> {

        protected Builder() {
            super(new DataspaceIssuer());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder did(String did) {
            participant.did = did;
            return self();
        }

        @Override
        public DataspaceIssuer build() {
            super.build();
            participant.didDocument = generateDidDocument();
            return participant;
        }

        private DidDocument generateDidDocument() {
            var jwk = participant.getKeyPairAsJwk();
            var verificationMethod = VerificationMethod.Builder.newInstance()
                    .id(participant.verificationId())
                    .controller(participant.didUrl())
                    .type("JsonWebKey2020")
                    .publicKeyJwk(jwk.toPublicJWK().toJSONObject())
                    .build();

            return DidDocument.Builder.newInstance()
                    .id(participant.didUrl())
                    .authentication(List.of(KEY_ID))
                    .verificationMethod(List.of(verificationMethod))
                    .build();

        }
    }
}
