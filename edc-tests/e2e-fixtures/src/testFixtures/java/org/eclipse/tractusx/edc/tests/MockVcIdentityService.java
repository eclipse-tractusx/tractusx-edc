/********************************************************************************
 * Copyright (c) 2026 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.tests;

import org.eclipse.edc.iam.decentralizedclaims.spi.validation.TokenValidationAction;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.iam.VerificationContext;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.eclipse.edc.iam.decentralizedclaims.spi.SelfIssuedTokenConstants.PRESENTATION_TOKEN_CLAIM;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;

/**
 * An {@link IdentityService} that will inject the BPN claim in every token.
 * Please only use in testing scenarios!
 */
public class MockVcIdentityService implements IdentityService {
    
    private static final String BUSINESS_PARTNER_NUMBER_CLAIM = "BusinessPartnerNumber";
    private static final String VC_CLAIM = "vc";
    private final String businessPartnerNumber;
    private final String did;
    private final TypeManager typeManager = new JacksonTypeManager();
    private final TokenValidationAction tokenValidationAction;

    public MockVcIdentityService(String businessPartnerNumber, String did, TokenValidationAction tokenValidationAction) {
        this.businessPartnerNumber = businessPartnerNumber;
        this.did = did;
        this.tokenValidationAction = tokenValidationAction;
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(String participantContextId, TokenParameters parameters) {
        var tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(getTestToken(parameters.getStringClaim("aud")))
                .build();

        return Result.success(tokenRepresentation);
    }

    @Override
    public Result<ClaimToken> verifyJwtToken(String participantContextId, TokenRepresentation tokenRepresentation, VerificationContext verificationContext) {
        var token = tokenRepresentation.getToken().replace("Bearer ", "");
        tokenRepresentation = tokenRepresentation.toBuilder().token(token).build();
        var claimTokenResult = tokenValidationAction.validate(participantContextId, tokenRepresentation);

        if (claimTokenResult.failed()) {
            return claimTokenResult.mapEmpty();
        }

        var claimToken = claimTokenResult.getContent();
        var bpnlConsumer = claimToken.getStringClaim(BUSINESS_PARTNER_NUMBER_CLAIM);
        var didConsumer = claimToken.getStringClaim(ISSUER);
        var credentials = List.of(membershipCredential(bpnlConsumer, didConsumer), dataExchangeGovernanceCredential(bpnlConsumer, didConsumer));

        var claimTokenWithVc = ClaimToken.Builder.newInstance()
                .claim(VC_CLAIM, credentials)
                .build();

        return Result.success(claimTokenWithVc);
    }

    private String getTestToken(String aud) {
        var header = Map.of(
                "alg", "ES256K",
                "typ", "JWT",
                "kid", did + "#key-1"
        );

        var now = Instant.now();
        var payload = new java.util.HashMap<String, Object>();
        payload.put(ISSUER, did);
        payload.put(SUBJECT, did);
        payload.put(AUDIENCE, aud);
        payload.put(EXPIRATION_TIME, now.plusSeconds(3600).getEpochSecond());
        payload.put(ISSUED_AT, now.getEpochSecond());
        payload.put(PRESENTATION_TOKEN_CLAIM, "token");
        payload.put(BUSINESS_PARTNER_NUMBER_CLAIM, businessPartnerNumber);

        var signature = "signature";

        String headerJson = typeManager.writeValueAsString(header);
        String payloadJson = typeManager.writeValueAsString(payload);
        String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes());
        String encodedToken = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes());
        String encodedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signature.getBytes());

        return (encodedHeader + "." + encodedToken + "." + encodedSignature);
    }

    private VerifiableCredential dataExchangeGovernanceCredential(String bpnlConsumer, String didConsumer) {
        return VerifiableCredential.Builder.newInstance()
                .type("VerifiableCredential")
                .type("DataExchangeGovernanceCredential")
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id(didConsumer)
                        .claim("holderIdentifier", bpnlConsumer)
                        .claim("contractVersion", "1.0")
                        .build())
                .issuer(new Issuer("issuer", Map.of()))
                .issuanceDate(Instant.now())
                .build();
    }

    private VerifiableCredential membershipCredential(String bpnlConsumer, String didConsumer) {
        return VerifiableCredential.Builder.newInstance()
                .type("VerifiableCredential")
                .type("MembershipCredential")
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id(didConsumer)
                        .claim("holderIdentifier", bpnlConsumer)
                        .build())
                .issuer(new Issuer("issuer", Map.of()))
                .issuanceDate(Instant.now())
                .build();
    }
}
