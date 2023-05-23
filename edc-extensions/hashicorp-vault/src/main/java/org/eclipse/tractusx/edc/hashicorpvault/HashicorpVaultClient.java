/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.edc.hashicorpvault;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;

public class HashicorpVaultClient {
    static final String VAULT_DATA_ENTRY_NAME = "content";
    private static final String VAULT_TOKEN_HEADER = "X-Vault-Token";
    private static final String VAULT_REQUEST_HEADER = "X-Vault-Request";
    private static final String VAULT_SECRET_DATA_PATH = "data";
    private static final String VAULT_SECRET_METADATA_PATH = "metadata";
    private static final MediaType MEDIA_TYPE_APPLICATION_JSON = MediaType.get("application/json");
    private static final String CALL_UNSUCCESSFUL_ERROR_TEMPLATE = "Call unsuccessful: %s";

    private final HashicorpVaultClientConfig config;
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    public HashicorpVaultClient(HashicorpVaultClientConfig config, OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        this.config = config;
        this.okHttpClient = okHttpClient;
        this.objectMapper = objectMapper;
    }

    Result<String> getSecretValue(String key) {
        var requestUri = getSecretUrl(key, VAULT_SECRET_DATA_PATH);
        var headers = getHeaders();
        var request = new Request.Builder().url(requestUri).headers(headers).get().build();

        try (var response = okHttpClient.newCall(request).execute()) {

            if (response.code() == 404) {
                return Result.failure(String.format(CALL_UNSUCCESSFUL_ERROR_TEMPLATE, "Secret not found"));
            }

            if (response.isSuccessful()) {
                var responseBody = Objects.requireNonNull(response.body()).string();
                var payload = objectMapper.readValue(responseBody, HashicorpVaultGetEntryResponsePayload.class);
                var value = Objects.requireNonNull(payload.getData().getData().get(VAULT_DATA_ENTRY_NAME));

                return Result.success(value);
            } else {
                return Result.failure(String.format(CALL_UNSUCCESSFUL_ERROR_TEMPLATE, response.code()));
            }

        } catch (IOException e) {
            return Result.failure(e.getMessage());
        }
    }

    public HashicorpVaultHealthResponse getHealth() {

        var healthResponseBuilder = HashicorpVaultHealthResponse.Builder.newInstance();

        var requestUri = getHealthUrl();
        var headers = getHeaders();
        var request = new Request.Builder().url(requestUri).headers(headers).get().build();
        try (var response = okHttpClient.newCall(request).execute()) {
            final var code = response.code();
            healthResponseBuilder.code(code);

            try {
                var responseBody = Objects.requireNonNull(response.body()).string();
                var responsePayload = objectMapper.readValue(responseBody, HashicorpVaultHealthResponsePayload.class);
                healthResponseBuilder.payload(responsePayload);
            } catch (JsonMappingException e) {
                // ignore. status code not checked, so it may be possible that no payload was
                // provided
            }
        } catch (IOException e) {
            throw new EdcException(e);
        }

        return healthResponseBuilder.build();
    }

    Result<HashicorpVaultCreateEntryResponsePayload> setSecret(
            String key, String value) {
        var requestUri = getSecretUrl(key, VAULT_SECRET_DATA_PATH);
        var headers = getHeaders();
        var requestPayload =
                HashicorpVaultCreateEntryRequestPayload.Builder.newInstance()
                        .data(Collections.singletonMap(VAULT_DATA_ENTRY_NAME, value))
                        .build();
        var request = new Request.Builder()
                .url(requestUri)
                .headers(headers)
                .post(createRequestBody(requestPayload))
                .build();

        try (var response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                var responseBody = Objects.requireNonNull(response.body()).string();
                var responsePayload =
                        objectMapper.readValue(responseBody, HashicorpVaultCreateEntryResponsePayload.class);
                return Result.success(responsePayload);
            } else {
                return Result.failure(String.format(CALL_UNSUCCESSFUL_ERROR_TEMPLATE, response.code()));
            }
        } catch (IOException e) {
            return Result.failure(e.getMessage());
        }
    }

    Result<Void> destroySecret(String key) {
        var requestUri = getSecretUrl(key, VAULT_SECRET_METADATA_PATH);
        var headers = getHeaders();
        var request = new Request.Builder().url(requestUri).headers(headers).delete().build();

        try (var response = okHttpClient.newCall(request).execute()) {
            return response.isSuccessful() || response.code() == 404
                    ? Result.success()
                    : Result.failure(String.format(CALL_UNSUCCESSFUL_ERROR_TEMPLATE, response.code()));
        } catch (IOException e) {
            return Result.failure(e.getMessage());
        }
    }

    @NotNull
    private Headers getHeaders() {
        return new Headers.Builder()
                .add(VAULT_REQUEST_HEADER, Boolean.toString(true))
                .add(VAULT_TOKEN_HEADER, config.getVaultToken())
                .build();
    }

    private HttpUrl getSecretUrl(String key, String entryType) {
        key = URLEncoder.encode(key, StandardCharsets.UTF_8);

        // restore '/' characters to allow sub-directories
        key = key.replace("%2F", "/");

        final var vaultApiPath = config.getVaultApiSecretPath();

        return Objects.requireNonNull(HttpUrl.parse(config.getVaultUrl()))
                .newBuilder()
                .addPathSegments(PathUtil.trimLeadingOrEndingSlash(vaultApiPath))
                .addPathSegment(entryType)
                .addPathSegments(key)
                .build();
    }

    private HttpUrl getHealthUrl() {
        final var vaultHealthPath = config.getVaultApiHealthPath();
        final var isVaultHealthStandbyOk = config.isVaultApiHealthStandbyOk();

        // by setting 'standbyok' and/or 'perfstandbyok' the vault will return an active
        // status
        // code instead of the standby status codes

        return Objects.requireNonNull(HttpUrl.parse(config.getVaultUrl()))
                .newBuilder()
                .addPathSegments(PathUtil.trimLeadingOrEndingSlash(vaultHealthPath))
                .addQueryParameter("standbyok", isVaultHealthStandbyOk ? "true" : "false")
                .addQueryParameter("perfstandbyok", isVaultHealthStandbyOk ? "true" : "false")
                .build();
    }

    private RequestBody createRequestBody(Object requestPayload) {
        String jsonRepresentation;
        try {
            jsonRepresentation = objectMapper.writeValueAsString(requestPayload);
        } catch (JsonProcessingException e) {
            throw new HashicorpVaultException(e.getMessage(), e);
        }
        return RequestBody.create(jsonRepresentation, MEDIA_TYPE_APPLICATION_JSON);
    }
}
