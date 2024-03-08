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

import org.eclipse.edc.connector.dataplane.spi.AccessTokenData;
import org.eclipse.edc.connector.dataplane.spi.store.AccessTokenDataStore;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RefreshTokenMustExistRuleTest {

    private static final String TEST_TOKEN_ID = "test-jti";
    private static final String TEST_REFRESH_TOKEN = "test-refresh-token";
    private final AccessTokenDataStore accessTokenData = mock();
    private final RefreshTokenMustExistRule rule = new RefreshTokenMustExistRule(accessTokenData, TEST_REFRESH_TOKEN);

    @Test
    void checkRule_noAccessTokenDataEntryFound() {
        when(accessTokenData.getById(TEST_TOKEN_ID)).thenReturn(null);

        assertThat(rule.checkRule(createToken(), Map.of()))
                .isFailed()
                .detail()
                .isEqualTo("No AccessTokenData entry found for token-ID '%s'.".formatted(TEST_TOKEN_ID));
    }

    @Test
    void checkRule_noRefreshTokenStored() {
        when(accessTokenData.getById(TEST_TOKEN_ID)).thenReturn(new AccessTokenData(TEST_TOKEN_ID,
                ClaimToken.Builder.newInstance().build(),
                DataAddress.Builder.newInstance().type("test-type").build(),
                Map.of("foo", "var")));

        assertThat(rule.checkRule(createToken(), Map.of()))
                .isFailed()
                .detail()
                .isEqualTo("Property 'refreshToken' expected to be String but was null");
    }

    @Test
    void checkRule_refreshTokenNotString() {
        when(accessTokenData.getById(TEST_TOKEN_ID)).thenReturn(new AccessTokenData(TEST_TOKEN_ID,
                ClaimToken.Builder.newInstance().build(),
                DataAddress.Builder.newInstance().type("test-type").build(),
                Map.of("refreshToken", 42L)));

        assertThat(rule.checkRule(createToken(), Map.of()))
                .isFailed()
                .detail()
                .isEqualTo("Property 'refreshToken' expected to be String but was class java.lang.Long");
    }

    @Test
    void checkRule_refreshTokenDoesNotMatch() {
        when(accessTokenData.getById(TEST_TOKEN_ID)).thenReturn(new AccessTokenData(TEST_TOKEN_ID,
                ClaimToken.Builder.newInstance().build(),
                DataAddress.Builder.newInstance().type("test-type").build(),
                Map.of("refreshToken", "this-is-not-equal")));

        assertThat(rule.checkRule(createToken(), Map.of()))
                .isFailed()
                .detail()
                .isEqualTo("Provided refresh token does not match the stored refresh token.");
    }

    @Test
    void checkRule_issuerDoesNotMatchAudience() {
        when(accessTokenData.getById(TEST_TOKEN_ID)).thenReturn(new AccessTokenData(TEST_TOKEN_ID,
                ClaimToken.Builder.newInstance().build(),
                DataAddress.Builder.newInstance().type("test-type").build(),
                Map.of("refreshToken", TEST_REFRESH_TOKEN, "audience", "did:web:alice")));

        assertThat(rule.checkRule(createToken(), Map.of()))
                .isFailed()
                .detail()
                .isEqualTo("Principal 'did:web:bob' is not authorized to refresh this token.");
    }

    @Test
    void checkRule_success() {
        when(accessTokenData.getById(TEST_TOKEN_ID)).thenReturn(new AccessTokenData(TEST_TOKEN_ID,
                ClaimToken.Builder.newInstance().build(),
                DataAddress.Builder.newInstance().type("test-type").build(),
                Map.of("refreshToken", TEST_REFRESH_TOKEN, "audience", "did:web:bob")));

        assertThat(rule.checkRule(createToken(), Map.of()))
                .isSucceeded();
    }

    private ClaimToken createToken() {
        return ClaimToken.Builder.newInstance()
                .claim("access_token", "test-access-token")
                .claim("jti", TEST_TOKEN_ID)
                .claim("iss", "did:web:bob")
                .build();
    }
}