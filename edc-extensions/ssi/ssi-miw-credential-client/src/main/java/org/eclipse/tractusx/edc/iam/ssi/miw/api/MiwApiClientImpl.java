/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.iam.ssi.miw.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.tractusx.edc.iam.ssi.miw.oauth2.MiwOauth2Client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static org.eclipse.tractusx.edc.iam.ssi.miw.api.MiwFallbackFactories.retryWhenStatusIsNotIn;

public class MiwApiClientImpl implements MiwApiClient {

    public static final MediaType TYPE_JSON = MediaType.parse("application/json");
    public static final String CREDENTIAL_PATH = "/api/credentials";
    public static final String PRESENTATIONS_PATH = "/api/presentations";
    public static final String PRESENTATIONS_VALIDATION_PATH = "/api/presentations/validation";
    public static final String HOLDER_IDENTIFIER = "holderIdentifier";

    public static final String ISSUER_IDENTIFIER = "issuerIdentifier";
    public static final String VERIFIABLE_CREDENTIALS = "verifiableCredentials";
    public static final String VP_FIELD = "vp";
    public static final String CONTENT_FIELD = "content";
    private static final String PRESENTATIONS_QUERY_PARAMS = "?asJwt=true&audience=%s";
    private final EdcHttpClient httpClient;
    private final String baseUrl;
    private final MiwOauth2Client oauth2Client;
    private final ObjectMapper mapper;
    private final Monitor monitor;

    private final String authorityId;

    private final String participantId;

    public MiwApiClientImpl(EdcHttpClient httpClient, String baseUrl, MiwOauth2Client oauth2Client, String participantId, String authorityId, ObjectMapper mapper, Monitor monitor) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
        this.oauth2Client = oauth2Client;
        this.participantId = participantId;
        this.authorityId = authorityId;
        this.mapper = mapper;
        this.monitor = monitor;
    }

    @Override
    public Result<List<Map<String, Object>>> getCredentials(Set<String> types) {

        var params = new ArrayList<String>();
        params.add(format("%s=%s", ISSUER_IDENTIFIER, authorityId));

        if (!types.isEmpty()) {
            params.add(format("type=%s", String.join(",", types)));
        }

        var queryParams = "?" + String.join("&", params);
        var url = baseUrl + CREDENTIAL_PATH + queryParams;

        return baseRequestWithToken().map(builder -> builder.get().url(url).build())
                .compose(request -> executeRequest(request, new TypeReference<Map<String, Object>>() {
                }))
                .compose(this::handleGetCredentialResponse);
    }

    @Override
    public Result<Map<String, Object>> createPresentation(List<Map<String, Object>> credentials, String audience) {
        try {
            var body = Map.of(HOLDER_IDENTIFIER, participantId, VERIFIABLE_CREDENTIALS, credentials);
            var url = baseUrl + PRESENTATIONS_PATH + format(PRESENTATIONS_QUERY_PARAMS, audience);
            var requestBody = RequestBody.create(mapper.writeValueAsString(body), TYPE_JSON);

            return baseRequestWithToken().map(builder -> builder.post(requestBody).url(url).build())
                    .compose(request -> executeRequest(request, new TypeReference<>() {
                    }));
        } catch (JsonProcessingException e) {
            return Result.failure(e.getMessage());
        }
    }

    @Override
    public Result<Void> verifyPresentation(String jwtPresentation, String audience) {
        try {
            var body = Map.of(VP_FIELD, jwtPresentation);
            var url = baseUrl + PRESENTATIONS_VALIDATION_PATH + format(PRESENTATIONS_QUERY_PARAMS, audience);
            var requestBody = RequestBody.create(mapper.writeValueAsString(body), TYPE_JSON);

            return baseRequestWithToken().map(builder -> builder.post(requestBody).url(url).build())
                    .compose(request -> executeRequest(request, new TypeReference<Map<String, Object>>() {
                    }))
                    .compose(this::handleVerifyResult);
        } catch (JsonProcessingException e) {
            return Result.failure(e.getMessage());
        }
    }

    private Result<List<Map<String, Object>>> handleGetCredentialResponse(Map<String, Object> response) {
        var content = response.get(CONTENT_FIELD);

        if (content == null) {
            return Result.failure("Missing content field in the credentials response");
        }
        return Result.success((List<Map<String, Object>>) content);
    }

    private Result<Void> handleVerifyResult(Map<String, Object> response) {
        var valid = Optional.ofNullable(response.get("valid"))
                .map(Boolean.TRUE::equals)
                .orElse(false);

        if (valid) {
            return Result.success();
        } else {
            var msg = "MIW verification failed";
            monitor.severe(msg);
            return Result.failure(msg);
        }
    }

    private <R> Result<R> executeRequest(Request request, TypeReference<R> typeReference) {
        try (var response = httpClient.execute(request, List.of(retryWhenStatusIsNotIn(200, 201)))) {
            return handleResponse(response, typeReference);
        } catch (MiwClientException e) {
            if (e.getResponse() != null) {
                return handleError(e.getResponse());
            } else {
                return Result.failure(e.getMessage());
            }
        } catch (IOException e) {
            return Result.failure(e.getMessage());
        }
    }

    private <R> Result<R> handleResponse(Response response, TypeReference<R> tr) {
        if (response.isSuccessful()) {
            return handleSuccess(response, tr);
        } else {
            return handleError(response);
        }
    }

    private <R> Result<R> handleSuccess(Response response, TypeReference<R> tr) {
        try {
            var body = Objects.requireNonNull(response.body()).string();
            return Result.success(mapper.readValue(body, tr));
        } catch (IOException e) {
            monitor.severe("Failed to parse response from MIW");
            return Result.failure(e.getMessage());
        }
    }

    private <R> Result<R> handleError(Response response) {
        var body = "";
        if (response.body() != null) {
            try {
                body = response.body().string();
            } catch (IOException e) {
                monitor.severe("Failed to read response from MIW");
                return Result.failure(e.getMessage());
            }
        }
        var code = response.code();
        monitor.severe(format("MIW API returned %s with body: %s", code, body));
        return Result.failure(format("MIW API returned %s", code));
    }

    private Result<Request.Builder> baseRequestWithToken() {
        return oauth2Client.obtainRequestToken()
                .map(this::baseRequestWithToken);
    }

    private Request.Builder baseRequestWithToken(TokenRepresentation tokenRepresentation) {
        return new Request.Builder()
                .addHeader("Authorization", format("Bearer %s", tokenRepresentation.getToken()));
    }
}
