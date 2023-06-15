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
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.lang.String.format;

public class MiwApiClientImpl implements MiwApiClient {

    public static final MediaType TYPE_JSON = MediaType.parse("application/json");
    private static final String CREDENTIAL_PATH = "/api/credentials";
    private static final String PRESENTATIONS_PATH = "/api/presentations";

    private final EdcHttpClient httpClient;
    private final String baseUrl;
    private final ObjectMapper mapper;
    private final Monitor monitor;

    public MiwApiClientImpl(EdcHttpClient httpClient, String baseUrl, ObjectMapper mapper, Monitor monitor) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
        this.mapper = mapper;
        this.monitor = monitor;
    }

    @Override
    public Result<List<Map<String, Object>>> getCredentials(Set<String> types, String holderIdentifier) {

        var params = new ArrayList<String>();
        params.add(format("holderIdentifier=%s", holderIdentifier));

        if (!types.isEmpty()) {
            params.add(format("type=%s", String.join(",", types)));
        }
        
        var queryParams = "?" + String.join("&", params);
        var url = baseUrl + CREDENTIAL_PATH + queryParams;
        var request = new Request.Builder().get().url(url).build();

        return executeRequest(request, new TypeReference<>() {
        });
    }

    @Override
    public Result<Map<String, Object>> createPresentation(List<Map<String, Object>> credentials, String holderIdentifier) {
        try {
            var body = Map.of("holderIdentifier", holderIdentifier, "verifiableCredentials", credentials);
            var url = baseUrl + PRESENTATIONS_PATH + "?asJwt=true";
            var requestBody = RequestBody.create(mapper.writeValueAsString(body), TYPE_JSON);
            var request = new Request.Builder().post(requestBody).url(url).build();

            return executeRequest(request, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return Result.failure(e.getMessage());
        }
    }

    private <R> Result<R> executeRequest(Request request, TypeReference<R> typeReference) {
        try (var response = httpClient.execute(request)) {
            return handleResponse(response, typeReference);
        } catch (IOException e) {
            return Result.failure(e.getMessage());
        }
    }

    @Override
    public Result<Void> verifyPresentation(String jwtPresentation) {
        return Result.success();
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
            monitor.debug("Failed to parse response from MIW");
            return Result.failure(e.getMessage());
        }
    }

    private <R> Result<R> handleError(Response response) {
        var msg = format("MIW API returned %s", response.code());
        monitor.debug(msg);
        return Result.failure(msg);
    }

}
