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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.boot.vault.InMemoryVault;
import org.eclipse.edc.connector.dataplane.framework.store.InMemoryAccessTokenDataStore;
import org.eclipse.edc.connector.dataplane.spi.AccessTokenData;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.keys.spi.LocalPublicKeyService;
import org.eclipse.edc.keys.spi.PrivateKeyResolver;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.security.token.jwt.CryptoConverter;
import org.eclipse.edc.security.token.jwt.DefaultJwsSignerProvider;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.token.TokenValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.AUDIENCE_PROPERTY;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_EXPIRES_IN;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_REFRESH_ENDPOINT;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_REFRESH_TOKEN;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
class DataPlaneTokenRefreshServiceImplComponentTest {

    public static final String PROVIDER_BPN = "BPN0000ALICE";
    public static final String CONSUMER_BPN = "BPN0000BOB";
    public static final String TEST_REFRESH_ENDPOINT = "https://fizz.buzz.com";
    public static final String CONSUMER_DID = "did:web:bob";
    public static final String PROVIDER_DID = "did:web:alice";
    private final DidPublicKeyResolver didPkResolverMock = mock();
    private final LocalPublicKeyService localPublicKeyService = mock();
    private final PrivateKeyResolver privateKeyResolver = mock();
    private final ParticipantContextSupplier participantContextSupplier = () -> ServiceResult.success(
            ParticipantContext.Builder.newInstance().participantContextId("participantContextId").identity("identity").build()
    );
    private static final long TOKEN_EXPIRY_SECONDS = 300L;
    private DataPlaneTokenRefreshServiceImpl tokenRefreshService;
    private final InMemoryAccessTokenDataStore tokenDataStore = new InMemoryAccessTokenDataStore(CriterionOperatorRegistryImpl.ofDefaults());
    private final Monitor monitor = mock();
    private final InMemoryVault vault = new InMemoryVault(mock(), null);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MutableClock clock = new MutableClock(Instant.now());
    private ECKey consumerKey;
    private ECKey providerKey;

    @BeforeEach
    void setup() throws JOSEException {
        var privateKeyAlias = "privateKeyAlias";
        providerKey = new ECKeyGenerator(Curve.P_384).keyID(PROVIDER_BPN + "#provider-key").keyUse(KeyUse.SIGNATURE).generate();
        consumerKey = new ECKeyGenerator(Curve.P_384).keyID(CONSUMER_DID + "#consumer-key").keyUse(KeyUse.SIGNATURE).generate();

        when(monitor.withPrefix(anyString())).thenReturn(monitor);
        tokenRefreshService = new DataPlaneTokenRefreshServiceImpl(clock,
                new TokenValidationServiceImpl(),
                didPkResolverMock,
                localPublicKeyService,
                tokenDataStore,
                new JwtGenerationService(new DefaultJwsSignerProvider(privateKeyResolver)),
                () -> privateKeyAlias,
                monitor,
                TEST_REFRESH_ENDPOINT,
                1,
                TOKEN_EXPIRY_SECONDS,
                () -> providerKey.getKeyID(),
                vault,
                objectMapper, participantContextSupplier);

        when(privateKeyResolver.resolvePrivateKey("participantContextId", privateKeyAlias)).thenReturn(Result.success(providerKey.toPrivateKey()));
        when(localPublicKeyService.resolveKey(eq(consumerKey.getKeyID()))).thenReturn(Result.success(consumerKey.toPublicKey()));
        when(localPublicKeyService.resolveKey(eq(providerKey.getKeyID()))).thenReturn(Result.success(providerKey.toPublicKey()));

        when(didPkResolverMock.resolveKey(eq(consumerKey.getKeyID()))).thenReturn(Result.success(consumerKey.toPublicKey()));
        when(didPkResolverMock.resolveKey(eq(providerKey.getKeyID()))).thenReturn(Result.success(providerKey.toPublicKey()));
    }

    @DisplayName("Verify that a correct EDR is obtained")
    @Test
    void obtainToken() {
        var tokenId = "test-token-id";
        var edr = tokenRefreshService.obtainToken(tokenParams(tokenId), DataAddress.Builder.newInstance().type("test-type").build(), Map.of(AUDIENCE_PROPERTY, CONSUMER_DID));
        assertThat(edr).isSucceeded();
        // assert access token contents
        assertThat(asClaims(edr.getContent().getToken()))
                .containsEntry("iss", PROVIDER_BPN)
                .containsEntry("sub", PROVIDER_BPN)
                .containsEntry("aud", List.of(CONSUMER_BPN))
                .containsKey("exp");

        // assert additional properties -> refresh token
        assertThat(edr.getContent().getAdditional())
                .containsEntry(EDR_PROPERTY_REFRESH_ENDPOINT, TEST_REFRESH_ENDPOINT)
                .containsKey(EDR_PROPERTY_REFRESH_TOKEN)
                .containsKey(EDR_PROPERTY_EXPIRES_IN);

        // verify that the correct data was stored
        var storedData = tokenDataStore.getById(tokenId);
        assertThat(storedData).isNotNull();
        assertThat(storedData.additionalProperties())
                .hasSize(2)
                .containsEntry(AUDIENCE_PROPERTY, CONSUMER_DID)
                .containsEntry("authType", "bearer");
    }

    @DisplayName("Verify that a token can be refreshed")
    @Test
    void refresh_success() throws JOSEException {

        var tokenId = "test-token-id";
        var edr = tokenRefreshService.obtainToken(tokenParams(tokenId), DataAddress.Builder.newInstance().type("test-type").build(), Map.of(AUDIENCE_PROPERTY, CONSUMER_DID))
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

        var accessToken = edr.getToken();
        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES384).keyID(consumerKey.getKeyID()).build();
        var claimsSet = getAuthTokenClaims(tokenId, accessToken).build();

        var signedAuthToken = new SignedJWT(jwsHeader, claimsSet);
        signedAuthToken.sign(CryptoConverter.createSigner(consumerKey));
        var tokenResponse = tokenRefreshService.refreshToken(edr.getAdditional().get(EDR_PROPERTY_REFRESH_TOKEN).toString(), signedAuthToken.serialize());

        assertThat(tokenResponse).withFailMessage(tokenResponse::getFailureDetail).isSucceeded()
                .satisfies(tr -> assertThat(tr.refreshToken()).isNotNull())
                .satisfies(tr -> assertThat(tr.accessToken()).isNotNull());

        assertThat(tokenDataStore.getById(tokenId).additionalProperties())
                .hasSize(2)
                .doesNotContainKey("refreshToken");
    }

    @DisplayName("Verify that a refresh whose response got lost can be repeated with the same refresh token")
    @Test
    void refresh_whenResponseWasLost_returnsCurrentRefreshTokenAndFreshAccessToken() throws JOSEException {
        var tokenId = "test-token-id";
        var edr = tokenRefreshService.obtainToken(tokenParams(tokenId), DataAddress.Builder.newInstance().type("test-type").build(), Map.of(AUDIENCE_PROPERTY, CONSUMER_DID))
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

        var refreshToken = edr.getAdditional().get(EDR_PROPERTY_REFRESH_TOKEN).toString();

        // the client never sees this response, e.g. because a proxy in between timed out
        var lostResponse = tokenRefreshService.refreshToken(refreshToken, createAuthToken(tokenId, edr.getToken()))
                .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        clock.advanceBy(Duration.ofSeconds(10));

        // ...so it retries with the only refresh token it has, and gets the very same pair back
        var retriedResponse = tokenRefreshService.refreshToken(refreshToken, createAuthToken(tokenId, edr.getToken()));

        assertThat(retriedResponse).withFailMessage(retriedResponse::getFailureDetail).isSucceeded()
                // the refresh token is the one the client failed to receive, not a rotated one
                .satisfies(tr -> assertThat(tr.refreshToken()).isEqualTo(lostResponse.refreshToken()))
                // the access token is minted fresh, so the client gets a usable one however late it retries
                .satisfies(tr -> assertThat(tokenRefreshService.resolve(tr.accessToken())).isSucceeded());
    }

    @DisplayName("Verify that the token pair handed out on a repeat can be refreshed again")
    @Test
    void refresh_afterRepeat_newTokenIsUsable() throws JOSEException {
        var tokenId = "test-token-id";
        var edr = tokenRefreshService.obtainToken(tokenParams(tokenId), DataAddress.Builder.newInstance().type("test-type").build(), Map.of(AUDIENCE_PROPERTY, CONSUMER_DID))
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

        var refreshToken = edr.getAdditional().get(EDR_PROPERTY_REFRESH_TOKEN).toString();

        tokenRefreshService.refreshToken(refreshToken, createAuthToken(tokenId, edr.getToken()))
                .orElseThrow(f -> new AssertionError(f.getFailureDetail()));
        var repeated = tokenRefreshService.refreshToken(refreshToken, createAuthToken(tokenId, edr.getToken()))
                .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        var nextResponse = tokenRefreshService.refreshToken(repeated.refreshToken(), createAuthToken(tokenId, repeated.accessToken()));

        assertThat(nextResponse).withFailMessage(nextResponse::getFailureDetail).isSucceeded()
                .satisfies(tr -> assertThat(tr.refreshToken()).isNotEqualTo(repeated.refreshToken()));
    }

    @DisplayName("Verify that a rotated refresh token is rejected once the client proved it received the new one")
    @Test
    void refresh_whenSupersededTokenWasAcknowledged_shouldFail() throws JOSEException {
        var tokenId = "test-token-id";
        var edr = tokenRefreshService.obtainToken(tokenParams(tokenId), DataAddress.Builder.newInstance().type("test-type").build(), Map.of(AUDIENCE_PROPERTY, CONSUMER_DID))
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

        var firstRefreshToken = edr.getAdditional().get(EDR_PROPERTY_REFRESH_TOKEN).toString();
        var second = tokenRefreshService.refreshToken(firstRefreshToken, createAuthToken(tokenId, edr.getToken()))
                .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        // using the second token proves that the client received it, which retires the first one
        tokenRefreshService.refreshToken(second.refreshToken(), createAuthToken(tokenId, second.accessToken()))
                .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        assertThat(tokenRefreshService.refreshToken(firstRefreshToken, createAuthToken(tokenId, edr.getToken())))
                .isFailed()
                .detail()
                .isEqualTo("Access token validation failed: Provided refresh token does not match the stored refresh token.");
    }

    @DisplayName("Verify that a rotated refresh token stays acceptable no matter how long the client takes to retry")
    @Test
    void refresh_whenResponseWasLostLongAgo_stillSucceeds() throws JOSEException {
        var tokenId = "test-token-id";
        var edr = tokenRefreshService.obtainToken(tokenParams(tokenId), DataAddress.Builder.newInstance().type("test-type").build(), Map.of(AUDIENCE_PROPERTY, CONSUMER_DID))
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

        var refreshToken = edr.getAdditional().get(EDR_PROPERTY_REFRESH_TOKEN).toString();
        var lostResponse = tokenRefreshService.refreshToken(refreshToken, createAuthToken(tokenId, edr.getToken()))
                .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        // the client still has not proven receipt, so its only refresh token must remain usable - there is no window
        // after which it is retired
        clock.advanceBy(Duration.ofDays(1));

        var retriedResponse = tokenRefreshService.refreshToken(refreshToken, createAuthToken(tokenId, edr.getToken()));

        assertThat(retriedResponse).withFailMessage(retriedResponse::getFailureDetail).isSucceeded()
                .satisfies(tr -> assertThat(tr.refreshToken()).isEqualTo(lostResponse.refreshToken()));
    }

    @DisplayName("Verify that a stolen refresh token cannot be used to refresh an access token")
    @Test
    void refresh_originalTokenWasIssuedToDifferentPrincipal() throws JOSEException {
        var trudyKey = new ECKeyGenerator(Curve.P_256).keyID("did:web:trudy#trudy-key").generate();
        when(didPkResolverMock.resolveKey(eq("did:web:trudy#trudy-key"))).thenReturn(Result.success(trudyKey.toPublicKey()));


        var tokenId = "test-token-id";
        var edr = tokenRefreshService.obtainToken(tokenParams(tokenId), DataAddress.Builder.newInstance().type("test-type").build(), Map.of(AUDIENCE_PROPERTY, "did:web:trudy"))
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

        // bob attempts to create an auth token with an EDR he stole from trudy
        var accessToken = edr.getToken();
        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES384).keyID(consumerKey.getKeyID()).build();
        var claimsSet = getAuthTokenClaims(tokenId, accessToken).build();

        var signedAuthToken = new SignedJWT(jwsHeader, claimsSet);
        signedAuthToken.sign(CryptoConverter.createSigner(consumerKey));
        var tokenResponse = tokenRefreshService.refreshToken(edr.getAdditional().get(EDR_PROPERTY_REFRESH_TOKEN).toString(), signedAuthToken.serialize());

        assertThat(tokenResponse).isFailed().detail().isEqualTo("Authentication token validation failed: Principal 'did:web:bob' is not authorized to refresh this token.");
    }

    @DisplayName("Verify that a spoofed refresh attempt is rejected ")
    @Test
    void refresh_issuerNotVerifiable() throws JOSEException {
        var trudyKey = new ECKeyGenerator(Curve.P_384).keyID("did:web:trudy#trudy-key").generate();
        when(didPkResolverMock.resolveKey(eq(trudyKey.getKeyID()))).thenReturn(Result.success(trudyKey.toPublicKey()));

        var tokenId = "test-token-id";
        var edr = tokenRefreshService.obtainToken(tokenParams(tokenId), DataAddress.Builder.newInstance().type("test-type").build(), Map.of(AUDIENCE_PROPERTY, "did:web:trudy"))
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

        // bob poses as trudy, using her key-ID and DID, but has to use his own private key
        var accessToken = edr.getToken();
        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES384).keyID(trudyKey.getKeyID()).build();
        var claimsSet = getAuthTokenClaims(tokenId, accessToken).issuer("did:web:trudy").subject("did:web:trudy").build();

        var signedAuthToken = new SignedJWT(jwsHeader, claimsSet);
        signedAuthToken.sign(CryptoConverter.createSigner(consumerKey));
        var tokenResponse = tokenRefreshService.refreshToken(edr.getAdditional().get(EDR_PROPERTY_REFRESH_TOKEN).toString(), signedAuthToken.serialize());

        assertThat(tokenResponse).isFailed().detail().isEqualTo("Authentication token validation failed: Token verification failed");
    }

    @DisplayName("Verify that an authentication token without an expiry is rejected")
    @Test
    void refresh_whenAuthTokenHasNoExpiry_shouldFail() throws JOSEException {
        var tokenId = "test-token-id";
        var edr = tokenRefreshService.obtainToken(tokenParams(tokenId), DataAddress.Builder.newInstance().type("test-type").build(), Map.of(AUDIENCE_PROPERTY, CONSUMER_DID))
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

        // without an expiry an intercepted refresh request could be replayed indefinitely
        var claimsSet = new JWTClaimsSet.Builder()
                .jwtID(tokenId)
                .issuer(CONSUMER_DID)
                .subject(CONSUMER_DID)
                .audience(PROVIDER_DID)
                .claim("token", edr.getToken())
                .build();

        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES384).keyID(consumerKey.getKeyID()).build();
        var signedAuthToken = new SignedJWT(jwsHeader, claimsSet);
        signedAuthToken.sign(CryptoConverter.createSigner(consumerKey));

        assertThat(tokenRefreshService.refreshToken(edr.getAdditional().get(EDR_PROPERTY_REFRESH_TOKEN).toString(), signedAuthToken.serialize()))
                .isFailed()
                .detail()
                .contains("Required expiration time (exp) claim is missing in token");
    }

    @DisplayName("Verify that an expired authentication token is rejected")
    @Test
    void refresh_whenAuthTokenExpired_shouldFail() throws JOSEException {
        var tokenId = "test-token-id";
        var edr = tokenRefreshService.obtainToken(tokenParams(tokenId), DataAddress.Builder.newInstance().type("test-type").build(), Map.of(AUDIENCE_PROPERTY, CONSUMER_DID))
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

        var authToken = createAuthToken(tokenId, edr.getToken());

        // the authentication token outlives its own validity, e.g. because it was captured and replayed later
        clock.advanceBy(Duration.ofSeconds(120));

        assertThat(tokenRefreshService.refreshToken(edr.getAdditional().get(EDR_PROPERTY_REFRESH_TOKEN).toString(), authToken))
                .isFailed()
                .detail()
                .contains("Token has expired (exp)");
    }

    @DisplayName("Verify that a refresh attempt fails if no \"token\" claim is present")
    @Test
    void refresh_whenNoAccessTokenClaim() throws JOSEException {
        var tokenId = "test-token-id";
        var edr = tokenRefreshService.obtainToken(tokenParams(tokenId), DataAddress.Builder.newInstance().type("test-type").build(), Map.of(AUDIENCE_PROPERTY, CONSUMER_DID))
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

        var accessToken = edr.getToken();
        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES384).keyID(consumerKey.getKeyID()).build();
        var claimsSet = getAuthTokenClaims(tokenId, accessToken).claim("token", null).build();

        var signedAuthToken = new SignedJWT(jwsHeader, claimsSet);
        signedAuthToken.sign(CryptoConverter.createSigner(consumerKey));
        var tokenResponse = tokenRefreshService.refreshToken(edr.getAdditional().get(EDR_PROPERTY_REFRESH_TOKEN).toString(), signedAuthToken.serialize());

        assertThat(tokenResponse).isFailed()
                .detail()
                .contains(" Required claim 'token' not present on token.");
    }

    @DisplayName("Verify that the equality of the 'iss' and the 'sub' claim of the authentication token")
    @Test
    void refresh_whenIssNotEqualToSub() throws JOSEException {
        var tokenId = "test-token-id";
        var edr = tokenRefreshService.obtainToken(tokenParams(tokenId), DataAddress.Builder.newInstance().type("test-type").build(), Map.of(AUDIENCE_PROPERTY, CONSUMER_DID))
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

        var accessToken = edr.getToken();
        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES384).keyID(consumerKey.getKeyID()).build();
        var claimsSet = getAuthTokenClaims(tokenId, accessToken)
                .issuer(CONSUMER_DID)
                .subject("violating-subject")
                .build();

        var signedAuthToken = new SignedJWT(jwsHeader, claimsSet);
        signedAuthToken.sign(CryptoConverter.createSigner(consumerKey));
        var tokenResponse = tokenRefreshService.refreshToken(edr.getAdditional().get(EDR_PROPERTY_REFRESH_TOKEN).toString(), signedAuthToken.serialize());

        assertThat(tokenResponse).isFailed()
                .detail()
                .isEqualTo("Authentication token validation failed: The 'iss' and 'sub' claims must be non-null and identical.");
    }

    @DisplayName("Verify that resolving an expired token fails")
    @Test
    void resolve_whenExpired_shouldFail() {
        var tokenId = "test-token-id";
        var edr = tokenRefreshService.obtainToken(tokenParamsBuilder(tokenId)
                                //token was issued 10min ago, and expired 5min ago
                                .claims(JwtRegisteredClaimNames.ISSUED_AT, Instant.now().minusSeconds(600).getEpochSecond())
                                .claims(JwtRegisteredClaimNames.EXPIRATION_TIME, Instant.now().minusSeconds(300).getEpochSecond())
                                .build(),
                        DataAddress.Builder.newInstance().type("test-type").build(), Map.of(AUDIENCE_PROPERTY, CONSUMER_DID))
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

        assertThat(tokenRefreshService.resolve(edr.getToken())).isFailed()
                .detail().isEqualTo("Token has expired (exp)");

    }

    @DisplayName("Verify that resolving a valid token succeeds")
    @Test
    void resolve_success() {
        var tokenId = "test-token-id";
        var edr = tokenRefreshService.obtainToken(tokenParamsBuilder(tokenId)
                                .claims(JwtRegisteredClaimNames.ISSUED_AT, Instant.now().getEpochSecond())
                                .build(),
                        DataAddress.Builder.newInstance().type("test-type").build(), Map.of(AUDIENCE_PROPERTY, CONSUMER_DID))
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

        assertThat(tokenRefreshService.resolve(edr.getToken())).isSucceeded();
    }

    @DisplayName("Verify that attempting to resolve a non-existing token results in a failure")
    @Test
    void resolve_notFound() {
        var tokenId = "test-token-id";
        var edr = tokenRefreshService.obtainToken(tokenParamsBuilder(tokenId)
                                .claims(JwtRegisteredClaimNames.ISSUED_AT, Instant.now().getEpochSecond())
                                .build(),
                        DataAddress.Builder.newInstance().type("test-type").build(), Map.of(AUDIENCE_PROPERTY, CONSUMER_DID))
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));
        tokenDataStore.deleteById(tokenId).orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        assertThat(tokenRefreshService.resolve(edr.getToken()))
                .isFailed()
                .detail().isEqualTo("AccessTokenData with ID '%s' does not exist.".formatted(tokenId));
    }

    @Test
    void revoke_successful() throws JsonProcessingException {
        var tokenId = "token-id";
        var transferProcessId = "dummy-tp-id";
        var accessTokenData = new AccessTokenData(tokenId, ClaimToken.Builder.newInstance().claim("claim1", "value1").build(),
                DataAddress.Builder.newInstance().type("type").build(), Map.of("process_id", transferProcessId));
        var refreshToken = new RefreshToken("dummy-token", 0L, "dummy-refresh-endpoint");

        tokenDataStore.store(accessTokenData);
        vault.storeSecret(tokenId, objectMapper.writeValueAsString(refreshToken));

        assertThat(tokenRefreshService.revoke(transferProcessId, "good-reason")).isSucceeded();
        assertThat(tokenDataStore.getById(tokenId)).isNull();
        assertThat(vault.resolveSecret(tokenId)).isNull();
    }

    private String createAuthToken(String tokenId, String accessToken) throws JOSEException {
        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES384).keyID(consumerKey.getKeyID()).build();
        var signedAuthToken = new SignedJWT(jwsHeader, getAuthTokenClaims(tokenId, accessToken).build());
        signedAuthToken.sign(CryptoConverter.createSigner(consumerKey));
        return signedAuthToken.serialize();
    }

    private JWTClaimsSet.Builder getAuthTokenClaims(String tokenId, String accessToken) {
        return new JWTClaimsSet.Builder()
                .jwtID(tokenId)
                .issuer(CONSUMER_DID)
                .subject(CONSUMER_DID)
                .audience(PROVIDER_DID)
                .issueTime(Date.from(clock.instant()))
                .expirationTime(Date.from(clock.instant().plusSeconds(60)))
                .claim("token", accessToken);
    }

    private TokenParameters tokenParams(String id) {
        return tokenParamsBuilder(id).build();
    }

    private TokenParameters.Builder tokenParamsBuilder(String id) {
        return TokenParameters.Builder.newInstance()
                .claims(JwtRegisteredClaimNames.JWT_ID, id)
                .claims(JwtRegisteredClaimNames.AUDIENCE, CONSUMER_BPN)
                .claims(JwtRegisteredClaimNames.ISSUER, PROVIDER_BPN)
                .claims(JwtRegisteredClaimNames.SUBJECT, PROVIDER_BPN)
                .claims(JwtRegisteredClaimNames.EXPIRATION_TIME, Instant.now().plusSeconds(60).getEpochSecond())
                .claims(JwtRegisteredClaimNames.ISSUED_AT, Instant.now().getEpochSecond())
                .header("kid", providerKey.getKeyID());

    }

    private Map<String, Object> asClaims(String serializedJwt) {
        try {
            var jwt = SignedJWT.parse(serializedJwt);
            return jwt.getJWTClaimsSet().getClaims();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Clock that only moves when the test tells it to, so that token expiry can be exercised without waiting.
     */
    private static class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advanceBy(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}