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
import org.eclipse.edc.connector.dataplane.spi.AccessTokenData;
import org.eclipse.edc.connector.dataplane.spi.store.AccessTokenDataStore;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

import static org.eclipse.tractusx.edc.dataplane.tokenrefresh.core.DataPlaneTokenRefreshServiceImpl.ACCESS_TOKEN_CLAIM;
import static org.eclipse.tractusx.edc.dataplane.tokenrefresh.core.DataPlaneTokenRefreshServiceImpl.REFRESH_TOKEN_PROPERTY;

public class RefreshTokenMustExistRule implements TokenValidationRule {
    private static final String AUDIENCE_PROPERTY = "audience";
    private final AccessTokenDataStore accessTokenDataStore;
    private final String refreshToken;

    public RefreshTokenMustExistRule(AccessTokenDataStore accessTokenDataStore, String refreshToken) {
        this.accessTokenDataStore = accessTokenDataStore;
        this.refreshToken = refreshToken;
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        var oldAccessToken = toVerify.getStringClaim(ACCESS_TOKEN_CLAIM);
        var tokenId = toVerify.getStringClaim(JWTClaimNames.JWT_ID);
        var issuer = toVerify.getStringClaim(JWTClaimNames.ISSUER);
        return Optional.ofNullable(accessTokenDataStore.getById(tokenId))
                .map(accessTokenData -> checkExists(accessTokenData, refreshToken, issuer))
                .orElse(Result.failure("No AccessTokenData entry found for token-ID '%s'.".formatted(tokenId)));
    }

    private Result<Void> checkExists(AccessTokenData accessTokenData, String refreshToken, String issuer) {
        var storedRefreshToken = accessTokenData.additionalProperties().getOrDefault(REFRESH_TOKEN_PROPERTY, null);
        if (!(storedRefreshToken instanceof String)) {
            return Result.failure("Property '%s' expected to be String but was %s".formatted(REFRESH_TOKEN_PROPERTY, storedRefreshToken == null ? "null" : storedRefreshToken.getClass()));
        }
        if (!refreshToken.equals(storedRefreshToken)) {
            return Result.failure("Provided refresh token does not match the stored refresh token.");
        }
        var audience = accessTokenData.additionalProperties().getOrDefault(AUDIENCE_PROPERTY, null);
        if (!(audience instanceof String)) {
            return Result.failure("Property '%s' expected to be String but was %s".formatted(AUDIENCE_PROPERTY, audience == null ? "null" : audience.getClass()));
        }

        if (!audience.equals(issuer)) {
            return Result.failure("Principal '%s' is not authorized to refresh this token.".formatted(issuer));
        }

        return Result.success();
    }
}
