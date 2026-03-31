/********************************************************************************
 * Copyright (c) 2026 Catena-X Automotive Network e.V.
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

package org.eclipse.tractusx.edc.identity.mapper;

import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsCacheInterface;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class BdrsCache implements BdrsCacheInterface {

    private final Map<UUID, EnumMap<BDRS_CACHE_TYPE, Map<String, String>>> participants = new ConcurrentHashMap<>();

    /**
     * Per-participant cache expiry timestamps.
     */
    private final Map<UUID, Instant> lastCacheUpdates = new ConcurrentHashMap<>();

    /**
     * Cache validity period in seconds.
     */
    private final int cacheValiditySeconds;

    public BdrsCache(int cacheValiditySeconds) {
        this.cacheValiditySeconds = cacheValiditySeconds;
    }

    @Override
    public boolean isCacheExpired(UUID participantContextId) {
        var lastUpdate = lastCacheUpdates.get(participantContextId);
        return lastUpdate == null || lastUpdate.plus(cacheValiditySeconds, ChronoUnit.SECONDS).isBefore(Instant.now());
    }

    @Override
    public void markCacheUpdated(UUID participantContextId) {
        lastCacheUpdates.put(participantContextId, Instant.now());
    }

    @Override
    public EnumMap<BDRS_CACHE_TYPE, Map<String, String>> getAllParticipantCaches(UUID participantContextId) {
        return participants.computeIfAbsent(participantContextId, tenantId -> initializeParticipantCaches());
    }

    /**
     * IMPLEMENTATION SPECIFIC!
     * Creates the two cache directions for a participant.
     * @return the two empty caches {@code }
     */
    private EnumMap<BDRS_CACHE_TYPE, Map<String, String>> initializeParticipantCaches() {
        // Initialize the cache entry enum map for the participant
        var caches = new EnumMap<BDRS_CACHE_TYPE, Map<String, String>>(BDRS_CACHE_TYPE.class);

        // Initialize both caches with empty hash maps
        for (var cacheId : BDRS_CACHE_TYPE.values()) {
            caches.put(cacheId, new HashMap<>());
        }
        return caches;
    }

    @Override
    public void purgeCache(UUID participantContextId, BDRS_CACHE_TYPE cacheId) {
        getAllParticipantCaches(participantContextId).get(cacheId).clear();
    }

    @Override
    public void purgeAllParticipantCaches(UUID participantContextId) {
        var participantContext = participants.get(participantContextId);
        if (participantContext != null) {
            for (var caches : BDRS_CACHE_TYPE.values()) {
                participantContext.get(caches).clear();
            }
        }
    }


    @Override
    public boolean purgeParticipantCaches(UUID participantContextId) {
        return participants.remove(participantContextId) != null;
    }

    @Override
    public boolean hasParticipantCaches(UUID participantContextId) {
        return participants.containsKey(participantContextId);
    }


    @Override
    public String get(UUID participantContextId, BDRS_CACHE_TYPE cacheId, String key) {
        return getAllParticipantCaches(participantContextId).get(cacheId).get(key);
    }

    @Override
    public void put(UUID participantContextId, BDRS_CACHE_TYPE cacheId, String key, String value) {
        getAllParticipantCaches(participantContextId).get(cacheId).put(key, value);
    }

    @Override
    public boolean purge(UUID participantContextId, BDRS_CACHE_TYPE cacheId, String key) {
        return getAllParticipantCaches(participantContextId).get(cacheId).remove(key) != null;
    }

    @Override
    public Map<String, String> getCache(UUID participantContextId, BDRS_CACHE_TYPE cacheId) {
        return getAllParticipantCaches(participantContextId).get(cacheId);
    }

    @Override
    public void setCache(UUID participantContextId, BDRS_CACHE_TYPE cacheId, Map<String, String> cacheData) {
        getAllParticipantCaches(participantContextId).put(cacheId, new HashMap<>(cacheData));
    }
}
