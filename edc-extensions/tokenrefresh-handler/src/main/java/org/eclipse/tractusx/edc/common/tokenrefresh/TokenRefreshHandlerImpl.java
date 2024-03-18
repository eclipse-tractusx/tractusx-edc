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
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceCache;
import org.eclipse.edc.identitytrust.SecureTokenService;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
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
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.result.Result.success;
import static org.eclipse.edc.util.string.StringUtils.isNullOrBlank;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_AUTH_NS;

public class TokenRefreshHandlerImpl implements TokenRefreshHandler {
    public static final String PROPERTY_AUTHORIZATION = EDC_NAMESPACE + "authorization";
    public static final String PROPERTY_REFRESH_TOKEN = TX_AUTH_NS + "refreshToken";
    public static final String PROPERTY_REFRESH_ENDPOINT = TX_AUTH_NS + "refreshEndpoint";
    public static final String PROPERTY_EXPIRES_IN = TX_AUTH_NS + "expiresIn";
    private final EndpointDataReferenceCache edrCache;
    private final EdcHttpClient httpClient;
    private final String ownDid;
    private final Monitor monitor;
    private final SecureTokenService secureTokenService;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new TokenRefreshHandler
     *
     * @param edrCache           a persistent storage where {@link EndpointDataReference} objects are stored.
     * @param httpClient         needed to make the actual refresh call against the refresh endpoint
     * @param ownDid             the DID of this connector
     * @param secureTokenService Service to generate the authentication token
     * @param objectMapper       ObjectMapper to interpret JSON responses
     */
    public TokenRefreshHandlerImpl(EndpointDataReferenceCache edrCache,
                                   EdcHttpClient httpClient,
                                   String ownDid,
                                   Monitor monitor,
                                   SecureTokenService secureTokenService,
                                   ObjectMapper objectMapper) {
        this.edrCache = edrCache;
        this.httpClient = httpClient;
        this.ownDid = ownDid;
        this.monitor = monitor;
        this.secureTokenService = secureTokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ServiceResult<DataAddress> refreshToken(String tokenId) {
        var edrResult = edrCache.get(tokenId);
        if (edrResult.failed()) {
            return ServiceResult.notFound(edrResult.getFailureDetail());
        }
        var edr = edrResult.getContent();
        return refreshToken(tokenId, edr);
    }

    @Override
    public ServiceResult<DataAddress> refreshToken(String tokenId, DataAddress edr) {
        var accessToken = edr.getStringProperty(PROPERTY_AUTHORIZATION);
        var refreshToken = edr.getProperties().get(PROPERTY_REFRESH_TOKEN);
        var refreshEndpoint = edr.getProperties().get(PROPERTY_REFRESH_ENDPOINT);

        if (isNullOrBlank(accessToken)) {
            return ServiceResult.badRequest("Cannot perform token refresh: required property 'authorization' not found on EDR.");
        }
        if (isNullOrBlank(StringUtils.toString(refreshToken))) {
            return ServiceResult.badRequest("Cannot perform token refresh: required property 'refreshToken' not found on EDR.");
        }
        if (isNullOrBlank(StringUtils.toString(refreshEndpoint))) {
            return ServiceResult.badRequest("Cannot perform token refresh: required property 'refreshEndpoint' not found on EDR.");
        }

        var result = getStringClaim(accessToken, ISSUER)
                .map(audience -> Map.of(
                        JWT_ID, tokenId,
                        ISSUER, ownDid,
                        SUBJECT, ownDid,
                        AUDIENCE, audience
                ))
                .compose(claims -> secureTokenService.createToken(claims, accessToken))
                .compose(authToken -> createTokenRefreshRequest(refreshEndpoint.toString(), refreshToken.toString(), "Bearer %s".formatted(authToken.getToken())));

        if (result.failed()) {
            return ServiceResult.badRequest("Could not execute token refresh: " + result.getFailureDetail());
        }

        return executeRequest(result.getContent())
                .compose(tr -> update(tokenId, edr, tr));
    }

    private ServiceResult<DataAddress> update(String id, DataAddress oldEdr, TokenResponse tokenResponse) {
        //todo: create new DataAddress out of the oldEdr, update refresh token, store and return
        var newEdr = DataAddress.Builder.newInstance()
                .type(oldEdr.getType())
                .properties(oldEdr.getProperties())
                .property(PROPERTY_AUTHORIZATION, tokenResponse.accessToken())
                .property(PROPERTY_REFRESH_TOKEN, tokenResponse.refreshToken())
                .property(PROPERTY_EXPIRES_IN, String.valueOf(tokenResponse.expiresInSeconds()))
                .build();
        return ServiceResult.from(edrCache.put(id, newEdr)).map(u -> newEdr);
    }

    private ServiceResult<TokenResponse> executeRequest(Request tokenRefreshRequest) {
        try (var response = httpClient.execute(tokenRefreshRequest)) {
            if (response.isSuccessful()) {
                if (response.body() != null) {

                    var jsonBody = response.body().string();
                    if (!StringUtils.isNullOrEmpty(jsonBody)) {
                        var tokenResponse = objectMapper.readValue(jsonBody, TokenResponse.class);
                        return ServiceResult.success(tokenResponse);
                    }
                }
                return ServiceResult.badRequest("Token refresh successful, but body was empty.");
            }
            return switch (response.code()) {
                case 401 -> ServiceResult.unauthorized(response.message());
                case 409 -> ServiceResult.conflict(response.message());
                case 404 -> ServiceResult.notFound(response.message());
                default -> ServiceResult.badRequest(response.message());
            };
        } catch (IOException e) {
            monitor.warning("Error executing token refresh request", e);
            return ServiceResult.from(StoreResult.generalError("Error executing token refresh request: %s".formatted(e)));
        }
    }

    private Result<Request> createTokenRefreshRequest(String refreshEndpoint, String refreshToken, String bearerToken) {
        // see https://github.com/eclipse-tractusx/tractusx-profiles/blob/main/tx/refresh/refresh.token.grant.profile.md#3-the-refresh-request
        if (!refreshEndpoint.endsWith("/token")) {
            refreshEndpoint += "/token";
        }
        var url = HttpUrl.parse(refreshEndpoint)
                .newBuilder()
                .addQueryParameter("grant_type", "refresh_token")
                .addQueryParameter("refresh_token", refreshToken)
                .build();

        return success(new Request.Builder()
                .addHeader("Authorization", bearerToken)
                .url(url)
                .post(RequestBody.create(new byte[0]))
                .build());
    }

    private Result<String> getStringClaim(String accessToken, String claimName) {
        try {
            return success(SignedJWT.parse(accessToken).getJWTClaimsSet().getStringClaim(claimName));
        } catch (ParseException e) {
            monitor.warning("Failed to get string claim '%s'".formatted(claimName), e);
            return Result.failure("Failed to parse string claim '%s': %s".formatted(claimName, e));
        }
    }

}
