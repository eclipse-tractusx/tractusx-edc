/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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
 */

package org.eclipse.tractusx.edc.dataplane.tokenrefresh.core.rules;

import com.nimbusds.jwt.JWTClaimNames;
import org.eclipse.edc.connector.dataplane.spi.store.AccessTokenDataStore;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static org.eclipse.tractusx.edc.dataplane.tokenrefresh.core.TokenFunctions.getTokenId;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.AUDIENCE_PROPERTY;


/**
 * Validates that the {@code iss} claim of a token is equal to the {@code audience} property found on the {@link org.eclipse.edc.connector.dataplane.spi.AccessTokenData}
 * that is associated with that token (using the {@code jti} claim).
 */
public class AuthTokenAudienceRule implements TokenValidationRule {
    private final AccessTokenDataStore store;

    public AuthTokenAudienceRule(AccessTokenDataStore store) {
        this.store = store;
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken authenticationToken, @Nullable Map<String, Object> map) {
        var issuer = authenticationToken.getStringClaim(JWTClaimNames.ISSUER);
        var accessToken = authenticationToken.getStringClaim("token");
        if (accessToken == null) {
            return Result.failure("Authentication token must contain a 'token' claim");
        }
        var tokenId = getTokenId(accessToken);

        var accessTokenData = store.getById(tokenId);
        if (accessTokenData == null) {
            return Result.failure("Token with id '%s' not found".formatted(tokenId));
        }
        var expectedAudience = accessTokenData.additionalProperties().getOrDefault(AUDIENCE_PROPERTY, null);
        if (expectedAudience instanceof String expectedAud) {
            return expectedAud.equals(issuer) ? Result.success() : Result.failure("Principal '%s' is not authorized to refresh this token.".formatted(issuer));
        }

        return Result.failure("Property '%s' was expected to be java.lang.String but was %s.".formatted(AUDIENCE_PROPERTY, expectedAudience == null ? null : expectedAudience.getClass()));
    }
}
