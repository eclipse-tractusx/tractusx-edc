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
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
class HashicorpVaultClient {
  static final String VAULT_DATA_ENTRY_NAME = "content";
  private static final String VAULT_TOKEN_HEADER = "X-Vault-Token";
  private static final String VAULT_REQUEST_HEADER = "X-Vault-Request";
  private static final String VAULT_SECRET_DATA_PATH = "data";
  private static final String VAULT_SECRET_METADATA_PATH = "metadata";
  private static final MediaType MEDIA_TYPE_APPLICATION_JSON = MediaType.get("application/json");
  private static final String CALL_UNSUCCESSFUL_ERROR_TEMPLATE = "Call unsuccessful: %s";

  @NonNull private final HashicorpVaultClientConfig config;
  @NonNull private final OkHttpClient okHttpClient;
  @NonNull private final ObjectMapper objectMapper;

  Result<String> getSecretValue(@NonNull String key) {
    HttpUrl requestURI = getSecretUrl(key, VAULT_SECRET_DATA_PATH);
    Headers headers = getHeaders();
    Request request = new Request.Builder().url(requestURI).headers(headers).get().build();

    try (Response response = okHttpClient.newCall(request).execute()) {

      if (response.code() == 404) {
        return Result.failure(String.format(CALL_UNSUCCESSFUL_ERROR_TEMPLATE, "Secret not found"));
      }

      if (response.isSuccessful()) {
        String responseBody = Objects.requireNonNull(response.body()).string();
        HashicorpVaultGetEntryResponsePayload payload =
            objectMapper.readValue(responseBody, HashicorpVaultGetEntryResponsePayload.class);
        String value =
            Objects.requireNonNull(payload.getData().getData().get(VAULT_DATA_ENTRY_NAME));

        return Result.success(value);
      } else {
        return Result.failure(String.format(CALL_UNSUCCESSFUL_ERROR_TEMPLATE, response.code()));
      }

    } catch (IOException e) {
      return Result.failure(e.getMessage());
    }
  }

  public HashicorpVaultHealthResponse getHealth() throws IOException {

    HashicorpVaultHealthResponse.HashicorpVaultHealthResponseBuilder healthResponseBuilder =
        HashicorpVaultHealthResponse.builder();

    HttpUrl requestURI = getHealthUrl();
    Headers headers = getHeaders();
    Request request = new Request.Builder().url(requestURI).headers(headers).get().build();
    try (Response response = okHttpClient.newCall(request).execute()) {
      final int code = response.code();
      healthResponseBuilder.code(code);

      try {
        String responseBody = Objects.requireNonNull(response.body()).string();
        HashicorpVaultHealthResponsePayload responsePayload =
            objectMapper.readValue(responseBody, HashicorpVaultHealthResponsePayload.class);
        healthResponseBuilder.payload(responsePayload);
      } catch (JsonMappingException e) {
        // ignore. status code not checked, so it may be possible that no payload was
        // provided
      }
    }

    return healthResponseBuilder.build();
  }

  Result<HashicorpVaultCreateEntryResponsePayload> setSecret(
      @NonNull String key, @NonNull String value) {
    HttpUrl requestURI = getSecretUrl(key, VAULT_SECRET_DATA_PATH);
    Headers headers = getHeaders();
    HashicorpVaultCreateEntryRequestPayload requestPayload =
        HashicorpVaultCreateEntryRequestPayload.builder()
            .data(Collections.singletonMap(VAULT_DATA_ENTRY_NAME, value))
            .build();
    Request request =
        new Request.Builder()
            .url(requestURI)
            .headers(headers)
            .post(createRequestBody(requestPayload))
            .build();

    try (Response response = okHttpClient.newCall(request).execute()) {
      if (response.isSuccessful()) {
        String responseBody = Objects.requireNonNull(response.body()).string();
        HashicorpVaultCreateEntryResponsePayload responsePayload =
            objectMapper.readValue(responseBody, HashicorpVaultCreateEntryResponsePayload.class);
        return Result.success(responsePayload);
      } else {
        return Result.failure(String.format(CALL_UNSUCCESSFUL_ERROR_TEMPLATE, response.code()));
      }
    } catch (IOException e) {
      return Result.failure(e.getMessage());
    }
  }

  Result<Void> destroySecret(@NonNull String key) {
    HttpUrl requestURI = getSecretUrl(key, VAULT_SECRET_METADATA_PATH);
    Headers headers = getHeaders();
    Request request = new Request.Builder().url(requestURI).headers(headers).delete().build();

    try (Response response = okHttpClient.newCall(request).execute()) {
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

    final String vaultApiPath = config.getVaultApiSecretPath();

    return Objects.requireNonNull(HttpUrl.parse(config.getVaultUrl()))
        .newBuilder()
        .addPathSegments(PathUtil.trimLeadingOrEndingSlash(vaultApiPath))
        .addPathSegment(entryType)
        .addPathSegments(key)
        .build();
  }

  private HttpUrl getHealthUrl() {
    final String vaultHealthPath = config.getVaultApiHealthPath();
    final boolean isVaultHealthStandbyOk = config.isVaultApiHealthStandbyOk();

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
