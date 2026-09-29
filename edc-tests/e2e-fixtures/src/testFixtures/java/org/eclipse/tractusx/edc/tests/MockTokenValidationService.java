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
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationService;

import java.text.ParseException;
import java.util.List;

public class MockTokenValidationService implements TokenValidationService {

    @Override
    public Result<ClaimToken> validate(TokenRepresentation tokenRepresentation, PublicKeyResolver publicKeyResolver, List<TokenValidationRule> rules) {
        var token = tokenRepresentation.getToken();
        var additional = tokenRepresentation.getAdditional();
        try {
            var signedJwt = SignedJWT.parse(token);
            var tokenBuilder = ClaimToken.Builder.newInstance();

            signedJwt.getJWTClaimsSet().getClaims().entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .forEach(entry -> tokenBuilder.claim(entry.getKey(), entry.getValue()));

            var claimToken = tokenBuilder.build();
            var errors = rules.stream()
                    .map(r -> r.checkRule(claimToken, additional))
                    .reduce(Result::merge)
                    .stream()
                    .filter(AbstractResult::failed)
                    .flatMap(r -> r.getFailureMessages().stream())
                    .toList();

            if (!errors.isEmpty()) {
                return Result.failure(errors);
            }

            return Result.success(claimToken);

        } catch (ParseException e) {
            return Result.failure("Failed to decode token");
        }
    }
}
