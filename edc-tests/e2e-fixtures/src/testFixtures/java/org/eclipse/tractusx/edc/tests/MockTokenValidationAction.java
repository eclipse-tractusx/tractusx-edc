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

import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.decentralizedclaims.spi.validation.TokenValidationAction;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.rules.AudienceValidationRule;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.rules.NotBeforeValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.verifiablecredentials.jwt.rules.HasSubjectRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerEqualsSubjectRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerKeyIdValidationRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.JtiValidationRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.SubJwkIsNullRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.TokenNotNullRule;

import java.time.Clock;
import java.util.ArrayList;

public class MockTokenValidationAction implements TokenValidationAction {

    private final TokenValidationService tokenValidationService;
    private final PublicKeyResolver publicKeyResolver;
    private final JtiValidationStore jtiValidationStore;
    private final Clock clock = Clock.systemUTC();

    public MockTokenValidationAction(TokenValidationService tokenValidationService, PublicKeyResolver publicKeyResolver, JtiValidationStore jtiValidationStore) {
        this.tokenValidationService = tokenValidationService;
        this.publicKeyResolver = publicKeyResolver;
        this.jtiValidationStore = jtiValidationStore;
    }

    public Result<ClaimToken> validate(String participantContextId, TokenRepresentation tokenRepresentation) {
        try {
            var signedJwt = SignedJWT.parse(tokenRepresentation.getToken());
            var keyId = signedJwt.getHeader().getKeyID();
            var rules = new ArrayList<TokenValidationRule>();

            rules.add(new IssuerEqualsSubjectRule());
            rules.add(new SubJwkIsNullRule());
            rules.add(new ExpirationIssuedAtValidationRule(clock, 5, false));
            rules.add(new TokenNotNullRule());
            rules.add(new NotBeforeValidationRule(clock, 4, true));
            rules.add(new HasSubjectRule());
            rules.add(new JtiValidationRule(jtiValidationStore, null));
            rules.add(new IssuerKeyIdValidationRule(keyId));
            rules.add(new AudienceValidationRule(audResolver(tokenRepresentation)));

            return tokenValidationService.validate(tokenRepresentation, publicKeyResolver, rules);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String audResolver(TokenRepresentation tokenRepresentation) {
        try {
            var jwt = SignedJWT.parse(tokenRepresentation.getToken());
            return jwt.getJWTClaimsSet().getAudience().get(0);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract audience from token", e);
        }
    }
}
