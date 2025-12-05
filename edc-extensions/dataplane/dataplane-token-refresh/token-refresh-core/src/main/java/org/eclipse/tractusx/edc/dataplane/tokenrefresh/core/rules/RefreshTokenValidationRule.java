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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimNames;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.tractusx.edc.dataplane.tokenrefresh.core.RefreshToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * Validates that the refresh token information associated with a token's ID ({@code jti}), that is stored in the {@link Vault}
 * matches a refresh token string. The refresh token in question is passed into the CTor.
 */
public class RefreshTokenValidationRule implements TokenValidationRule {
    private final Vault vault;
    private final String incomingRefreshToken;
    private final ObjectMapper objectMapper;
    private final ParticipantContext participantContext;

    public RefreshTokenValidationRule(Vault vault, String incomingRefreshToken, ObjectMapper objectMapper, ParticipantContext participantContext) {
        this.vault = vault;
        this.incomingRefreshToken = incomingRefreshToken;
        this.objectMapper = objectMapper;
        this.participantContext = participantContext;
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken accessToken, @Nullable Map<String, Object> additional) {

        var tokenId = accessToken.getStringClaim(JWTClaimNames.JWT_ID);
        var storedRefreshTokenJson = vault.resolveSecret(participantContext.getParticipantContextId(), tokenId);
        if (storedRefreshTokenJson == null) {
            return failure("No refresh token with the ID '%s' was found in the vault.".formatted(tokenId));
        }
        return parse(storedRefreshTokenJson)
                .compose(rt -> incomingRefreshToken.equals(rt.refreshToken()) ?
                        success() :
                        failure("Provided refresh token does not match the stored refresh token."));
    }

    private Result<RefreshToken> parse(String storedRefreshTokenJson) {
        try {
            return success(objectMapper.readValue(storedRefreshTokenJson, RefreshToken.class));
        } catch (JsonProcessingException e) {
            return failure("Failed to parse stored secret: " + e.getMessage());
        }
    }

}
