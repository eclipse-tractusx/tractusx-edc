/********************************************************************************
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

package org.eclipse.edc.verifiablecredentials.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.decentralizedclaims.service.DcpIdentityService;
import org.eclipse.edc.iam.decentralizedclaims.service.verification.MultiFormatPresentationVerifier;
import org.eclipse.edc.iam.decentralizedclaims.spi.PresentationRequestService;
import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.CredentialVerifier;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.SignatureSuiteRegistry;
import org.eclipse.edc.iam.decentralizedclaims.transform.to.JwtToVerifiableCredentialTransformer;
import org.eclipse.edc.iam.decentralizedclaims.transform.to.JwtToVerifiablePresentationTransformer;
import org.eclipse.edc.iam.verifiablecredentials.VerifiableCredentialValidationServiceImpl;
import org.eclipse.edc.iam.verifiablecredentials.revocation.RevocationServiceRegistryImpl;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.DataModelVersion;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentation;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.iam.VerificationContext;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.TokenValidationRulesRegistryImpl;
import org.eclipse.edc.token.TokenValidationServiceImpl;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.verifiablecredentials.jwt.rules.HasSubjectRule;
import org.eclipse.edc.verifiablecredentials.linkeddata.LdpVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;
import static org.eclipse.edc.verifiablecredentials.jwt.JwtPresentationVerifier.JWT_VC_TOKEN_CONTEXT;
import static org.mockito.Mockito.mock;

/**
 * Verifies {@link JwtPresentationVerifier} through the complete DCP verification path, wired exactly as
 * {@code DcpCoreExtension} / {@code DcpTransformExtension} do, driving the real {@link DcpIdentityService#verifyJwtToken}.
 * Only the SI-token validation and the HTTP fetch of the VP are stubbed.
 *
 * <p>Presentations that declare a single, consistent format and carry credentials signed by a trusted issuer must be
 * accepted; presentations with inconsistent format markers, or credentials not signed by their claimed issuer, must be
 * rejected.
 */
class JwtPresentationVerifierTest {

    private static final String PARTICIPANT_CONTEXT = "participant";
    private static final String VERIFIER_DID = "did:web:provider.example";
    private static final String PRESENTER_DID = "did:web:presenter.example";
    private static final String TRUSTED_ISSUER_DID = "did:web:dim.example:catena-x-issuer";
    private static final String PRESENTER_KID = PRESENTER_DID + "#key-1";
    private static final String ISSUER_KID = TRUSTED_ISSUER_DID + "#key-1";
    private static final String OTHER_BPN = "BPNL0000000OTHER";
    private static final String MEMBERSHIP_SCOPE = "org.eclipse.tractusx.vc.type:MembershipCredential:read";
    private static final String VCDM_11_CONTEXT = "https://www.w3.org/2018/credentials/v1";
    private static final String VCDM_20_CONTEXT = "https://www.w3.org/ns/credentials/v2";

    private final Monitor monitor = new ConsoleMonitor();
    private final TypeManager typeManager = new JacksonTypeManager();
    private final Map<String, PublicKey> resolvableKeys = new ConcurrentHashMap<>();
    private final PublicKeyResolver didPublicKeyResolver = id -> resolvableKeys.containsKey(id)
            ? Result.success(resolvableKeys.get(id))
            : Result.failure("Key not found: " + id);

    private ECKey presenterKey;
    private ECKey trustedIssuerKey;
    private ECKey strangerKey;

    private TypeTransformerRegistryImpl transformerRegistry;
    private VerifiableCredentialValidationServiceImpl credentialValidationService;

    @BeforeEach
    void setUp() throws JOSEException {
        presenterKey = new ECKeyGenerator(Curve.P_256).keyID(PRESENTER_KID).generate();
        trustedIssuerKey = new ECKeyGenerator(Curve.P_256).keyID(ISSUER_KID).generate();
        strangerKey = new ECKeyGenerator(Curve.P_256).keyID(PRESENTER_DID + "#not-in-did-document").generate();
        resolvableKeys.put(PRESENTER_KID, presenterKey.toECPublicKey());
        resolvableKeys.put(ISSUER_KID, trustedIssuerKey.toECPublicKey());

        var jsonLd = mock(JsonLd.class);
        transformerRegistry = new TypeTransformerRegistryImpl();
        transformerRegistry.register(new JwtToVerifiablePresentationTransformer(monitor, typeManager, JSON_LD, jsonLd));
        transformerRegistry.register(new JwtToVerifiableCredentialTransformer(monitor));

        var rulesRegistry = new TokenValidationRulesRegistryImpl();
        rulesRegistry.addRule(JWT_VC_TOKEN_CONTEXT, new HasSubjectRule());

        CredentialVerifier jwtVerifier = new JwtPresentationVerifier(typeManager, JSON_LD, new TokenValidationServiceImpl(), rulesRegistry, didPublicKeyResolver);
        var ldpVerifier = LdpVerifier.Builder.newInstance()
                .signatureSuites(mock(SignatureSuiteRegistry.class))
                .jsonLd(jsonLd)
                .typeManager(typeManager)
                .typeContext(JSON_LD)
                .build();
        var presentationVerifier = new MultiFormatPresentationVerifier(jwtVerifier, ldpVerifier);

        var trustedIssuerRegistry = new InMemoryTrustedIssuerRegistry();
        trustedIssuerRegistry.register(new Issuer(TRUSTED_ISSUER_DID, Map.of()), TrustedIssuerRegistry.WILDCARD);

        credentialValidationService = new VerifiableCredentialValidationServiceImpl(presentationVerifier, trustedIssuerRegistry,
                new RevocationServiceRegistryImpl(monitor), Clock.systemUTC(), typeManager.getMapper());
    }

    @Test
    void vcdm11Presentation_issuerSigned_isAccepted() throws Exception {
        var issued = membershipVcJwt(trustedIssuerKey, ISSUER_KID, PRESENTER_DID, "BPNL00000PRESENT");
        assertThat(verifyAsProvider(vcdm11Vp(presenterKey, List.of(issued))).succeeded()).isTrue();
    }

    @Test
    void vcdm11Presentation_credentialNotSignedByIssuer_isRejected() throws Exception {
        var selfSigned = membershipVcJwt(presenterKey, ISSUER_KID, PRESENTER_DID, OTHER_BPN);
        assertThat(verifyAsProvider(vcdm11Vp(presenterKey, List.of(selfSigned))).failed()).isTrue();
    }

    @Test
    void vpClaimWithVcdm20Context_isRejected() throws Exception {
        var selfSigned = membershipVcJwt(presenterKey, ISSUER_KID, PRESENTER_DID, OTHER_BPN);
        var vp = envelopedVpWithVpClaim(presenterKey, PRESENTER_KID, VERIFIER_DID, selfSigned);
        assertThat(verifyAsProvider(vp).failed()).isTrue();
    }

    @Test
    void vpClaimWithVcdm20Context_invalidOuterSignatureAndAudience_isRejected() throws Exception {
        var selfSigned = membershipVcJwt(presenterKey, ISSUER_KID, PRESENTER_DID, OTHER_BPN);
        var vp = envelopedVpWithVpClaim(strangerKey, strangerKey.getKeyID(), "did:web:somebody-else.example", selfSigned);
        assertThat(verifyAsProvider(vp).failed()).isTrue();
    }

    @Test
    void vcAndVpClaims_isRejected() throws Exception {
        var selfSigned = membershipVcJwt(presenterKey, ISSUER_KID, PRESENTER_DID, OTHER_BPN);
        var claims = baseVpClaims(VERIFIER_DID)
                .claim("vc", Map.of("type", List.of("VerifiableCredential")))
                .claim("vp", Map.of(
                        "@context", List.of(VCDM_11_CONTEXT),
                        "type", List.of("VerifiablePresentation"),
                        "verifiableCredential", List.of(selfSigned)))
                .build();
        assertThat(verifyAsProvider(sign(presenterKey, PRESENTER_KID, claims)).failed()).isTrue();
    }

    @Test
    void nestedCredentialWithVpClaimAndVcdm20Context_isRejected() throws Exception {
        var now = Instant.now();
        var nestedClaims = new JWTClaimsSet.Builder()
                .issuer(TRUSTED_ISSUER_DID)
                .subject(PRESENTER_DID)
                .jwtID(UUID.randomUUID().toString())
                .claim("vp", Map.of("type", List.of("VerifiablePresentation"), "verifiableCredential", List.of()))
                .claim("@context", List.of(VCDM_20_CONTEXT))
                .claim("type", List.of("VerifiableCredential", "MembershipCredential"))
                .claim("issuer", TRUSTED_ISSUER_DID)
                .claim("validFrom", now.minus(1, ChronoUnit.DAYS).toString())
                .claim("validUntil", now.plus(365, ChronoUnit.DAYS).toString())
                .claim("credentialSubject", Map.of("id", PRESENTER_DID, "holderIdentifier", OTHER_BPN))
                .build();
        var nested = sign(presenterKey, ISSUER_KID, nestedClaims);
        assertThat(verifyAsProvider(vcdm11Vp(presenterKey, List.of(nested))).failed()).isTrue();
    }

    // ---------------------------------------------------------------------------------------------------------------

    private Result<ClaimToken> verifyAsProvider(String rawVp) {
        var siClaims = ClaimToken.Builder.newInstance().claim("iss", PRESENTER_DID).claim("token", "access-token").build();
        PresentationRequestService presentationRequestService = (pc, ownDid, counterPartyDid, token, scopes) ->
                parseJwtVp(rawVp).map(List::of);

        var identityService = new DcpIdentityService(mock(SecureTokenService.class), pc -> VERIFIER_DID,
                (pc, tokenRepresentation) -> Result.success(siClaims),
                presentationRequestService,
                credentials -> Result.success(ClaimToken.Builder.newInstance().claims(Map.of("vc", credentials)).build()),
                credentialValidationService);

        var context = VerificationContext.Builder.newInstance()
                .policy(Policy.Builder.newInstance().build())
                .scopes(Set.of(MEMBERSHIP_SCOPE))
                .build();
        return identityService.verifyJwtToken(PARTICIPANT_CONTEXT, TokenRepresentation.Builder.newInstance().token("Bearer si-token").build(), context);
    }

    /** Mirror of EDC's DefaultCredentialServiceClient#parseJwtVp (0.15.1). */
    private Result<VerifiablePresentationContainer> parseJwtVp(String rawJwt) {
        return transformerRegistry.transform(rawJwt, VerifiablePresentation.class)
                .map(pres -> {
                    var format = pres.getDataModelVersion() == DataModelVersion.V_2_0 ? CredentialFormat.VC2_0_JOSE : CredentialFormat.VC1_0_JWT;
                    return new VerifiablePresentationContainer(rawJwt, format, pres);
                });
    }

    private String envelopedVpWithVpClaim(ECKey outerKey, String outerKid, String audience, String vcJwt) throws JOSEException {
        var innerClaims = new JWTClaimsSet.Builder()
                .issuer(PRESENTER_DID)
                .jwtID(UUID.randomUUID().toString())
                .claim("@context", List.of(VCDM_20_CONTEXT))
                .claim("type", List.of("VerifiablePresentation"))
                .claim("verifiableCredential", List.of(envelopedVc(vcJwt)))
                .build();
        var innerVp = sign(presenterKey, PRESENTER_KID, innerClaims);

        var outerClaims = baseVpClaims(audience)
                .claim("vp", Map.of("type", List.of("VerifiablePresentation"), "verifiableCredential", List.of()))
                .claim("@context", List.of(VCDM_20_CONTEXT))
                .claim("type", "EnvelopedVerifiablePresentation")
                .claim("id", "data:application/vp+jwt," + innerVp)
                .build();
        return sign(outerKey, outerKid, outerClaims);
    }

    private String vcdm11Vp(ECKey key, List<String> credentials) throws JOSEException {
        var claims = baseVpClaims(VERIFIER_DID)
                .claim("vp", Map.of(
                        "@context", List.of(VCDM_11_CONTEXT),
                        "type", List.of("VerifiablePresentation"),
                        "verifiableCredential", credentials))
                .build();
        return sign(key, PRESENTER_KID, claims);
    }

    private JWTClaimsSet.Builder baseVpClaims(String audience) {
        var now = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer(PRESENTER_DID)
                .subject(PRESENTER_DID)
                .audience(audience)
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plus(5, ChronoUnit.MINUTES)));
    }

    private String membershipVcJwt(ECKey signingKey, String kid, String subjectDid, String bpn) throws JOSEException {
        var now = Instant.now();
        var subject = new HashMap<String, Object>();
        subject.put("id", subjectDid);
        subject.put("holderIdentifier", bpn);
        var vc = new HashMap<String, Object>();
        vc.put("@context", List.of(VCDM_11_CONTEXT, "https://w3id.org/catenax/credentials/v1.0.0"));
        vc.put("type", List.of("VerifiableCredential", "MembershipCredential"));
        vc.put("issuer", TRUSTED_ISSUER_DID);
        vc.put("issuanceDate", now.minus(1, ChronoUnit.DAYS).toString());
        vc.put("expirationDate", now.plus(365, ChronoUnit.DAYS).toString());
        vc.put("credentialSubject", subject);

        var claims = new JWTClaimsSet.Builder()
                .issuer(TRUSTED_ISSUER_DID)
                .subject(subjectDid)
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .claim("vc", vc)
                .build();
        return sign(signingKey, kid, claims);
    }

    private Map<String, Object> envelopedVc(String vcJwt) {
        return Map.of("type", "EnvelopedVerifiableCredential", "id", "data:application/vc+jwt," + vcJwt);
    }

    private String sign(ECKey key, String kid, JWTClaimsSet claims) throws JOSEException {
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(kid).build(), claims);
        jwt.sign(new ECDSASigner(key));
        return jwt.serialize();
    }

    /** Same semantics as EDC's DefaultTrustedIssuerRegistry (0.15.1). */
    private static class InMemoryTrustedIssuerRegistry implements TrustedIssuerRegistry {
        private final Map<String, Set<String>> store = new ConcurrentHashMap<>();

        @Override
        public void register(Issuer issuer, String credentialType) {
            store.computeIfAbsent(issuer.id(), k -> ConcurrentHashMap.newKeySet()).add(credentialType);
        }

        @Override
        public Set<String> getSupportedTypes(Issuer issuer) {
            return store.getOrDefault(issuer.id(), Set.of());
        }
    }
}
