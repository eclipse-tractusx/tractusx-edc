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
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceCache;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.identitytrust.spi.SecureTokenService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.util.string.StringUtils;
import org.eclipse.tractusx.edc.spi.tokenrefresh.common.TokenRefreshHandler;
import org.eclipse.tractusx.edc.spi.tokenrefresh.dataplane.model.TokenResponse;

import java.io.IOException;
import java.util.Map;

import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.JWT_ID;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;
import static org.eclipse.edc.spi.result.Result.success;
import static org.eclipse.edc.util.string.StringUtils.isNullOrBlank;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_AUTHORIZATION;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_EXPIRES_IN;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_REFRESH_AUDIENCE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_REFRESH_ENDPOINT;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_REFRESH_TOKEN;

public class TokenRefreshHandlerImpl implements TokenRefreshHandler {
    private final EndpointDataReferenceCache edrCache;
    private final EdcHttpClient httpClient;
    private final String ownDid;
    private final Monitor monitor;
    private final SecureTokenService secureTokenService;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new TokenRefreshHandler
     *
     * @param edrCache           a persistent storage where {@link DataAddress} objects are stored.
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
        var accessToken = edr.getStringProperty(EDR_PROPERTY_AUTHORIZATION);
        var refreshToken = edr.getProperties().get(EDR_PROPERTY_REFRESH_TOKEN);
        var refreshEndpoint = edr.getProperties().get(EDR_PROPERTY_REFRESH_ENDPOINT);
        var refreshAudience = edr.getProperties().get(EDR_PROPERTY_REFRESH_AUDIENCE);

        if (isNullOrBlank(accessToken)) {
            return ServiceResult.badRequest("Cannot perform token refresh: required property 'authorization' not found on EDR.");
        }
        if (isNullOrBlank(StringUtils.toString(refreshToken))) {
            return ServiceResult.badRequest("Cannot perform token refresh: required property 'refreshToken' not found on EDR.");
        }
        if (isNullOrBlank(StringUtils.toString(refreshEndpoint))) {
            return ServiceResult.badRequest("Cannot perform token refresh: required property 'refreshEndpoint' not found on EDR.");
        }
        if (isNullOrBlank(StringUtils.toString(refreshAudience))) {
            return ServiceResult.badRequest("Cannot perform token refresh: required property 'refreshAudience' not found on EDR.");
        }

        var claims = Map.<String, Object>of(
                JWT_ID, tokenId,
                ISSUER, ownDid,
                SUBJECT, ownDid,
                AUDIENCE, refreshAudience.toString(),
                "token", accessToken
        );

        var result = secureTokenService.createToken(claims, null)
                .compose(authToken -> createTokenRefreshRequest(refreshEndpoint.toString(), refreshToken.toString(), "Bearer %s".formatted(authToken.getToken())));

        if (result.failed()) {
            return ServiceResult.badRequest("Could not execute token refresh: " + result.getFailureDetail());
        }

        return executeRequest(result.getContent())
                .map(tr -> createNewEdr(edr, tr));
    }

    private DataAddress createNewEdr(DataAddress oldEdr, TokenResponse tokenResponse) {
        return DataAddress.Builder.newInstance()
                .type(oldEdr.getType())
                .properties(oldEdr.getProperties())
                .property(EDR_PROPERTY_AUTHORIZATION, tokenResponse.accessToken())
                .property(EDR_PROPERTY_REFRESH_TOKEN, tokenResponse.refreshToken())
                .property(EDR_PROPERTY_EXPIRES_IN, String.valueOf(tokenResponse.expiresInSeconds()))
                .build();
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
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .url(url)
                .post(RequestBody.create(new byte[0]))
                .build());
    }

}
