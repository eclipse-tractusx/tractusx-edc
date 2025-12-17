/********************************************************************************
 * Copyright (c) 2025 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.iam.dcp.cache.store;

import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.tractusx.edc.spi.dcp.VerifiablePresentationCacheEntry;
import org.eclipse.tractusx.edc.spi.dcp.VerifiablePresentationCacheStore;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of the {@link VerifiablePresentationCacheStore}. Stores all entries
 * in a concurrent map.
 */
public class InMemoryVerifiablePresentationCacheStore implements VerifiablePresentationCacheStore {

    private final Map<CacheEntryId, VerifiablePresentationCacheEntry> cache = new ConcurrentHashMap<>();

    @Override
    public StoreResult<Void> store(VerifiablePresentationCacheEntry entry) {
        var id = new CacheEntryId(entry.getParticipantContextId(), entry.getCounterPartyDid(), entry.getScopes());
        cache.put(id, entry);
        return StoreResult.success();
    }

    @Override
    public StoreResult<VerifiablePresentationCacheEntry> query(String participantContextId, String counterPartyDid, List<String> scopes) {
        var id = new CacheEntryId(participantContextId, counterPartyDid, scopes);
        var entry = cache.get(id);
        if (entry == null) {
            return StoreResult.notFound("No entry found in cache for given participant and scopes.");
        }

        return StoreResult.success(entry);
    }

    @Override
    public StoreResult<Void> remove(String participantContextId, String counterPartyDid, List<String> scopes) {
        var id = new CacheEntryId(participantContextId, counterPartyDid, scopes);
        cache.remove(id);
        return StoreResult.success();
    }

    @Override
    public StoreResult<Void> remove(String participantContextId, String counterPartyDid) {
        cache.keySet().stream()
                .filter(id -> id.participantContextId.equals(participantContextId) && id.counterPartyDid.equals(counterPartyDid))
                .forEach(cache::remove);

        return StoreResult.success();
    }

    /**
     * Compound ID for cache entries. Comprises participant context ID, participant DID and scopes.
     */
    private static class CacheEntryId {
        private final String participantContextId;
        private final String counterPartyDid;
        private final HashSet<String> scopes;

        CacheEntryId(String participantContextId, String counterPartyDid, List<String> scopes) {
            this.participantContextId = participantContextId;
            this.counterPartyDid = counterPartyDid;
            this.scopes = new HashSet<>(scopes);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            var other = (CacheEntryId) o;
            return this.participantContextId.equals(other.participantContextId) &&
                    this.counterPartyDid.equals(other.counterPartyDid) &&
                    this.scopes.equals(other.scopes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(participantContextId, counterPartyDid, scopes);
        }
    }
}
