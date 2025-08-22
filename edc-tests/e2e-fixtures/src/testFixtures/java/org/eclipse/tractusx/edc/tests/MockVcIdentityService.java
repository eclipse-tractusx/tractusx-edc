/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

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
    
    public MockVcIdentityService(String businessPartnerNumber, String did) {
        this.businessPartnerNumber = businessPartnerNumber;
        this.did = did;
    }
    
    @Override
    public Result<TokenRepresentation> obtainClientCredentials(TokenParameters parameters) {
        var credentials = List.of(membershipCredential(), dataExchangeGovernanceCredential());
        var token = Map.of(VC_CLAIM, credentials);

        var tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(typeManager.writeValueAsString(token))
                .build();
        return Result.success(tokenRepresentation);
    }
    
    @Override
    public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation, VerificationContext verificationContext) {
        var token = typeManager.readValue(tokenRepresentation.getToken(), Map.class);
        if (token.containsKey(VC_CLAIM)) {
            var credentials = typeManager.getMapper().convertValue(token.get(VC_CLAIM), new TypeReference<List<VerifiableCredential>>(){});
            var claimToken = ClaimToken.Builder.newInstance()
                    .claim(VC_CLAIM, credentials)
                    .build();
            return Result.success(claimToken);
        }
        return Result.failure(format("Expected %s claim, but token did not contain them", VC_CLAIM));
    }
    
    private VerifiableCredential membershipCredential() {
        return VerifiableCredential.Builder.newInstance()
                .type("VerifiableCredential")
                .type("MembershipCredential")
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id(did)
                        .claim("holderIdentifier", businessPartnerNumber)
                        .build())
                .issuer(new Issuer("issuer", Map.of()))
                .issuanceDate(Instant.now())
                .build();
    }

    private VerifiableCredential dataExchangeGovernanceCredential() {
        return VerifiableCredential.Builder.newInstance()
                .type("VerifiableCredential")
                .type("DataExchangeGovernanceCredential")
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id(did)
                        .claim("holderIdentifier", businessPartnerNumber)
                        .claim("contractVersion", "2.0")
                        .build())
                .issuer(new Issuer("issuer", Map.of()))
                .issuanceDate(Instant.now())
                .build();
    }
}
