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

package org.eclipse.tractusx.edc.iam.dcp.sts.dim;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.identitytrust.spi.SecureTokenService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.tractusx.edc.iam.dcp.sts.dim.oauth.DimOauth2Client;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static java.lang.String.format;
import static org.eclipse.edc.http.spi.FallbackFactories.retryWhenStatusIsNotIn;
import static org.eclipse.edc.iam.identitytrust.spi.SelfIssuedTokenConstants.PRESENTATION_TOKEN_CLAIM;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;

/**
 * Implementation of {@link SecureTokenService} that talks with DIM wallet. It supports two APIs for fetching the
 * SI Token:
 * <ul>
 *     <li>grantAccess: request the SI token to DIM by providing the credential types required</li>
 *     <li>signToken: request the SI token to DIM by providing the extracted `token` from the received SI token</li>
 * </ul>
 */
public class DimSecureTokenService implements SecureTokenService {

    public static final MediaType TYPE_JSON = MediaType.parse("application/json");
    public static final String READ_SCOPE = "read";
    public static final String GRANT_ACCESS = "grantAccess";
    public static final String SCOPE = "scope";
    public static final String CREDENTIAL_TYPES = "credentialTypes";
    public static final String SIGN_TOKEN = "signToken";

    private final EdcHttpClient httpClient;
    private final String dimUrl;
    private final DimOauth2Client dimOauth2Client;
    private final ObjectMapper mapper;
    private final Monitor monitor;

    private final Map<String, String> grantAccessMapper = Map.of(
            ISSUER, "consumerDid",
            AUDIENCE, "providerDid");

    private final Map<String, String> signTokenMapper = Map.of(
            AUDIENCE, "audience",
            ISSUER, "issuer",
            SUBJECT, "subject",
            PRESENTATION_TOKEN_CLAIM, PRESENTATION_TOKEN_CLAIM);

    public DimSecureTokenService(EdcHttpClient httpClient, String dimUrl, DimOauth2Client dimOauth2Client, ObjectMapper mapper, Monitor monitor) {
        this.httpClient = httpClient;
        this.dimUrl = dimUrl;
        this.dimOauth2Client = dimOauth2Client;
        this.mapper = mapper;
        this.monitor = monitor;
    }

    @Override
    public Result<TokenRepresentation> createToken(Map<String, Object> claims, @Nullable String bearerAccessScope) {
        return Optional.ofNullable(bearerAccessScope)
                .map(scope -> grantAccessRequest(claims, scope))
                .orElseGet(() -> signTokenRequest(claims));
    }

    private Result<TokenRepresentation> grantAccessRequest(Map<String, Object> claims, @Nullable String bearerAccessScope) {
        return grantAccessPayload(claims, bearerAccessScope)
                .compose(this::postRequest)
                .map(builder -> builder.url(dimUrl).build())
                .compose(request -> executeRequest(request, GRANT_ACCESS));
    }

    private Result<TokenRepresentation> signTokenRequest(Map<String, Object> claims) {
        return signTokenPayload(claims)
                .compose(this::postRequest)
                .map(builder -> builder.url(dimUrl).build())
                .compose(request -> executeRequest(request, SIGN_TOKEN));
    }

    private Result<Map<String, Object>> grantAccessPayload(Map<String, Object> claims, String bearerAccessScope) {
        return mapClaims(claims, grantAccessMapper)
                .compose(payload -> extractScopes(bearerAccessScope)
                        .onSuccess(scopes -> payload.put(CREDENTIAL_TYPES, scopes))
                        .compose(i -> Result.success(payload)))
                .map(payload -> {
                    payload.put(SCOPE, READ_SCOPE);
                    return Map.of(GRANT_ACCESS, payload);
                });
    }

    private Result<Set<String>> extractScopes(String bearerAccessScope) {
        var scopes = new HashSet<String>();
        var split = bearerAccessScope.split(" ");

        var result = Arrays.stream(split)
                .map(scope -> extractCredential(scope, scopes::add))
                .reduce(Result::merge)
                .orElseGet(Result::success);

        if (result.succeeded()) {
            return Result.success(scopes);
        } else {
            return Result.failure(result.getFailureDetail());
        }
    }

    private Result<Void> extractCredential(String scope, Consumer<String> consumer) {
        var tokens = scope.split(":");
        if (tokens.length != 3) {
            return Result.failure("Scope string %s has invalid format".formatted(scope));
        }
        consumer.accept(tokens[1]);
        return Result.success();
    }

    @NotNull
    private Result<Map<String, Object>> signTokenPayload(Map<String, Object> claims) {
        return mapClaims(claims, signTokenMapper)
                .map(payload -> Map.of(SIGN_TOKEN, payload));
    }

    private Result<TokenRepresentation> executeRequest(Request request, String context) {
        return httpClient.execute(request, List.of(retryWhenStatusIsNotIn(200, 201)), this::handleResponse)
                .recover(failure -> Result.failure("[%s] %s".formatted(context, failure.getFailureDetail())));
    }

    private Result<TokenRepresentation> handleResponse(Response response) {
        try {
            var body = Objects.requireNonNull(response.body()).string();
            var parsedBody = mapper.readValue(body, new TypeReference<Map<String, Object>>() {
            });
            return Optional.ofNullable(parsedBody.get("jwt"))
                    .map(token -> TokenRepresentation.Builder.newInstance().token(token.toString()).build())
                    .map(Result::success)
                    .orElseGet(() -> Result.failure("Failed to get jwt field"));
        } catch (IOException e) {
            monitor.severe("Failed to parse response from DIM");
            return Result.failure(e.getMessage());
        }
    }

    private Result<Request.Builder> postRequest(Map<String, Object> body) {
        try {
            var requestBody = RequestBody.create(mapper.writeValueAsString(body), TYPE_JSON);
            return baseRequestWithToken()
                    .map(builder -> builder.post(requestBody));
        } catch (JsonProcessingException e) {
            return Result.failure(e.getMessage());
        }

    }

    private Result<Request.Builder> baseRequestWithToken() {
        return dimOauth2Client.obtainRequestToken()
                .map(this::baseRequestWithToken);
    }

    private Request.Builder baseRequestWithToken(TokenRepresentation tokenRepresentation) {
        return new Request.Builder()
                .addHeader("Authorization", format("Bearer %s", tokenRepresentation.getToken()));
    }

    private Result<Map<String, Object>> mapClaims(Map<String, Object> claims, Map<String, String> mappings) {
        var payload = new HashMap<String, Object>();
        var result = mappings.entrySet().stream()
                .map((entry -> mapClaim(claims, entry)))
                .peek(inner -> inner.onSuccess(mapped -> payload.put(mapped.getKey(), mapped.getValue())))
                .reduce(Result::merge)
                .orElseGet(() -> Result.success(null));

        if (result.failed()) {
            return Result.failure(result.getFailureDetail());
        } else {
            return Result.success(payload);
        }

    }

    private Result<Map.Entry<String, Object>> mapClaim(Map<String, Object> claims, Map.Entry<String, String> mapping) {
        var value = claims.get(mapping.getKey());
        if (value != null) {
            return Result.success(Map.entry(mapping.getValue(), value));
        } else {
            return Result.failure("Key %s not found in the input claims".formatted(mapping.getKey()));
        }
    }

}
