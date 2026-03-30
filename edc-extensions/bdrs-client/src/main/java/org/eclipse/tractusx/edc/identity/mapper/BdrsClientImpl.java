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
import org.eclipse.edc.iam.decentralizedclaims.spi.CredentialServiceClient;
import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.tractusx.edc.TxIatpConstants;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.JWT_ID;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;
import static org.eclipse.tractusx.edc.spi.identity.mapper.BdrsCacheInterface.BDRS_CACHE_TYPE;

/**
 * Holds a local, participant-aware cache of BPN-to-DID mapping entries.
 * <p>
 * Each participant (tenant) gets its own cache partition managed by a {@link BdrsCache}.
 * The cache expires after a configurable time per participant, at which point
 * the next resolution request will hit the BDRS server again.
 */
class BdrsClientImpl implements BdrsClient {
    private static final TypeReference<Map<String, String>> MAP_REF = new TypeReference<>() {
    };
    private final String serverUrl;
    private final EdcHttpClient httpClient;
    private final Monitor monitor;
    private final ObjectMapper mapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final SecureTokenService secureTokenService;
    private final String ownDid;
    private final Supplier<String> ownCredentialServiceUrl;
    private final CredentialServiceClient credentialServiceClient;
    private final ParticipantContextSupplier participantContextSupplier;

    /**
     * Participant-aware cache holding both BPN→DID and DID→BPN directions.
     */
    private final BdrsCache cache;

    BdrsClientImpl(String baseUrl,
                   int cacheValidity,
                   String ownDid,
                   Supplier<String> ownCredentialServiceUrl,
                   EdcHttpClient httpClient,
                   Monitor monitor,
                   ObjectMapper mapper,
                   SecureTokenService secureTokenService,
                   CredentialServiceClient credentialServiceClient,
                   ParticipantContextSupplier participantContextSupplier) {
        this.serverUrl = baseUrl;
        this.httpClient = httpClient;
        this.monitor = monitor.withPrefix(getClass().getSimpleName());
        this.mapper = mapper;
        this.secureTokenService = secureTokenService;
        this.ownDid = ownDid;
        this.ownCredentialServiceUrl = ownCredentialServiceUrl;
        this.credentialServiceClient = credentialServiceClient;
        this.participantContextSupplier = participantContextSupplier;
        this.cache = new BdrsCache(cacheValidity);
    }

    @Override
    public String resolveDid(UUID participantContextId, String bpn) {
        lock.readLock().lock();
        try {
            if (!cache.isCacheExpired(participantContextId)) {
                return cache.get(participantContextId, BDRS_CACHE_TYPE.BPN_TO_DID, bpn);
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            if (cache.isCacheExpired(participantContextId)) {
                updateCache(participantContextId).orElseThrow(f -> new EdcException(f.getFailureDetail()));
            }
            return cache.get(participantContextId, BDRS_CACHE_TYPE.BPN_TO_DID, bpn);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String resolveBpn(UUID participantContextId, String did) {
        lock.readLock().lock();
        try {
            if (!cache.isCacheExpired(participantContextId)) {
                return cache.get(participantContextId, BDRS_CACHE_TYPE.DID_TO_BPN, did);
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            if (cache.isCacheExpired(participantContextId)) {
                updateCache(participantContextId).orElseThrow(f -> new EdcException(f.getFailureDetail()));
            }
            return cache.get(participantContextId, BDRS_CACHE_TYPE.DID_TO_BPN, did);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Result<Void> updateCache(UUID participantContextId) {
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
            if (response.isSuccessful()) {
                var body = response.body().byteStream();
                try (var gz = new GZIPInputStream(body)) {
                    var bytes = gz.readAllBytes();
                    Map<String, String> bpnToDidMapping = mapper.readValue(bytes, MAP_REF);

                    // Store BPN→DID direction
                    cache.setCache(participantContextId, BDRS_CACHE_TYPE.BPN_TO_DID, bpnToDidMapping);

                    // Store inverted DID→BPN direction
                    var didToBpnMapping = bpnToDidMapping.entrySet()
                            .stream()
                            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
                    cache.setCache(participantContextId, BDRS_CACHE_TYPE.DID_TO_BPN, didToBpnMapping);

                    cache.markCacheUpdated(participantContextId);
                    return Result.success();
                } catch (Exception e) {
                    var msg = "Failed parsing the BDRS response into the local cache: code: %d, message: %s".formatted(response.code(), response.message());
                    monitor.severe(msg);
                    return Result.failure(msg);
                }
            } else {
                var msg = "Could not obtain data from BDRS server: code: %d, message: %s".formatted(response.code(), response.message());
                monitor.warning(msg);
                return Result.failure(msg);
            }
        } catch (IOException e) {
            var msg = "Error fetching BDRS data: exception: %s".formatted(e.toString());
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
        var scope = TxIatpConstants.MEMBERSHIP_SCOPE;

        return participantContextSupplier.get().map(ParticipantContext::getParticipantContextId)
                .flatMap(result -> {
                    if (result.succeeded()) {
                        return Result.success(result.getContent());
                    } else {
                        monitor.severe("Could not get participant context: " + result.getFailureDetail());
                        return Result.failure(result.getFailureDetail());
                    }
                })
                .compose(id -> secureTokenService.createToken(id, claims, scope))
                .compose(sit -> credentialServiceClient.requestPresentation(ownCredentialServiceUrl.get(), sit.getToken(), List.of(scope)))
                .compose(pres -> {
                    if (pres.isEmpty()) {
                        var msg = "Expected exactly 1 VP, but was empty";
                        monitor.warning(msg);
                        return Result.failure(msg);
                    }
                    if (pres.size() != 1) {
                        monitor.warning("Expected exactly 1 VP, but found %d.".formatted(pres.size()));
                    }
                    return Result.success(pres.get(0).rawVp());
                });
    }

}
