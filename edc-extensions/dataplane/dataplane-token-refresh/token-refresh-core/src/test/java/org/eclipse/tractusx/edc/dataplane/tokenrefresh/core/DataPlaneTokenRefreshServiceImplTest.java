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

package org.eclipse.tractusx.edc.dataplane.tokenrefresh.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.dataplane.spi.AccessTokenData;
import org.eclipse.edc.connector.dataplane.spi.store.AccessTokenDataStore;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.keys.spi.LocalPublicKeyService;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.tractusx.edc.dataplane.tokenrefresh.core.TestFunctions.createJwt;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.AUDIENCE_PROPERTY;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_EXPIRES_IN;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_REFRESH_ENDPOINT;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_REFRESH_TOKEN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DataPlaneTokenRefreshServiceImplTest {
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private final AccessTokenDataStore accessTokenDataStore = mock();
    private final TokenGenerationService tokenGenService = mock();
    private final TokenValidationService tokenValidationService = mock();
    private final DidPublicKeyResolver didPublicKeyResolver = mock();
    private final LocalPublicKeyService localPublicKeyService = mock();
    private final ParticipantContextSupplier participantContextSupplier = () -> ServiceResult.success(
            ParticipantContext.Builder.newInstance().participantContextId("participantContextId").identity("identity").build()
    );

    private final DataPlaneTokenRefreshServiceImpl accessTokenService = new DataPlaneTokenRefreshServiceImpl(Clock.systemUTC(),
            tokenValidationService, didPublicKeyResolver, localPublicKeyService, accessTokenDataStore, tokenGenService, mock(), mock(),
            "https://example.com", "did:web:provider", 1, 300L,
            () -> "keyid", mock(), new ObjectMapper(), participantContextSupplier);

    @Test
    void obtainToken() {
        var params = TokenParameters.Builder.newInstance().claims("foo", "bar").claims("jti", "baz").header("qux", "quz").build();
        var address = DataAddress.Builder.newInstance().type("test-type").build();

        when(tokenGenService.generate(any(), any(), any(TokenDecorator[].class))).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("foo-token").build()));
        when(accessTokenDataStore.store(any(AccessTokenData.class))).thenReturn(StoreResult.success());

        var result = accessTokenService.obtainToken(params, address, Map.of("fizz", "buzz", "refreshToken", "getsOverwritten", AUDIENCE_PROPERTY, "audience"));
        assertThat(result).isSucceeded().extracting(TokenRepresentation::getToken).isEqualTo("foo-token");
        assertThat(result.getContent().getAdditional())
                .containsKeys("fizz", EDR_PROPERTY_REFRESH_TOKEN, EDR_PROPERTY_EXPIRES_IN, EDR_PROPERTY_REFRESH_ENDPOINT)
                .containsEntry(EDR_PROPERTY_REFRESH_TOKEN, "foo-token");

        verify(tokenGenService, times(2)).generate(any(), any(), any(TokenDecorator[].class));
        verify(accessTokenDataStore).store(any(AccessTokenData.class));
    }

    @Test
    void obtainToken_withAdditionalProperties() {
        var params = TokenParameters.Builder.newInstance().claims("foo", "bar").claims("jti", "baz").header("qux", "quz").build();
        var address = DataAddress.Builder.newInstance().type("test-type").build();

        when(tokenGenService.generate(any(), any(), any(TokenDecorator[].class))).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("foo-token").build()));
        when(accessTokenDataStore.store(any(AccessTokenData.class))).thenReturn(StoreResult.success());

        var result = accessTokenService.obtainToken(params, address, Map.of("foo", "bar", AUDIENCE_PROPERTY, "audience"));
        assertThat(result).isSucceeded().extracting(TokenRepresentation::getToken).isEqualTo("foo-token");

        verify(tokenGenService, times(2)).generate(any(), any(), any(TokenDecorator[].class));
        verify(accessTokenDataStore).store(argThat(accessTokenData -> accessTokenData.additionalProperties().get("foo").equals("bar")));
    }

    @Test
    void obtainToken_invalidParams() {
        assertThatThrownBy(() -> accessTokenService.obtainToken(null, DataAddress.Builder.newInstance().type("foo").build(), Map.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> accessTokenService.obtainToken(TokenParameters.Builder.newInstance().build(), null, Map.of()))
                .isInstanceOf(NullPointerException.class);

    }

    @Test
    void obtainToken_noTokenId() {
        var params = TokenParameters.Builder.newInstance().claims("foo", "bar")/* missing: .claims("jti", "baz")*/.header("qux", "quz").build();
        var address = DataAddress.Builder.newInstance().type("test-type").build();

        when(tokenGenService.generate(any(), any(), any(TokenDecorator[].class))).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("foo-token").build()));
        when(accessTokenDataStore.store(any(AccessTokenData.class))).thenReturn(StoreResult.success());

        var result = accessTokenService.obtainToken(params, address, Map.of(AUDIENCE_PROPERTY, "audience"));
        assertThat(result).isSucceeded().extracting(TokenRepresentation::getToken).isEqualTo("foo-token");

        verify(tokenGenService, times(2)).generate(any(), any(), any(TokenDecorator[].class));
        verify(accessTokenDataStore).store(argThat(accessTokenData -> UUID_PATTERN.matcher(accessTokenData.id()).matches()));
    }

    @Test
    void obtainToken_creationFails() {
        var params = TokenParameters.Builder.newInstance().claims("foo", "bar").claims("jti", "baz").header("qux", "quz").build();
        var address = DataAddress.Builder.newInstance().type("test-type").build();

        when(tokenGenService.generate(any(), any(), any(TokenDecorator[].class))).thenReturn(Result.failure("test failure"));

        var result = accessTokenService.obtainToken(params, address, Map.of());
        assertThat(result).isFailed().detail().contains("test failure");

        verify(tokenGenService).generate(any(), any(), any(TokenDecorator[].class));
        verifyNoMoreInteractions(accessTokenDataStore);
    }

    @Test
    void obtainToken_storingFails() {
        var params = TokenParameters.Builder.newInstance().claims("foo", "bar").claims("jti", "baz").header("qux", "quz").build();
        var address = DataAddress.Builder.newInstance().type("test-type").build();

        when(tokenGenService.generate(any(), any(), any(TokenDecorator[].class))).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("foo-token").build()));
        when(accessTokenDataStore.store(any(AccessTokenData.class))).thenReturn(StoreResult.alreadyExists("test failure"));

        var result = accessTokenService.obtainToken(params, address, Map.of(AUDIENCE_PROPERTY, "audience"));
        assertThat(result).isFailed().detail().isEqualTo("test failure");

        verify(tokenGenService, times(2)).generate(any(), any(), any(TokenDecorator[].class));
        verify(accessTokenDataStore).store(any(AccessTokenData.class));
    }

    @Test
    void resolve() {
        var tokenId = "test-id";
        var claimToken = ClaimToken.Builder.newInstance().claim("jti", tokenId).build();
        when(tokenValidationService.validate(anyString(), any(), anyList()))
                .thenReturn(Result.success(claimToken));
        when(accessTokenDataStore.getById(eq(tokenId))).thenReturn(new AccessTokenData(tokenId, ClaimToken.Builder.newInstance().build(),
                DataAddress.Builder.newInstance().type("test-type").build()));

        var result = accessTokenService.resolve("some-jwt");
        assertThat(result).isSucceeded()
                .satisfies(atd -> assertThat(atd.id()).isEqualTo(tokenId));
        verify(tokenValidationService).validate(eq("some-jwt"), any(), anyList());
        verify(accessTokenDataStore).getById(eq(tokenId));
    }

    @Test
    void resolve_whenValidationFails() {
        when(tokenValidationService.validate(anyString(), any(), anyList()))
                .thenReturn(Result.failure("test-failure"));

        var result = accessTokenService.resolve("some-jwt");
        assertThat(result).isFailed()
                .detail().isEqualTo("test-failure");
        verify(tokenValidationService).validate(eq("some-jwt"), any(), anyList());
        verifyNoInteractions(accessTokenDataStore);
    }

    @Test
    void resolve_whenTokenIdNotFound() {
        var tokenId = "test-id";
        var claimToken = ClaimToken.Builder.newInstance().claim("jti", tokenId).build();
        when(tokenValidationService.validate(anyString(), any(), anyList()))
                .thenReturn(Result.success(claimToken));
        when(accessTokenDataStore.getById(eq(tokenId))).thenReturn(null);

        var result = accessTokenService.resolve("some-jwt");
        assertThat(result).isFailed()
                .detail().isEqualTo("AccessTokenData with ID 'test-id' does not exist.");
        verify(tokenValidationService).validate(eq("some-jwt"), any(), anyList());
        verify(accessTokenDataStore).getById(eq(tokenId));
    }

    @Test
    void refresh_whenAccessTokenDataNotResolved() {
        var accessToken = "foo-bar";
        var refreshToken = "fizz-buzz";
        when(tokenValidationService.validate(eq(accessToken), any(), anyList()))
                .thenReturn(Result.failure("test-failure"));

        assertThat(accessTokenService.refreshToken(refreshToken, accessToken))
                .isFailed()
                .detail()
                .isEqualTo("Authentication token validation failed: test-failure");

        verify(tokenValidationService).validate(eq(accessToken), any(), anyList());
        verifyNoMoreInteractions(tokenValidationService, tokenGenService, didPublicKeyResolver, accessTokenDataStore);
    }

    @Test
    void refresh_whenRegeneratingTokenFails() {
        var accessToken = createJwt("quizz-quazz");
        var authenticationToken = createJwt("foo-bar");
        var refreshToken = createJwt("fizz-buzz");

        var tokenId = "token-id";
        when(tokenValidationService.validate(eq(authenticationToken), any(), anyList()))
                .thenReturn(Result.success(ClaimToken.Builder.newInstance().claim("token", accessToken).build()));

        when(tokenValidationService.validate(eq(accessToken), any(), any(TokenValidationRule[].class)))
                .thenReturn(Result.success(ClaimToken.Builder.newInstance().claim("jti", tokenId).build()));

        when(accessTokenDataStore.getById(eq(tokenId))).thenReturn(new AccessTokenData(tokenId, ClaimToken.Builder.newInstance().claim("claim1", "value1").build(),
                DataAddress.Builder.newInstance().type("type").build(), Map.of("fizz", "buzz")));

        when(tokenGenService.generate(any(), any(), any(TokenDecorator[].class))).thenReturn(Result.failure("generator-failure"));


        assertThat(accessTokenService.refreshToken(refreshToken, authenticationToken))
                .isFailed()
                .detail()
                .startsWith("Failed to regenerate access/refresh token pair: ");

        verify(tokenValidationService).validate(eq(accessToken), any(), any(TokenValidationRule[].class));
        verify(tokenValidationService).validate(eq(authenticationToken), any(), anyList());
        verify(accessTokenDataStore).getById(tokenId);
        verify(tokenGenService, times(2)).generate(any(), any(), any(TokenDecorator[].class));
        verifyNoMoreInteractions(tokenValidationService, tokenGenService, didPublicKeyResolver, accessTokenDataStore);
    }

    @Test
    void refresh_whenStoreFails() {
        var accessToken = createJwt("quizz-quazz");
        var authenticationToken = createJwt("foo-bar");
        var refreshToken = createJwt("fizz-buzz");

        var tokenId = "token-id";
        when(tokenValidationService.validate(eq(authenticationToken), any(), anyList()))
                .thenReturn(Result.success(ClaimToken.Builder.newInstance().claim("token", accessToken).build()));

        when(tokenValidationService.validate(eq(accessToken), any(), any(TokenValidationRule[].class)))
                .thenReturn(Result.success(ClaimToken.Builder.newInstance().claim("jti", tokenId).build()));


        when(accessTokenDataStore.getById(eq(tokenId))).thenReturn(new AccessTokenData(tokenId, ClaimToken.Builder.newInstance().claim("claim1", "value1").build(),
                DataAddress.Builder.newInstance().type("type").build(), Map.of("fizz", "buzz")));

        when(tokenGenService.generate(any(), any(), any(TokenDecorator[].class))).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().build()));

        when(accessTokenDataStore.update(any())).thenReturn(StoreResult.alreadyExists("test-failure"));

        assertThat(accessTokenService.refreshToken(refreshToken, authenticationToken))
                .isFailed()
                .detail()
                .startsWith("test-failure");

        verify(tokenValidationService).validate(eq(authenticationToken), any(), anyList());
        verify(tokenValidationService).validate(eq(accessToken), any(), any(TokenValidationRule[].class));
        verify(accessTokenDataStore).getById(tokenId);
        verify(tokenGenService, times(2)).generate(any(), any(), any(TokenDecorator[].class));
        verify(accessTokenDataStore).update(any());
        verifyNoMoreInteractions(tokenValidationService, tokenGenService, didPublicKeyResolver, accessTokenDataStore);
    }

    @Test
    void refresh_successful() {
        var accessToken = createJwt("quizz-quazz");
        var authenticationToken = createJwt("foo-bar");
        var refreshToken = createJwt("fizz-buzz");

        var tokenId = "token-id";
        when(tokenValidationService.validate(eq(authenticationToken), any(), anyList()))
                .thenReturn(Result.success(ClaimToken.Builder.newInstance().claim("token", accessToken).build()));

        when(tokenValidationService.validate(eq(accessToken), any(), any(TokenValidationRule[].class)))
                .thenReturn(Result.success(ClaimToken.Builder.newInstance().claim("jti", tokenId).build()));

        when(accessTokenDataStore.getById(eq(tokenId))).thenReturn(new AccessTokenData(tokenId, ClaimToken.Builder.newInstance().claim("claim1", "value1").build(),
                DataAddress.Builder.newInstance().type("type").build(), Map.of("fizz", "buzz")));

        when(tokenGenService.generate(any(), any(), any(TokenDecorator[].class)))
                .thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("fizz-token").build()))
                .thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("buzz-token").build()));

        when(accessTokenDataStore.update(any())).thenReturn(StoreResult.success());

        assertThat(accessTokenService.refreshToken(refreshToken, authenticationToken))
                .isSucceeded()
                .satisfies(tr -> {
                    assertThat(tr.accessToken()).isEqualTo("fizz-token");
                    assertThat(tr.refreshToken()).isEqualTo("buzz-token");
                });

        verify(tokenValidationService).validate(eq(authenticationToken), any(), anyList());
        verify(tokenValidationService).validate(eq(accessToken), any(), any(TokenValidationRule[].class));
        verify(accessTokenDataStore).getById(tokenId);
        verify(tokenGenService, times(2)).generate(any(), any(), any(TokenDecorator[].class));
        verify(accessTokenDataStore).update(any());
        verifyNoMoreInteractions(tokenValidationService, tokenGenService, didPublicKeyResolver, accessTokenDataStore);
    }
}
