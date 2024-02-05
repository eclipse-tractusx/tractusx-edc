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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.AccessControlServerException;
import org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.HttpAccessControlCheckClientConfig;
import org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.HttpAccessControlCheckDtrClientConfig;
import org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.client.model.DtrAccessVerificationRequest;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.HttpAccessControlRequestParamsDecorator.HEADER_EDC_BPN;

public class DtrAccessVerificationClient implements HttpAccessVerificationClient {

    private static final String APPLICATION_JSON = "application/json";
    private final Monitor monitor;
    private final EdcHttpClient httpClient;
    private final TypeManager typeManager;
    private final HttpAccessControlCheckClientConfig config;
    private final HttpAccessControlCheckDtrClientConfig dtrConfig;
    private final LoadingCache<String, String> tokenCache;
    private final Cache<RequestKey, Boolean> accessControlDecisionCache;

    public DtrAccessVerificationClient(
            final Monitor monitor,
            final EdcHttpClient httpClient,
            final Oauth2TokenClient tokenClient,
            final TypeManager typeManager,
            final HttpAccessControlCheckClientConfig config,
            final HttpAccessControlCheckDtrClientConfig dtrConfig) {
        this.monitor = monitor;
        this.httpClient = httpClient;
        this.typeManager = typeManager;
        this.config = config;
        this.dtrConfig = dtrConfig;
        this.tokenCache = Caffeine.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(Duration.ofMinutes(5))
                .refreshAfterWrite(Duration.ofMinutes(4))
                .build(tokenClient::getBearerToken);
        final long cacheDurationMinutes = dtrConfig.getDecisionCacheDurationMinutes();
        if (cacheDurationMinutes <= 0) {
            this.accessControlDecisionCache = null;
        } else {
            this.accessControlDecisionCache = Caffeine.newBuilder()
                    .maximumSize(10000)
                    .expireAfterWrite(Duration.ofMinutes(cacheDurationMinutes))
                    .build();
        }
    }

    @Override
    public boolean isAspectModelCall(final String url) {
        return urlMatchesPattern(url, dtrConfig.getAspectModelUrlPattern());
    }

    @Override
    public boolean shouldAllowAccess(
            final Map<String, String> additionalHeaders,
            final DataFlowRequest request,
            final HttpDataAddress address) throws AccessControlServerException {
        final String requestedUrl = getTargetUrl(request);
        final String bpn = Optional.ofNullable(additionalHeaders.get(HEADER_EDC_BPN))
                .orElseThrow(() -> new AccessControlServerException("Null BPN found."));
        final RequestKey key = new RequestKey(bpn, requestedUrl);
        if (accessControlDecisionCache == null) {
            return this.callDtr(key);
        } else {
            return accessControlDecisionCache.get(
                    key, this::callDtr);
        }
    }

    private boolean callDtr(final RequestKey requestKey) {
        final Request dtrRequest = getDtrRequest(requestKey);
        try (Response response = httpClient.execute(dtrRequest)) {
            return response.isSuccessful();
        } catch (final IOException exception) {
            monitor.severe("Failed to execute DTR access verification request.", exception);
            throw new AccessControlServerException(exception);
        }
    }

    private boolean urlMatchesPattern(final String url, final String urlPattern) {
        final Pattern pattern = Pattern.compile(urlPattern, Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher(url);
        return matcher.find();
    }

    @NotNull
    private String getTargetUrl(final DataFlowRequest request) {
        final var edcBaseUrl = config.getEdcDataPlaneBaseUrl();
        final var path = request.getProperties().getOrDefault("pathSegments", "");
        final var query = Optional.of(request.getProperties().getOrDefault("queryParams", ""))
                .filter(s -> !s.isBlank())
                .map(s -> "?" + s)
                .orElse("");
        return edcBaseUrl + path + query;
    }

    @NotNull
    private Request getDtrRequest(final RequestKey requestKey) {
        final String token = tokenCache.get(dtrConfig.getOauth2TokenScope());
        if (token == null) {
            throw new AccessControlServerException("Token is null.");
        }
        final RequestBody body = RequestBody.create(
                createRequest(requestKey),
                MediaType.get(APPLICATION_JSON));
        return new Request.Builder()
                .url(dtrConfig.getDtrAccessVerificationUrl())
                .addHeader(HEADER_EDC_BPN, requestKey.bpn())
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Accept", APPLICATION_JSON)
                .addHeader("Content-Type", APPLICATION_JSON)
                .post(body)
                .build();
    }

    @NotNull
    private String createRequest(RequestKey requestKey) {
        return typeManager.writeValueAsString(new DtrAccessVerificationRequest(requestKey.requestedUrl()));
    }

}
