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
import static org.eclipse.tractusx.edc.dataplane.tokenrefresh.core.TestFunctions.createAuthenticationToken;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.AUDIENCE_PROPERTY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthTokenAudienceRuleTest {

    private static final String TEST_TOKEN_ID = "token-id";
    private static final Object TEST_REFRESH_TOKEN = "refresh-token";
    private final AccessTokenDataStore store = mock();
    private final AuthTokenAudienceRule rule = new AuthTokenAudienceRule(store);

    @Test
    void checkRule_issuerDoesNotMatchAudience() {
        when(store.getById(TEST_TOKEN_ID)).thenReturn(new AccessTokenData(TEST_TOKEN_ID,
                ClaimToken.Builder.newInstance().build(),
                DataAddress.Builder.newInstance().type("test-type").build(),
                Map.of(AUDIENCE_PROPERTY, "did:web:alice")));

        assertThat(rule.checkRule(createAuthenticationToken(TEST_TOKEN_ID), Map.of()))
                .isFailed()
                .detail()
                .isEqualTo("Principal 'did:web:bob' is not authorized to refresh this token.");
    }

    @Test
    void checkRule_audienceNotString() {
        when(store.getById(TEST_TOKEN_ID)).thenReturn(new AccessTokenData(TEST_TOKEN_ID,
                ClaimToken.Builder.newInstance().build(),
                DataAddress.Builder.newInstance().type("test-type").build(),
                Map.of(AUDIENCE_PROPERTY, 42L)));

        assertThat(rule.checkRule(createAuthenticationToken(TEST_TOKEN_ID), Map.of()))
                .isFailed()
                .detail()
                .isEqualTo("Property '%s' was expected to be java.lang.String but was class java.lang.Long.".formatted(AUDIENCE_PROPERTY));
    }

    @Test
    void checkRule_audienceNotPresent() {
        when(store.getById(TEST_TOKEN_ID)).thenReturn(new AccessTokenData(TEST_TOKEN_ID,
                ClaimToken.Builder.newInstance().build(),
                DataAddress.Builder.newInstance().type("test-type").build(),
                Map.of()));

        assertThat(rule.checkRule(createAuthenticationToken(TEST_TOKEN_ID), Map.of()))
                .isFailed()
                .detail()
                .isEqualTo("Property '%s' was expected to be java.lang.String but was null.".formatted(AUDIENCE_PROPERTY));
    }

    @Test
    void checkRule_accessTokenDataNotFound() {
        when(store.getById(TEST_TOKEN_ID)).thenReturn(null);

        assertThat(rule.checkRule(createAuthenticationToken(TEST_TOKEN_ID), Map.of()))
                .isFailed()
                .detail()
                .isEqualTo("Token with id '%s' not found".formatted(TEST_TOKEN_ID));
    }
}