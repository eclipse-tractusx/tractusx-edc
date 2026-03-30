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

package org.eclipse.tractusx.edc.spi.identity.mapper;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 *
 * This is the BDRS cache implementation context aware (based on the participantContext logic from upstream).
 * Also, it has an enabled selector, so that both cache directions (BPN_TO_DID and DID_TO_BPN) can be stored in the same cache instance.
 *
 * Example:
 * <pre>{@code
 *   var cache = new BdrsCache();
 *   UUID tenant = UUID.randomUUID();
 *
 *   cache.put(tenant, BDRS_CACHE.BPN_TO_DID, "BPNL00000000VV12", "did:web:alice");
 *   cache.put(tenant, BDRS_CACHE.DID_TO_BPN, "did:web:alice", "BPNL00000000VV12");
 *
 *   String did = cache.get(tenant, BDRS_CACHE.BPN_TO_DID, "BPNL00000000VV12");   // "did:web:alice"
 *   String bpn = cache.get(tenant, BDRS_CACHE.DID_TO_BPN, "did:web:alice"); // "BPNL00000000VV12"
 * }</pre>
 *
 * Additionally all the cache can be retrieved, replaced or cleared per direction and participant:
 * Example:
 * <pre>{@code
 *  var cache = new BdrsCache();
 *  UUID tenant = UUID.randomUUID();
 *
 *  cache.setCache(tenant, BDRS_CACHE.BPN_TO_DID, Map.of("BPNL00000000VV12", "did:web:alice", "BPNL00000000AT21", "did:web:bob"));
 *
 *  Map<String, String> bpnToDidCache = cache.getCache(tenant, BDRS_CACHE.BPN_TO_DID); // {"BPNL00000000VV12": "did:web:alice", "BPNL00000000AT21": "did:web:bob"}
 *
 * }</pre>
 */
@ExtensionPoint
public interface BdrsCacheInterface {

    /**
     * This is are the selectors for the two cache partitions which is maintained for each participant.
     */
    enum BDRS_CACHE_TYPE {
        BPN_TO_DID,
        DID_TO_BPN
    }

    /**
     * Returns the cache partition for the given participant, initializing the cache if not already present.
     */
    EnumMap<BDRS_CACHE_TYPE, Map<String, String>> getAllParticipantCaches(UUID participantContextId);

    /**
     * Purge all cached data in one direction for a participant.
     *
     * @param participantContextId the tenant scope
     * @param cacheId which direction (BPN_TO_DID, or DID_TO_BPN)
     */
    void purgeCache(UUID participantContextId, BDRS_CACHE_TYPE cacheId);

    /**
     * Purge both directions for a participant, full reset of this participant's cache.
     *
     * @param participantContextId the tenant scope
     */
     void purgeAllParticipantCaches(UUID participantContextId);

    /**
     * Purge the participant from the cache.
     *
     * @param participantContextId the tenant scope
     * @return {@code true} if the participant was present
     */
    boolean purgeParticipantCaches(UUID participantContextId);

    /**
     * Check whether a participant has any caches available.
     *
     * @param participantContextId the tenant scope
     * @return {@code true} if the participant is registered in the cache
     */
    boolean hasParticipantCaches(UUID participantContextId);

    /**
     * Check whether the cache for a participant has expired.
     *
     * @param participantContextId the tenant scope
     * @return {@code true} if the cache is expired or has never been populated
     */
    boolean isCacheExpired(UUID participantContextId);

    /**
     * Mark the cache for a participant as recently updated
     *
     * @param participantContextId the tenant scope
     */
    void markCacheUpdated(UUID participantContextId);

    /**
     * Get a single value.
     *
     * @param participantContextId the tenant scope
     * @param cacheId which direction (BPN_TO_DID, or DID_TO_BPN)
     * @param key the lookup key (bpn or did depending on direction)
     * @return the mapped value, or {@code null}
     */
    String get(UUID participantContextId, BDRS_CACHE_TYPE cacheId, String key);

    /**
     * Store a single entry.
     *
     * @param participantContextId the tenant scope
     * @param cacheId which direction (BPN_TO_DID, or DID_TO_BPN)
     * @param key the lookup key (bpn or did depending on direction)
     * @param value the mapped value (did or bpn depending on direction)
     */
    void put(UUID participantContextId, BDRS_CACHE_TYPE cacheId, String key, String value);

    /**
     * Purge a single entry.
     *
     * @param participantContextId the tenant scope
     * @param cacheId which direction (BPN_TO_DID, or DID_TO_BPN)
     * @param key the lookup key (bpn or did depending on direction)
     * @return {@code true} if the key was present
     */
    boolean purge(UUID participantContextId, BDRS_CACHE_TYPE cacheId, String key);

    /**
     * Get the full cache for one direction of a participant.
     *
     * @param participantContextId the tenant scope
     * @param cacheId which direction (BPN_TO_DID, or DID_TO_BPN)
     * @return the cache map
     */
     Map<String, String> getCache(UUID participantContextId, BDRS_CACHE_TYPE cacheId);

    /**
     * Replace the full map for one direction of a participant.
     *
     * @param participantContextId the tenant scope
     * @param cacheId which direction (BPN_TO_DID, or DID_TO_BPN)
     * @param cacheData the new cache entries
     */
    void setCache(UUID participantContextId, BDRS_CACHE_TYPE cacheId, Map<String, String> cacheData);
}


