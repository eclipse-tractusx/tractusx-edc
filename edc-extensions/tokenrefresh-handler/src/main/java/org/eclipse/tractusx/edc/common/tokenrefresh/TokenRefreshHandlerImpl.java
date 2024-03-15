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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.identitytrust.SecureTokenService;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.util.string.StringUtils;
import org.eclipse.tractusx.edc.spi.tokenrefresh.common.TokenRefreshHandler;
import org.eclipse.tractusx.edc.spi.tokenrefresh.dataplane.model.TokenResponse;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.JWT_ID;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;
import static org.eclipse.edc.util.string.StringUtils.isNullOrBlank;

public class TokenRefreshHandlerImpl implements TokenRefreshHandler {
    public static final String PROPERTY_AUTHORIZATION = "authorization";
    public static final String PROPERTY_REFRESH_TOKEN = "refreshToken";
    public static final String PROPERTY_REFRESH_ENDPOINT = "refreshEndpoint";
    private final EndpointDataReferenceStore edrStore;
    private final EdcHttpClient httpClient;
    private final String ownDid;
    private final Monitor monitor;
    private final SecureTokenService secureTokenService;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new TokenRefreshHandler
     *
     * @param edrStore           a persistent storage where {@link org.eclipse.edc.spi.types.domain.edr.EndpointDataReference} objects are stored.
     * @param httpClient         needed to make the actual refresh call against the refresh endpoint
     * @param ownDid             the DID of this connector
     * @param secureTokenService Service to generate the authentication token
     * @param objectMapper       ObjectMapper to interpret JSON responses
     */
    public TokenRefreshHandlerImpl(EndpointDataReferenceStore edrStore,
                                   EdcHttpClient httpClient,
                                   String ownDid,
                                   Monitor monitor,
                                   SecureTokenService secureTokenService,
                                   ObjectMapper objectMapper) {
        this.edrStore = edrStore;
        this.httpClient = httpClient;
        this.ownDid = ownDid;
        this.monitor = monitor;
        this.secureTokenService = secureTokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Result<TokenResponse> refreshToken(String tokenId) {
        var edrResult = edrStore.resolveByTransferProcess(tokenId);
        if (edrResult.failed()) {
            return Result.failure(edrResult.getFailureDetail());
        }
        var edr = edrResult.getContent();
        var accessToken = edr.getStringProperty(PROPERTY_AUTHORIZATION);
        var refreshToken = edr.getProperties().get(PROPERTY_REFRESH_TOKEN);
        var refreshEndpoint = edr.getProperties().get(PROPERTY_REFRESH_ENDPOINT);

        if (isNullOrBlank(accessToken)) {
            return Result.failure("Cannot perform token refresh: required property 'authorization' not found on EDR.");
        }
        if (isNullOrBlank(StringUtils.toString(refreshToken))) {
            return Result.failure("Cannot perform token refresh: required property 'refreshToken' not found on EDR.");
        }
        if (isNullOrBlank(StringUtils.toString(refreshEndpoint))) {
            return Result.failure("Cannot perform token refresh: required property 'refreshEndpoint' not found on EDR.");
        }

        return getStringClaim(accessToken, ISSUER)
                .map(audience -> Map.of(
                        JWT_ID, tokenId,
                        ISSUER, ownDid,
                        SUBJECT, ownDid,
                        AUDIENCE, audience
                ))
                .compose(claims -> secureTokenService.createToken(claims, accessToken))
                .compose(authToken -> createTokenRefreshRequest(refreshEndpoint.toString(), refreshToken.toString(), "Bearer %s".formatted(authToken.getToken())))
                .compose(this::executeRequest);
    }

    private Result<TokenResponse> executeRequest(Request tokenRefreshRequest) {
        try (var response = httpClient.execute(tokenRefreshRequest)) {
            if (response.isSuccessful()) {
                if (response.body() != null) {

                    var jsonBody = response.body().string();
                    if (!StringUtils.isNullOrEmpty(jsonBody)) {
                        var tokenResponse = objectMapper.readValue(jsonBody, TokenResponse.class);
                        return Result.success(tokenResponse);
                    }
                }
                return Result.failure("Token refresh successful, but body was empty.");
            }
            return Result.failure("Token refresh not successful: %d, message: %s".formatted(response.code(), response.message()));
        } catch (IOException e) {
            monitor.warning("Error executing token refresh request", e);
            return Result.failure("Error executing token refresh request: %s".formatted(e));
        }
    }

    private Result<Request> createTokenRefreshRequest(String refreshEndpoint, String refreshToken, String bearerToken) {
        // see https://github.com/eclipse-tractusx/tractusx-profiles/blob/main/tx/refresh/refresh.token.grant.profile.md#3-the-refresh-request
        if (!refreshEndpoint.endsWith("/token")) {
            refreshToken += "/token";
        }
        var url = HttpUrl.parse(refreshEndpoint)
                .newBuilder()
                .addQueryParameter("grant_type", "refresh_token")
                .addQueryParameter("refresh_token", refreshToken)
                .build();

        return Result.success(new Request.Builder()
                .addHeader("Authorization", bearerToken)
                .url(url)
                .post(RequestBody.create(new byte[0]))
                .build());
    }

    private Result<String> getStringClaim(String accessToken, String claimName) {
        try {
            return Result.success(SignedJWT.parse(accessToken).getJWTClaimsSet().getStringClaim(claimName));
        } catch (ParseException e) {
            monitor.warning("Failed to get string claim '%s'".formatted(claimName), e);
            return Result.failure("Failed to parse string claim '%s': %s".formatted(claimName, e));
        }
    }

}
