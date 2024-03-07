/*
 *
 *   Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
 *
 *   See the NOTICE file(s) distributed with this work for additional
 *   information regarding copyright ownership.
 *
 *   This program and the accompanying materials are made available under the
 *   terms of the Apache License, Version 2.0 which is available at
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *   License for the specific language governing permissions and limitations
 *   under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.tractusx.edc.dataplane.tokenrefresh.core;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.connector.core.store.CriterionOperatorRegistryImpl;
import org.eclipse.edc.connector.dataplane.framework.store.InMemoryAccessTokenDataStore;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.security.token.jwt.CryptoConverter;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.token.TokenValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.framework.iam.DataPlaneAuthorizationServiceImpl.CLAIM_AGREEMENT_ID;
import static org.eclipse.edc.connector.dataplane.framework.iam.DataPlaneAuthorizationServiceImpl.CLAIM_ASSET_ID;
import static org.eclipse.edc.connector.dataplane.framework.iam.DataPlaneAuthorizationServiceImpl.CLAIM_FLOW_TYPE;
import static org.eclipse.edc.connector.dataplane.framework.iam.DataPlaneAuthorizationServiceImpl.CLAIM_PROCESS_ID;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
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
    private DataPlaneTokenRefreshServiceImpl tokenRefreshService;
    private InMemoryAccessTokenDataStore tokenDataStore;
    private ECKey consumerKey;

    @BeforeEach
    void setup() throws JOSEException {

        var providerKey = new ECKeyGenerator(Curve.P_384).keyID(PROVIDER_BPN + "#provider-key").keyUse(KeyUse.SIGNATURE).generate();
        consumerKey = new ECKeyGenerator(Curve.P_384).keyID(CONSUMER_DID + "#consumer-key").keyUse(KeyUse.SIGNATURE).generate();

        var privateKey = providerKey.toPrivateKey();

        tokenDataStore = new InMemoryAccessTokenDataStore(CriterionOperatorRegistryImpl.ofDefaults());
        tokenRefreshService = new DataPlaneTokenRefreshServiceImpl(new TokenValidationServiceImpl(),
                didPkResolverMock,
                tokenDataStore,
                new JwtGenerationService(),
                () -> privateKey,
                mock(),
                TEST_REFRESH_ENDPOINT);

        when(didPkResolverMock.resolveKey(eq(consumerKey.getKeyID()))).thenReturn(Result.success(consumerKey.toPublicKey()));
        when(didPkResolverMock.resolveKey(eq(providerKey.getKeyID()))).thenReturn(Result.success(providerKey.toPublicKey()));
    }

    @DisplayName("Verify that a correct EDR is obtained")
    @Test
    void obtainToken() {
        var tokenId = "test-token-id";
        var edr = tokenRefreshService.obtainToken(tokenParams(tokenId), DataAddress.Builder.newInstance().type("test-type").build(), Map.of("audience", CONSUMER_DID));
        assertThat(edr).isSucceeded();
        // assert access token contents
        assertThat(asClaims(edr.getContent().getToken()))
                .containsKey("asset_id")
                .containsKey("process_id")
                .containsKey("agreement_id")
                .containsEntry("iss", PROVIDER_BPN)
                .containsEntry("sub", PROVIDER_BPN)
                .containsEntry("aud", List.of(CONSUMER_BPN));

        // assert additional properties -> refresh token
        assertThat(edr.getContent().getAdditional())
                .containsEntry("refreshEndpoint", TEST_REFRESH_ENDPOINT)
                .containsKey("refreshToken")
                .containsKey("expiresIn");

        // verify that the correct data was stored
        var storedData = tokenDataStore.getById(tokenId);
        assertThat(storedData).isNotNull();
        assertThat(storedData.additionalProperties())
                .containsEntry("audience", CONSUMER_DID)
                .containsEntry("refreshEndpoint", TEST_REFRESH_ENDPOINT)
                .containsKey("refreshToken")
                .containsKey("expiresIn")
                .containsEntry("authType", "bearer");

    }

    @DisplayName("Verify that a token can be refreshed")
    @Test
    void refresh_success() throws JOSEException {

        var tokenId = "test-token-id";
        var edr = tokenRefreshService.obtainToken(tokenParams(tokenId), DataAddress.Builder.newInstance().type("test-type").build(), Map.of("audience", CONSUMER_DID))
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

        var accessToken = edr.getToken();
        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES384).keyID(consumerKey.getKeyID()).build();
        var claimsSet = getAuthTokenClaims(tokenId, accessToken).build();

        var signedAuthToken = new SignedJWT(jwsHeader, claimsSet);
        signedAuthToken.sign(CryptoConverter.createSigner(consumerKey));
        var tokenResponse = tokenRefreshService.refreshToken(edr.getAdditional().get("refreshToken").toString(), signedAuthToken.serialize());

        assertThat(tokenResponse).withFailMessage(tokenResponse::getFailureDetail).isSucceeded()
                .satisfies(tr -> assertThat(tr.refreshToken()).isNotNull())
                .satisfies(tr -> assertThat(tr.accessToken()).isNotNull());
    }

    @DisplayName("Verify that a refresh token can only be refreshed by the original recipient")
    @Test
    void refresh_originalTokenWasIssuedToDifferentPrincipal() throws JOSEException {
        var trudyKey = new ECKeyGenerator(Curve.P_256).keyID("did:web:trudy#trudy-key").generate();
        when(didPkResolverMock.resolveKey(eq("did:web:trudy#trudy-key"))).thenReturn(Result.success(trudyKey.toPublicKey()));


        var tokenId = "test-token-id";
        var edr = tokenRefreshService.obtainToken(tokenParams(tokenId), DataAddress.Builder.newInstance().type("test-type").build(), Map.of("audience", "did:web:trudy"))
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

        // bob attempts to create an auth token with an EDR he stole from trudy
        var accessToken = edr.getToken();
        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES384).keyID(consumerKey.getKeyID()).build();
        var claimsSet = getAuthTokenClaims(tokenId, accessToken).build();

        var signedAuthToken = new SignedJWT(jwsHeader, claimsSet);
        signedAuthToken.sign(CryptoConverter.createSigner(consumerKey));
        var tokenResponse = tokenRefreshService.refreshToken(edr.getAdditional().get("refreshToken").toString(), signedAuthToken.serialize());

        assertThat(tokenResponse).isFailed().detail().isEqualTo("Principal 'did:web:bob' is not authorized to refresh this token.");
    }

    @DisplayName("Verify that a spoofed refresh attempt is rejected ")
    @Test
    void refresh_issuerNotVerifiable() throws JOSEException {
        var trudyKey = new ECKeyGenerator(Curve.P_384).keyID("did:web:trudy#trudy-key").generate();
        when(didPkResolverMock.resolveKey(eq(trudyKey.getKeyID()))).thenReturn(Result.success(trudyKey.toPublicKey()));

        var tokenId = "test-token-id";
        var edr = tokenRefreshService.obtainToken(tokenParams(tokenId), DataAddress.Builder.newInstance().type("test-type").build(), Map.of("audience", "did:web:trudy"))
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

        // bob poses as trudy, using her key-ID and DID, but has to use his own private key
        var accessToken = edr.getToken();
        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES384).keyID(trudyKey.getKeyID()).build();
        var claimsSet = getAuthTokenClaims(tokenId, accessToken).issuer("did:web:trudy").subject("did:web:trudy").build();

        var signedAuthToken = new SignedJWT(jwsHeader, claimsSet);
        signedAuthToken.sign(CryptoConverter.createSigner(consumerKey));
        var tokenResponse = tokenRefreshService.refreshToken(edr.getAdditional().get("refreshToken").toString(), signedAuthToken.serialize());

        assertThat(tokenResponse).isFailed().detail().isEqualTo("Token verification failed");
    }

    @DisplayName("Verify that a refresh attempt fails if no \"access_token\" claim is present")
    @Test
    void refresh_whenNoAccessTokenClaim() throws JOSEException {
        var tokenId = "test-token-id";
        var edr = tokenRefreshService.obtainToken(tokenParams(tokenId), DataAddress.Builder.newInstance().type("test-type").build(), Map.of("audience", CONSUMER_DID))
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

        var accessToken = edr.getToken();
        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES384).keyID(consumerKey.getKeyID()).build();
        var claimsSet = getAuthTokenClaims(tokenId, accessToken).claim("access_token", null).build();

        var signedAuthToken = new SignedJWT(jwsHeader, claimsSet);
        signedAuthToken.sign(CryptoConverter.createSigner(consumerKey));
        var tokenResponse = tokenRefreshService.refreshToken(edr.getAdditional().get("refreshToken").toString(), signedAuthToken.serialize());

        assertThat(tokenResponse).isFailed()
                .detail()
                .isEqualTo("Required claim 'access_token' not present on token.");
    }

    private JWTClaimsSet.Builder getAuthTokenClaims(String tokenId, String accessToken) {
        return new JWTClaimsSet.Builder()
                .jwtID(tokenId)
                .issuer(CONSUMER_DID)
                .subject(CONSUMER_DID)
                .audience(PROVIDER_DID)
                .claim("access_token", accessToken);
    }

    private TokenParameters tokenParams(String id) {
        return TokenParameters.Builder.newInstance()
                .claims(JwtRegisteredClaimNames.JWT_ID, id)
                .claims(JwtRegisteredClaimNames.AUDIENCE, CONSUMER_BPN)
                .claims(JwtRegisteredClaimNames.ISSUER, PROVIDER_BPN)
                .claims(JwtRegisteredClaimNames.SUBJECT, PROVIDER_BPN)
                .claims(JwtRegisteredClaimNames.ISSUED_AT, Instant.now().toEpochMilli()) // todo: milli or second?
                .claims(CLAIM_AGREEMENT_ID, "test-agreement-id")
                .claims(CLAIM_ASSET_ID, "test-asset-id")
                .claims(CLAIM_PROCESS_ID, "test-process-id")
                .claims(CLAIM_FLOW_TYPE, FlowType.PULL.toString())
                .build();
    }

    private Map<String, Object> asClaims(String serializedJwt) {
        try {
            var jwt = SignedJWT.parse(serializedJwt);
            return jwt.getJWTClaimsSet().getClaims();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}