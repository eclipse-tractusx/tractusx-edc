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

package org.eclipse.tractusx.edc.identity.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Request;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.identitytrust.spi.CredentialServiceClient;
import org.eclipse.edc.iam.identitytrust.spi.SecureTokenService;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.tractusx.edc.TxIatpConstants;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;

import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.JWT_ID;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;

/**
 * Holds a local cache of BPN-to-DID mapping entries.
 * <p>
 * The local cache expires after a configurable time, at which point {@link BdrsClientImpl#resolve(String)}} requests will hit the server again.
 */
class BdrsClientImpl implements BdrsClient {
    private static final TypeReference<Map<String, String>> MAP_REF = new TypeReference<>() {
    };
    private final String serverUrl;
    private final int cacheValidity;
    private final EdcHttpClient httpClient;
    private final Monitor monitor;
    private final ObjectMapper mapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final SecureTokenService secureTokenService;
    private final String ownDid;
    private final Supplier<String> ownCredentialServiceUrl;
    private final CredentialServiceClient credentialServiceClient;
    private Map<String, String> cache = new HashMap<>();
    private Instant lastCacheUpdate;

    BdrsClientImpl(String baseUrl,
                   int cacheValidity,
                   String ownDid,
                   Supplier<String> ownCredentialServiceUrl,
                   EdcHttpClient httpClient,
                   Monitor monitor,
                   ObjectMapper mapper,
                   SecureTokenService secureTokenService,
                   CredentialServiceClient credentialServiceClient) {
        this.serverUrl = baseUrl;
        this.cacheValidity = cacheValidity;
        this.httpClient = httpClient;
        this.monitor = monitor;
        this.mapper = mapper;
        this.secureTokenService = secureTokenService;
        this.ownDid = ownDid;
        this.ownCredentialServiceUrl = ownCredentialServiceUrl;
        this.credentialServiceClient = credentialServiceClient;
    }

    @Override
    public String resolve(String bpn) {
        lock.readLock().lock();
        try {
            if (!isCacheExpired()) {
                return cache.get(bpn);
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            if (isCacheExpired()) {
                updateCache().orElseThrow(f -> new EdcException(f.getFailureDetail()));
            }
            return cache.get(bpn);
        } finally {
            lock.writeLock().unlock();
        }

    }

    private boolean isCacheExpired() {
        return lastCacheUpdate == null || lastCacheUpdate.plus(cacheValidity, ChronoUnit.SECONDS).isBefore(Instant.now());
    }

    private Result<Void> updateCache() {
        var membershipCredToken = createMembershipPresentation();
        if (membershipCredToken.failed()) {
            return membershipCredToken.mapFailure();
        }

        var request = new Request.Builder()
                .addHeader("Authorization", "Bearer " + membershipCredToken.getContent())
                .header("Accept-Encoding", "gzip")
                .url(serverUrl + "/bpn-directory")
                .get()
                .build();
        try (var response = httpClient.execute(request)) {
            if (response.isSuccessful() && response.body() != null) {
                var body = response.body().byteStream();
                try (var gz = new GZIPInputStream(body)) {
                    var bytes = gz.readAllBytes();
                    cache = mapper.readValue(bytes, MAP_REF);
                    lastCacheUpdate = Instant.now();
                    return Result.success();
                }
            } else {
                var msg = "Could not obtain data from BDRS server: code: %d, message: %s".formatted(response.code(), response.message());
                return Result.failure(msg);
            }
        } catch (IOException e) {
            var msg = "Error fetching BDRS data";
            monitor.severe(msg, e);
            return Result.failure(msg);
        }
    }

    private Result<String> createMembershipPresentation() {
        var claims = Map.<String, Object>of(
                JWT_ID, UUID.randomUUID().toString(),
                ISSUER, ownDid,
                SUBJECT, ownDid,
                AUDIENCE, ownDid
        );
        var scope = TxIatpConstants.DEFAULT_MEMBERSHIP_SCOPE;

        return secureTokenService.createToken(claims, scope)
                .compose(sit -> credentialServiceClient.requestPresentation(ownCredentialServiceUrl.get(), sit.getToken(), List.of(scope)))
                .compose(pres -> {
                    if (pres.isEmpty()) {
                        return Result.failure("Expected exactly 1 VP, but was empty");
                    }
                    if (pres.size() != 1) {
                        monitor.warning("Expected exactly 1 VP, but found %d.".formatted(pres.size()));
                    }
                    return Result.success(pres.get(0).rawVp());
                });
    }

}
