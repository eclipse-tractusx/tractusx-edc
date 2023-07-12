/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.edr.core.defaults;

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.util.concurrency.LockManager;
import org.eclipse.tractusx.edc.edr.spi.store.EndpointDataReferenceCache;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.spi.result.StoreResult.notFound;
import static org.eclipse.edc.spi.result.StoreResult.success;

/**
 * An in-memory, threadsafe implementation of the cache.
 */
public class InMemoryEndpointDataReferenceCache implements EndpointDataReferenceCache {
    private final LockManager lockManager;

    private final EdrCacheEntryPredicateConverter predicateConverter = new EdrCacheEntryPredicateConverter();

    private final Map<String, List<EndpointDataReferenceEntry>> entriesByAssetId;

    private final Map<String, EndpointDataReferenceEntry> entriesByEdrId;
    private final Map<String, EndpointDataReference> edrsByTransferProcessId;

    public InMemoryEndpointDataReferenceCache() {
        lockManager = new LockManager(new ReentrantReadWriteLock());
        entriesByAssetId = new HashMap<>();
        entriesByEdrId = new ConcurrentHashMap<>();
        edrsByTransferProcessId = new HashMap<>();
    }

    @Override
    public @Nullable EndpointDataReference resolveReference(String transferProcessId) {
        return lockManager.readLock(() -> edrsByTransferProcessId.get(transferProcessId));
    }

    @Override
    @NotNull
    public List<EndpointDataReference> referencesForAsset(String assetId, String providerId) {
        var entries = entriesByAssetId.get(assetId);

        Predicate<EndpointDataReferenceEntry> providerIdFilter = (cached) ->
                Optional.ofNullable(providerId)
                        .map(id -> id.equals(cached.getProviderId()))
                        .orElse(true);

        if (entries == null) {
            return emptyList();
        }
        return entries.stream()
                .filter(providerIdFilter)
                .map(e -> resolveReference(e.getTransferProcessId()))
                .filter(Objects::nonNull)
                .collect(toList());
    }

    @Override
    public Stream<EndpointDataReferenceEntry> queryForEntries(QuerySpec spec) {
        return filterBy(spec.getFilterExpression());
    }

    @Override
    public void save(EndpointDataReferenceEntry entry, EndpointDataReference edr) {
        lockManager.writeLock(() -> {
            entriesByEdrId.put(edr.getId(), entry);
            var list = entriesByAssetId.computeIfAbsent(entry.getAssetId(), k -> new ArrayList<>());
            list.add(entry);

            edrsByTransferProcessId.put(entry.getTransferProcessId(), edr);
            return null;
        });
    }

    @Override
    public StoreResult<EndpointDataReferenceEntry> deleteByTransferProcessId(String id) {
        return lockManager.writeLock(() -> {
            var edr = edrsByTransferProcessId.remove(id);
            if (edr == null) {
                return notFound("EDR entry not found for id: " + id);
            }
            var entry = entriesByEdrId.remove(edr.getId());
            var entries = entriesByAssetId.get(entry.getAssetId());
            entries.remove(entry);
            if (entries.isEmpty()) {
                entriesByAssetId.remove(entry.getAssetId());
            }

            return success(entry);
        });
    }

    private Stream<EndpointDataReferenceEntry> filterBy(List<Criterion> criteria) {
        var predicate = criteria.stream()
                .map(predicateConverter::convert)
                .reduce(x -> true, Predicate::and);

        return entriesByEdrId.values().stream()
                .filter(predicate);
    }
}
