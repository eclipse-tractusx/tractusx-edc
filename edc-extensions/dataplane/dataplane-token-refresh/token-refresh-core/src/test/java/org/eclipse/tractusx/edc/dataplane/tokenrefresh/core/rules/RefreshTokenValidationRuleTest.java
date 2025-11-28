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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.tractusx.edc.dataplane.tokenrefresh.core.TestFunctions.createAccessToken;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RefreshTokenValidationRuleTest {

    private static final String TEST_TOKEN_ID = "test-jti";
    private static final String TEST_REFRESH_TOKEN = "test-refresh-token";
    private final Vault vault = mock();
    private final String participantContextId = "participantContextId";
    private final ParticipantContext participantContext = ParticipantContext.Builder.newInstance()
            .participantContextId(participantContextId).identity("identity").build();
    private final RefreshTokenValidationRule rule = new RefreshTokenValidationRule(vault, TEST_REFRESH_TOKEN, new ObjectMapper(), participantContext);

    @Test
    void checkRule_noAccessTokenDataEntryFound() {
        when(vault.resolveSecret(participantContextId, TEST_TOKEN_ID)).thenReturn(null);

        assertThat(rule.checkRule(createAccessToken(TEST_TOKEN_ID), Map.of()))
                .isFailed()
                .detail()
                .isEqualTo("No refresh token with the ID '%s' was found in the vault.".formatted(TEST_TOKEN_ID));
    }

    @Test
    void checkRule_noRefreshTokenStored() {
        when(vault.resolveSecret(participantContextId, TEST_TOKEN_ID)).thenReturn(null);

        assertThat(rule.checkRule(createAccessToken(TEST_TOKEN_ID), Map.of()))
                .isFailed()
                .detail()
                .isEqualTo("No refresh token with the ID 'test-jti' was found in the vault.");
    }

    @Test
    void checkRule_refreshTokenNotString() {
        when(vault.resolveSecret(participantContextId, TEST_TOKEN_ID)).thenReturn(
                """
                        {
                          "refreshToken": 42
                        }
                        """);

        assertThat(rule.checkRule(createAccessToken(TEST_TOKEN_ID), Map.of()))
                .isFailed()
                .detail()
                .isEqualTo("Provided refresh token does not match the stored refresh token.");
    }

    @Test
    void checkRule_refreshTokenDoesNotMatch() {
        when(vault.resolveSecret(participantContextId, TEST_TOKEN_ID)).thenReturn(
                """
                        {
                          "refreshToken": "someRefreshToken"
                        }
                        """);

        assertThat(rule.checkRule(createAccessToken(TEST_TOKEN_ID), Map.of()))
                .isFailed()
                .detail()
                .isEqualTo("Provided refresh token does not match the stored refresh token.");
    }

    @Test
    void checkRule_success() {
        when(vault.resolveSecret(participantContextId, TEST_TOKEN_ID)).thenReturn(
                """
                        {
                          "refreshToken": "%s"
                        }
                        """.formatted(TEST_REFRESH_TOKEN));

        assertThat(rule.checkRule(createAccessToken(TEST_TOKEN_ID), Map.of()))
                .isSucceeded();
    }

    @Test
    void checkRule_invalidJson() {
        when(vault.resolveSecret(participantContextId, TEST_TOKEN_ID)).thenReturn(
                "nope-thats-not-json");

        assertThat(rule.checkRule(createAccessToken(TEST_TOKEN_ID), Map.of()))
                .isFailed()
                .detail()
                .startsWith("Failed to parse stored secret");
    }

}
