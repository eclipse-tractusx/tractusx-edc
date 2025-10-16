/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
 * Copyright (c) 2025 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.dataplane.tokenrefresh.e2e;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.edr.EndpointDataReferenceServiceRegistry;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.eclipse.tractusx.edc.spi.tokenrefresh.dataplane.model.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;

import java.net.URI;
import java.text.ParseException;
import java.util.Map;

import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.AUDIENCE_PROPERTY;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_AUTH_NS;
import static org.hamcrest.Matchers.containsString;


@EndToEndTest
public class DataPlaneTokenRefreshEndToEndTest {

    public static final RuntimeConfig RUNTIME_CONFIG = new RuntimeConfig();
    public static final String CONSUMER_DID = "did:web:alice";
    public static final String PROVIDER_DID = "did:web:bob";
    public static final String PROVIDER_KEY_ID = PROVIDER_DID + "#key-1";
    public static final String PROVIDER_KEY_ID_PUBLIC = PROVIDER_DID + "#key-1-public";

    @RegisterExtension
    private static final RuntimeExtension DATAPLANE_RUNTIME = new RuntimePerMethodExtension(
            new EmbeddedRuntime("Token-Refresh-Dataplane", ":edc-tests:runtime:dataplane-cloud")
                    .configurationProvider(RUNTIME_CONFIG::getConfig)
                    .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                            "edc.transfer.proxy.token.signer.privatekey.alias", PROVIDER_KEY_ID,
                            "edc.transfer.proxy.token.verifier.publickey.alias", PROVIDER_KEY_ID_PUBLIC
                    )))
    );

    public static final String CONSUMER_KEY_ID = CONSUMER_DID + "#cons-1";
    private ECKey providerKey;
    private ECKey consumerKey;

    @BeforeEach
    void setup() throws JOSEException {
        providerKey = new ECKeyGenerator(Curve.P_384).keyID(PROVIDER_KEY_ID).generate();
        consumerKey = new ECKeyGenerator(Curve.P_256).keyID(CONSUMER_KEY_ID).generate();

        // mock the did resolver, hard-wire it to the provider or consumer DID
        DATAPLANE_RUNTIME.registerServiceMock(DidPublicKeyResolver.class, s -> {
            try {
                if (s.startsWith(CONSUMER_DID)) {
                    return Result.success(consumerKey.toPublicKey());
                } else if (s.startsWith(PROVIDER_DID)) {
                    return Result.success(providerKey.toPublicKey());
                }
                throw new IllegalArgumentException("DID '%s' could not be resolved.".formatted(s));
            } catch (JOSEException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @DisplayName("Refresh token success")
    @Test
    void refresh_success() {

        // register generator and secrets
        prepareDataplaneRuntime();

        var edrService = DATAPLANE_RUNTIME.getService(EndpointDataReferenceServiceRegistry.class);
        var dataFlow = createDataFlow("test-process-id", CONSUMER_DID);
        var edr = edrService.create(dataFlow, dataFlow.getSource())
                .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        var refreshToken = edr.getStringProperty(TX_AUTH_NS + "refreshToken");
        var accessToken = edr.getStringProperty(EDC_NAMESPACE + "authorization");
        var authToken = createAuthToken(accessToken, consumerKey);

        var tokenResponse = RUNTIME_CONFIG.basePublicApiRequest()
                .queryParam("grant_type", "refresh_token")
                .queryParam("refresh_token", refreshToken)
                .header(AUTHORIZATION, "Bearer " + authToken)
                .post("/token")
                .then()
                .log().ifError()
                .statusCode(200)
                .extract().body().as(TokenResponse.class);

        assertThat(tokenResponse).isNotNull();
    }

    @DisplayName("Refresh token is null or empty (missing)")
    @ParameterizedTest
    @NullSource
    @EmptySource
    void refresh_invalidRefreshToken(String invalidRefreshToken) {
        // register generator and secrets
        prepareDataplaneRuntime();

        var edrService = DATAPLANE_RUNTIME.getService(EndpointDataReferenceServiceRegistry.class);
        var dataFlow = createDataFlow("test-process-id", CONSUMER_DID);
        var edr = edrService.create(dataFlow, dataFlow.getSource())
                .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        var refreshToken = edr.getStringProperty(TX_AUTH_NS + "refreshToken");
        var accessToken = edr.getStringProperty(EDC_NAMESPACE + "authorization");
        var authToken = createAuthToken(accessToken, consumerKey);

        RUNTIME_CONFIG.basePublicApiRequest()
                .queryParam("grant_type", "refresh_token")
                .queryParam("refresh_token", invalidRefreshToken)
                .header(AUTHORIZATION, "Bearer " + authToken)
                .post("/token")
                .then()
                .log().ifError()
                .statusCode(400);
    }

    @DisplayName("The Authorization header is empty")
    @Test
    void refresh_emptyAuthHeader() {
        // register generator and secrets
        prepareDataplaneRuntime();

        var edrService = DATAPLANE_RUNTIME.getService(EndpointDataReferenceServiceRegistry.class);
        var dataFlow = createDataFlow("test-process-id", CONSUMER_DID);
        var edr = edrService.create(dataFlow, dataFlow.getSource())
                .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        var refreshToken = edr.getStringProperty(TX_AUTH_NS + "refreshToken");
        var accessToken = edr.getStringProperty(EDC_NAMESPACE + "authorization");

        // auth header is empty
        RUNTIME_CONFIG.basePublicApiRequest()
                .queryParam("grant_type", "refresh_token")
                .queryParam("refresh_token", refreshToken)
                .header(AUTHORIZATION, "")
                .post("/token")
                .then()
                .log().ifError()
                .statusCode(401);
    }

    @DisplayName("The Authorization header is missing")
    @Test
    void refresh_missingAuthHeader() {
        // register generator and secrets
        prepareDataplaneRuntime();

        var edrService = DATAPLANE_RUNTIME.getService(EndpointDataReferenceServiceRegistry.class);
        var dataFlow = createDataFlow("test-process-id", CONSUMER_DID);
        var edr = edrService.create(dataFlow, dataFlow.getSource())
                .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        var refreshToken = edr.getStringProperty(TX_AUTH_NS + "refreshToken");
        var accessToken = edr.getStringProperty(EDC_NAMESPACE + "authorization");

        // auth header is empty
        RUNTIME_CONFIG.basePublicApiRequest()
                .queryParam("grant_type", "refresh_token")
                .queryParam("refresh_token", refreshToken)
                .post("/token")
                .then()
                .log().ifError()
                .statusCode(401);
    }

    @DisplayName("The sign key of the authentication token is different from the public key from the DID")
    @Test
    void refresh_spoofedAuthToken() throws JOSEException {
        prepareDataplaneRuntime();

        var edrService = DATAPLANE_RUNTIME.getService(EndpointDataReferenceServiceRegistry.class);
        var dataFlow = createDataFlow("test-process-id", CONSUMER_DID);
        var edr = edrService.create(dataFlow, dataFlow.getSource())
                .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        var refreshToken = edr.getStringProperty(TX_AUTH_NS + "refreshToken");
        var accessToken = edr.getStringProperty(EDC_NAMESPACE + "authorization");
        var spoofedKey = new ECKeyGenerator(Curve.P_256).keyID(CONSUMER_KEY_ID).generate();
        var authTokenWithSpoofedKey = createAuthToken(accessToken, spoofedKey);

        RUNTIME_CONFIG.basePublicApiRequest()
                .queryParam("grant_type", "refresh_token")
                .queryParam("refresh_token", refreshToken)
                .header(AUTHORIZATION, "Bearer " + authTokenWithSpoofedKey)
                .post("/token")
                .then()
                .log().ifValidationFails()
                .statusCode(401)
                .body(containsString("Token verification failed"));
    }

    @DisplayName("The refresh token does not match the stored one")
    @Test
    void refresh_withWrongRefreshToken() {
        prepareDataplaneRuntime();

        var edrService = DATAPLANE_RUNTIME.getService(EndpointDataReferenceServiceRegistry.class);
        var dataFlow = createDataFlow("test-process-id", CONSUMER_DID);
        var edr = edrService.create(dataFlow, dataFlow.getSource())
                .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        var refreshToken = "invalid_refresh_token";
        var accessToken = edr.getStringProperty(EDC_NAMESPACE + "authorization");

        RUNTIME_CONFIG.basePublicApiRequest()
                .queryParam("grant_type", "refresh_token")
                .queryParam("refresh_token", refreshToken)
                .header(AUTHORIZATION, "Bearer " + createAuthToken(accessToken, consumerKey))
                .post("/token")
                .then()
                .log().ifValidationFails()
                .statusCode(401)
                .body(containsString("Provided refresh token does not match the stored refresh token."));
    }


    @DisplayName("The authentication token misses required claims: token")
    @Test
    void refresh_invalidAuthenticationToken_missingAccessToken() {
        prepareDataplaneRuntime();

        var edrService = DATAPLANE_RUNTIME.getService(EndpointDataReferenceServiceRegistry.class);
        var dataFlow = createDataFlow("test-process-id", CONSUMER_DID);
        var edr = edrService.create(dataFlow, dataFlow.getSource())
                .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        var refreshToken = edr.getStringProperty(TX_AUTH_NS + "refreshToken");
        var accessToken = edr.getStringProperty(EDC_NAMESPACE + "authorization");

        var claims = new JWTClaimsSet.Builder()
                /* missing: .claim("token", accessToken)*/
                .issuer(CONSUMER_DID)
                .subject(CONSUMER_DID)
                .audience("did:web:bob")
                .jwtID(getJwtId(accessToken))
                .build();
        var authToken = createJwt(consumerKey, claims);

        RUNTIME_CONFIG.basePublicApiRequest()
                .queryParam("grant_type", "refresh_token")
                .queryParam("refresh_token", refreshToken)
                .header(AUTHORIZATION, "Bearer " + authToken)
                .post("/token")
                .then()
                .log().ifValidationFails()
                .statusCode(401)
                .body(containsString("Required claim 'token' not present on token."));
    }

    @DisplayName("The authentication token misses required claims: audience")
    @Test
    void refresh_invalidAuthenticationToken_missingAudience() {
        prepareDataplaneRuntime();

        var edrService = DATAPLANE_RUNTIME.getService(EndpointDataReferenceServiceRegistry.class);
        var dataFlow = createDataFlow("test-process-id", CONSUMER_DID);
        var edr = edrService.create(dataFlow, dataFlow.getSource())
                .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        var refreshToken = edr.getStringProperty(TX_AUTH_NS + "refreshToken");
        var accessToken = edr.getStringProperty(EDC_NAMESPACE + "authorization");

        var claims = new JWTClaimsSet.Builder()
                .claim("token", accessToken)
                .issuer(CONSUMER_DID)
                .subject(CONSUMER_DID)
                /* missing: .audience("did:web:bob")*/
                .jwtID(getJwtId(accessToken))
                .build();
        var authToken = createJwt(consumerKey, claims);

        RUNTIME_CONFIG.basePublicApiRequest()
                .queryParam("grant_type", "refresh_token")
                .queryParam("refresh_token", refreshToken)
                .header(AUTHORIZATION, "Bearer " + authToken)
                .post("/token")
                .then()
                .log().ifValidationFails()
                .statusCode(401)
                .body(containsString("Required claim 'aud' not present on token."));
    }

    @DisplayName("The authentication token has a invalid id")
    @Test
    void refresh_invalidTokenId() {
        prepareDataplaneRuntime();

        var edrService = DATAPLANE_RUNTIME.getService(EndpointDataReferenceServiceRegistry.class);
        var dataFlow = createDataFlow("test-process-id", CONSUMER_DID);
        var edr = edrService.create(dataFlow, dataFlow.getSource())
                .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        var refreshToken = edr.getStringProperty(TX_AUTH_NS + "refreshToken");
        var accessToken = edr.getStringProperty(EDC_NAMESPACE + "authorization");


        edrService.revoke(dataFlow, "Revoked");
        var tokenId = getJwtId(accessToken);

        var claims = new JWTClaimsSet.Builder()
                .claim("token", accessToken)
                .issuer(CONSUMER_DID)
                .subject(CONSUMER_DID)
                .audience("did:web:bob")
                .jwtID(tokenId)
                .build();

        var authToken = createJwt(consumerKey, claims);

        RUNTIME_CONFIG.basePublicApiRequest()
                .queryParam("grant_type", "refresh_token")
                .queryParam("refresh_token", refreshToken)
                .header(AUTHORIZATION, "Bearer " + authToken)
                .post("/token")
                .then()
                .log().ifValidationFails()
                .statusCode(401)
                .body(containsString("Authentication token validation failed: Token with id '%s' not found".formatted(tokenId)));
    }

    private void prepareDataplaneRuntime() {
        var vault = DATAPLANE_RUNTIME.getService(Vault.class);
        vault.storeSecret(PROVIDER_KEY_ID, providerKey.toJSONString());
        vault.storeSecret(PROVIDER_KEY_ID_PUBLIC, providerKey.toPublicJWK().toJSONString());
    }

    private String createAuthToken(String accessToken, ECKey signerKey) {
        var claims = new JWTClaimsSet.Builder()
                .claim("token", accessToken)
                .issuer(CONSUMER_DID)
                .subject(CONSUMER_DID)
                .audience("did:web:bob")
                .jwtID(getJwtId(accessToken))
                .build();
        return createJwt(signerKey, claims);
    }

    private String createJwt(ECKey signerKey, JWTClaimsSet claims) {
        var header = new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(consumerKey.getKeyID()).build();
        var jwt = new SignedJWT(header, claims);
        try {
            jwt.sign(new ECDSASigner(signerKey));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    private String getJwtId(String accessToken) {
        try {
            return SignedJWT.parse(accessToken).getJWTClaimsSet().getJWTID();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private DataFlow createDataFlow(String processId, String audience) {
        return DataFlow.Builder.newInstance()
                .id(processId)
                .source(DataAddress.Builder.newInstance().type("HttpData").property(EDC_NAMESPACE + "baseUrl", "http://foo.bar/").build())
                .destination(DataAddress.Builder.newInstance().type("HttpData").property(EDC_NAMESPACE + "baseUrl", "http://fizz.buzz").build())
                .transferType(new TransferType("HttpData", FlowType.PULL))
                .participantId("some-participantId")
                .assetId("test-asset")
                .callbackAddress(URI.create("https://foo.bar/callback"))
                .agreementId("test-agreement")
                .properties(Map.of(AUDIENCE_PROPERTY, audience))
                .build();
    }
}
