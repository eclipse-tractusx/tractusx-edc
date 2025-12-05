/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
 ********************************************************************************/

package org.eclipse.tractusx.edc.common.tokenrefresh;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.assertj.core.api.Assertions;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceCache;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.tractusx.edc.spi.tokenrefresh.dataplane.model.TokenResponse;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.util.stream.Stream;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_AUTHORIZATION;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_EXPIRES_IN;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_REFRESH_AUDIENCE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_REFRESH_ENDPOINT;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_REFRESH_TOKEN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


class TokenRefreshHandlerImplTest {
    public static final String REFRESH_ENDPOINT = "http://fizz.buzz/quazz";
    private static final String CONSUMER_DID = "did:web:bob";
    private static final String PROVIDER_DID = "did:web:alice";
    private final EndpointDataReferenceCache edrCache = mock();
    private final EdcHttpClient mockedHttpClient = mock();
    private final SecureTokenService mockedTokenService = mock();
    private final ParticipantContextSupplier participantContextSupplier = mock();
    private TokenRefreshHandlerImpl tokenRefreshHandler;
    private ObjectMapper objectMapper;

    private static String createJwt() {
        try {
            var providerKey = new ECKeyGenerator(Curve.P_256).generate();
            var signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.ES256), new JWTClaimsSet.Builder().issuer(PROVIDER_DID).build());
            signedJwt.sign(new ECDSASigner(providerKey));
            return signedJwt.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setup() {
        var participantContext = ParticipantContext.Builder.newInstance().participantContextId("participantContextId").identity("identity").build();
        when(participantContextSupplier.get()).thenReturn(ServiceResult.success(participantContext));
        objectMapper = new ObjectMapper();
        tokenRefreshHandler = new TokenRefreshHandlerImpl(edrCache, mockedHttpClient, CONSUMER_DID, mock(),
                mockedTokenService, objectMapper, participantContextSupplier);
    }

    @Test
    void refresh_validateCorrectRequest() throws IOException {
        when(edrCache.get(anyString())).thenReturn(StoreResult.success(createEdr().build()));
        when(mockedTokenService.createToken(any(), anyMap(), isNull())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("foo-auth-token").build()));
        var tokenResponse = new TokenResponse("new-access-token", "new-refresh-token", 60 * 5L, "bearer");
        var successResponse = createResponse(tokenResponse, 200, "");
        when(mockedHttpClient.execute(any())).thenReturn(successResponse);
        var res = tokenRefreshHandler.refreshToken("token-id");
        assertThat(res).isSucceeded()
                .satisfies(tr -> {
                    Assertions.assertThat(tr.getProperties()).containsEntry(EDR_PROPERTY_AUTHORIZATION, "new-access-token");
                    Assertions.assertThat(tr.getProperties()).containsEntry(EDR_PROPERTY_EXPIRES_IN, "300");
                    Assertions.assertThat(tr.getProperties()).containsEntry(EDR_PROPERTY_REFRESH_TOKEN, "new-refresh-token");
                    Assertions.assertThat(tr.getProperties()).containsEntry(EDR_PROPERTY_REFRESH_ENDPOINT, REFRESH_ENDPOINT);
                });
        verify(mockedHttpClient).execute(argThat(r -> {
            var hdr = r.header("Content-Type");
            return hdr != null && hdr.equalsIgnoreCase("application/x-www-form-urlencoded");
        }));
    }

    @Test
    void refresh_edrNotFound() {
        when(edrCache.get(anyString())).thenReturn(StoreResult.notFound("foo"));

        assertThat(tokenRefreshHandler.refreshToken("token-id")).isFailed()
                .detail().isEqualTo("foo");

        verify(edrCache).get(eq("token-id"));
        verifyNoMoreInteractions(edrCache);
        verifyNoInteractions(mockedHttpClient, mockedTokenService);
    }

    @ParameterizedTest(name = "{3}")
    @ArgumentsSource(InvalidEdrProvider.class)
    void refresh_edrLacksRequiredProperties(String authorization, String refreshToken, String refreshEndpoint, String desc) {
        var invalidEdr = DataAddress.Builder.newInstance().type("test-type")
                .property(EDR_PROPERTY_AUTHORIZATION, authorization)
                .property(EDR_PROPERTY_REFRESH_TOKEN, refreshToken)
                .property(EDR_PROPERTY_REFRESH_ENDPOINT, refreshEndpoint)
                .build();
        when(edrCache.get(anyString())).thenReturn(StoreResult.success(invalidEdr));

        assertThat(tokenRefreshHandler.refreshToken("token-id")).isFailed()
                .detail()
                .matches("^Cannot perform token refresh: required property '(authorization|refreshToken|refreshEndpoint)' not found on EDR.$");
    }

    @Test
    void refresh_endpointReturnsFailure() throws IOException {
        when(edrCache.get(anyString())).thenReturn(StoreResult.success(createEdr().build()));
        when(mockedTokenService.createToken(any(), anyMap(), isNull())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("foo-auth-token").build()));
        var response401 = createResponse(null, 401, "Not authorized");

        when(mockedHttpClient.execute(any())).thenReturn(response401);

        var res = tokenRefreshHandler.refreshToken("token-id");
        assertThat(res).isFailed()
                .detail().isEqualTo("Not authorized");
    }

    @Test
    void refresh_endpointReturnsEmptyBody() throws IOException {
        when(edrCache.get(anyString())).thenReturn(StoreResult.success(createEdr().build()));
        when(mockedTokenService.createToken(any(), anyMap(), isNull())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("foo-auth-token").build()));
        var successResponse = createResponse(null, 200, "");
        when(mockedHttpClient.execute(any())).thenReturn(successResponse);

        var res = tokenRefreshHandler.refreshToken("token-id");
        assertThat(res).isFailed()
                .detail().isEqualTo("Token refresh successful, but body was empty.");
    }

    @Test
    void refresh_ioException() throws IOException {
        when(edrCache.get(anyString())).thenReturn(StoreResult.success(createEdr().build()));
        when(mockedTokenService.createToken(any(), anyMap(), isNull())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("foo-auth-token").build()));
        when(mockedHttpClient.execute(any())).thenThrow(new IOException("test exception"));

        assertThat(tokenRefreshHandler.refreshToken("token-id")).isFailed()
                .detail().isEqualTo("Error executing token refresh request: java.io.IOException: test exception");
    }

    @Test
    void refresh_tokenGenerationFailed() {
        when(edrCache.get(anyString())).thenReturn(StoreResult.success(createEdr().build()));
        when(mockedTokenService.createToken(any(), anyMap(), isNull())).thenReturn(Result.failure("foobar"));
        assertThat(tokenRefreshHandler.refreshToken("token-id")).isFailed()
                .detail().isEqualTo("Could not execute token refresh: foobar");
    }

    @NotNull
    private Response createResponse(Object responseBodyObject, int statusCode, String message) throws JsonProcessingException {
        var body = responseBodyObject == null ? new byte[0] : objectMapper.writeValueAsBytes(responseBodyObject);
        return new Response.Builder()
                .code(statusCode)
                .protocol(Protocol.HTTP_1_1)
                .message(message)
                .request(new Request.Builder().url(REFRESH_ENDPOINT).build())
                .body(ResponseBody.create(body, MediaType.parse("application/json")))
                .build();
    }

    private DataAddress.Builder createEdr() {
        return DataAddress.Builder.newInstance()
                .type("HttpData")
                .property(EDR_PROPERTY_AUTHORIZATION, createJwt())
                .property(EDR_PROPERTY_REFRESH_TOKEN, "foo-refresh-token")
                .property(EDR_PROPERTY_REFRESH_ENDPOINT, REFRESH_ENDPOINT)
                .property(EDR_PROPERTY_REFRESH_AUDIENCE, CONSUMER_DID);
    }

    private static class InvalidEdrProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    Arguments.of(createJwt(), "foo-refresh-token", null, "refresh endpoint is null"),
                    Arguments.of(createJwt(), "foo-refresh-token", "", "refresh endpoint is empty"),
                    Arguments.of(createJwt(), "foo-refresh-token", "   ", "refresh endpoint is blank"),
                    Arguments.of(createJwt(), null, REFRESH_ENDPOINT, "refresh token is null"),
                    Arguments.of(createJwt(), "", REFRESH_ENDPOINT, "refresh token is empty"),
                    Arguments.of(createJwt(), "   ", REFRESH_ENDPOINT, "refresh token is blank"),
                    Arguments.of(null, "foo-refresh-token", REFRESH_ENDPOINT, "access token is null")
            );
        }
    }
}
