/********************************************************************************
 * Copyright (c) 2024 Robert Bosch Manufacturing Solutions GmbH and others
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.AccessControlServerException;
import org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.HttpAccessControlCheckDtrClientConfig;
import org.jetbrains.annotations.NotNull;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;

public class DtrOauth2TokenClient implements Oauth2TokenClient {

    static final TypeReference<HashMap<String, String>> MAP_TYPE_REFERENCE = new TypeReference<>() {
    };
    static final String ACCESS_TOKEN = "access_token";
    private final Monitor monitor;
    private final EdcHttpClient httpClient;
    private final HttpAccessControlCheckDtrClientConfig dtrConfig;
    private final TypeManager typeManager;
    private final LoadingCache<String, String> secretCache;

    public DtrOauth2TokenClient(
            final Monitor monitor,
            final EdcHttpClient httpClient,
            final TypeManager typeManager,
            final Vault vault,
            final HttpAccessControlCheckDtrClientConfig dtrConfig) {
        this.monitor = monitor;
        this.httpClient = httpClient;
        this.typeManager = typeManager;
        this.dtrConfig = dtrConfig;
        this.secretCache = Caffeine.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(Duration.ofMinutes(30))
                .refreshAfterWrite(Duration.ofMinutes(15))
                .build(vault::resolveSecret);
    }

    @Override
    public String getBearerToken(final String scope) {
        final Request request = createTokenRequest(scope);
        try (Response response = httpClient.execute(request)) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("OAuth2 authentication error. Response code=" + response.code());
            }
            final ResponseBody body = response.body();
            if (body == null) {
                throw new IllegalStateException("OAuth2 response body is empty.");
            }
            final var map = typeManager.readValue(body.string(), MAP_TYPE_REFERENCE);
            if (!map.containsKey(ACCESS_TOKEN)) {
                throw new IllegalStateException("OAuth2 response body had no token.");
            }
            return map.get(ACCESS_TOKEN);
        } catch (final Exception e) {
            monitor.severe("Failed to obtain Bearer token: " + e.getMessage());
            return null;
        }
    }

    @NotNull
    private String fetchClientSecret() {
        final String secret = secretCache.get(dtrConfig.getOauth2ClientSecretPath());
        if (secret == null) {
            throw new AccessControlServerException("Cannot resolve authentication credentials.");
        }
        return secret;
    }

    @NotNull
    private Request createTokenRequest(final String scope) {
        final String secret = fetchClientSecret();
        final FormBody formBody = new FormBody(
                List.of("grant_type", "client_id", "client_secret", "scope"),
                List.of("client_credentials",
                        URLEncoder.encode(dtrConfig.getOauth2ClientId(), StandardCharsets.UTF_8),
                        URLEncoder.encode(secret, StandardCharsets.UTF_8),
                        URLEncoder.encode(scope, StandardCharsets.UTF_8)));
        return new Request.Builder()
                .url(dtrConfig.getOauth2TokenEndpointUrl())
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", formBody.contentType().toString())
                .post(formBody)
                .build();
    }
}
