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
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.iam.AudienceResolver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.GZIPInputStream;

/**
 * Holds a local cache of BPN-to-DID mapping entries. An incoming {@link RemoteMessage} is mapped by looking up the {@link RemoteMessage#getCounterPartyId()}
 * property in that map.
 * <p>
 * The local cache expires after a configurable time, at which point {@link BdrsClient#resolve(RemoteMessage)} requests will hit the server again.
 */
class BdrsClient implements AudienceResolver {
    private static final TypeReference<Map<String, String>> MAP_REF = new TypeReference<>() {
    };
    private final String serverUrl;
    private final int cacheValidity;
    private final EdcHttpClient httpClient;
    private final Monitor monitor;
    private final ObjectMapper mapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private Map<String, String> cache = new HashMap<>();
    private Instant lastCacheUpdate;

    BdrsClient(String baseUrl, int cacheValidity, EdcHttpClient httpClient, Monitor monitor, ObjectMapper mapper) {
        this.serverUrl = baseUrl;
        this.cacheValidity = cacheValidity;
        this.httpClient = httpClient;
        this.monitor = monitor;
        this.mapper = mapper;
    }

    @Override
    public String resolve(RemoteMessage remoteMessage) {
        var bpn = remoteMessage.getCounterPartyId();
        String value;
        lock.readLock().lock();
        try {
            if (isCacheExpired()) {
                lock.readLock().unlock(); // unlock read, acquire write -> "upgrade" lock
                lock.writeLock().lock();
                try {
                    if (isCacheExpired()) {
                        updateCache();
                    }
                } finally {
                    lock.readLock().lock(); // downgrade lock
                    lock.writeLock().unlock();
                }
            }

            value = cache.get(bpn);
        } finally {
            lock.readLock().unlock();
        }
        return value;
    }

    private boolean isCacheExpired() {
        return lastCacheUpdate == null || lastCacheUpdate.plus(cacheValidity, ChronoUnit.SECONDS).isBefore(Instant.now());
    }

    private void updateCache() {
        var request = new Request.Builder()
                //.addHeader("Authorization", createMembershipPresentation()) //todo: add MembershipCredential as JWT-VP to the auth header
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
                }
            } else {
                var msg = "Could not obtain data from BDRS server: code: %d, message: %s".formatted(response.code(), response.message());
                throw new EdcException(msg);
            }
        } catch (IOException e) {
            monitor.severe("Error fetching BDRS data", e);
            throw new EdcException(e);
        }
    }

}
