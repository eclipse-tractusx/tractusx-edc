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

import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.persistence.Lease;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.util.concurrency.LockManager;
import org.eclipse.tractusx.edc.edr.spi.store.EndpointDataReferenceCache;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.spi.result.StoreResult.notFound;
import static org.eclipse.edc.spi.result.StoreResult.success;

/**
 * An in-memory, threadsafe implementation of the cache.
 */
public class InMemoryEndpointDataReferenceCache implements EndpointDataReferenceCache {
    private static final long DEFAULT_LEASE_TIME_MILLIS = 60_000;
    private final LockManager lockManager;
    private final EdrCacheEntryPredicateConverter predicateConverter = new EdrCacheEntryPredicateConverter();


    private final Map<String, List<EndpointDataReferenceEntry>> entriesByAssetId;

    private final Map<String, EndpointDataReferenceEntry> entriesByEdrId;

    private final Map<String, EndpointDataReference> edrsByTransferProcessId;
    private final String lockId;

    private final Map<String, Lease> leases;

    private final Clock clock;

    public InMemoryEndpointDataReferenceCache() {
        this(UUID.randomUUID().toString(), Clock.systemUTC(), new ConcurrentHashMap<>());
    }

    public InMemoryEndpointDataReferenceCache(String lockId, Clock clock, Map<String, Lease> leases) {
        this.lockId = lockId;
        lockManager = new LockManager(new ReentrantReadWriteLock());
        entriesByAssetId = new HashMap<>();
        entriesByEdrId = new ConcurrentHashMap<>();
        edrsByTransferProcessId = new HashMap<>();
        this.leases = leases;
        this.clock = clock;
    }

    @Override
    public @Nullable EndpointDataReference resolveReference(String transferProcessId) {
        return lockManager.readLock(() -> edrsByTransferProcessId.get(transferProcessId));
    }

    @Override
    public StoreResult<EndpointDataReferenceEntry> findByIdAndLease(String transferProcessId) {
        return lockManager.readLock(() -> {
            var edr = edrsByTransferProcessId.get(transferProcessId);
            var edrEntry = entriesByEdrId.get(edr.getId());
            return edrEntry == null ? StoreResult.notFound(format("EndpointDataReferenceEntry %s not found", transferProcessId)) :
                    StoreResult.success(edrEntry);
        });
    }

    @Override
    public EndpointDataReferenceEntry findById(String correlationId) {
        return findByIdAndLease(correlationId).orElse(storeFailure -> null);
    }

    @Override
    public void save(EndpointDataReferenceEntry entity) {
        throw new UnsupportedOperationException("Please use save(EndpointDataReferenceEntry, EndpointDataReference) instead!");
    }

    @Override
    @NotNull
    public List<EndpointDataReference> referencesForAsset(String assetId, String providerId) {
        return lockManager.readLock(() -> {
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
                    .filter(this::filterActive)
                    .map(e -> resolveReference(e.getTransferProcessId()))
                    .filter(Objects::nonNull)
                    .collect(toList());

        });
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
    public void update(EndpointDataReferenceEntry entry) {
        lockManager.writeLock(() -> {
            acquireLease(entry.getTransferProcessId(), lockId);
            var edr = edrsByTransferProcessId.get(entry.getTransferProcessId());
            entriesByEdrId.put(edr.getId(), entry);
            var list = entriesByAssetId.computeIfAbsent(entry.getAssetId(), k -> new ArrayList<>());
            list.removeIf((edrEntry) -> edrEntry.getTransferProcessId().equals(entry.getTransferProcessId()));
            list.add(entry);
            freeLease(entry.getTransferProcessId());
            return null;
        });
    }

    @Override
    public StoreResult<EndpointDataReferenceEntry> deleteByTransferProcessId(String id) {
        return lockManager.writeLock(() -> {
            if (isLeased(id)) {
                throw new IllegalStateException("EndpointDataReferenceEntry is leased and cannot be deleted!");
            }
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

    @Override
    public @NotNull List<EndpointDataReferenceEntry> nextNotLeased(int max, Criterion... criteria) {
        return leaseAndGet(max, criteria);
    }


    private @NotNull List<EndpointDataReferenceEntry> leaseAndGet(int max, Criterion... criteria) {
        return lockManager.writeLock(() -> {
            var filterPredicate = Arrays.stream(criteria).map(predicateConverter::convert).reduce(x -> true, Predicate::and);
            var entities = entriesByEdrId.values().stream()
                    .filter(filterPredicate)
                    .filter(e -> !isLeased(e.getId()))
                    .sorted(comparingLong(StatefulEntity::getStateTimestamp)) //order by state timestamp, oldest first
                    .limit(max)
                    .toList();
            entities.forEach(i -> acquireLease(i.getId(), lockId));
            return entities.stream().map(StatefulEntity::copy).collect(toList());
        });
    }

    private Stream<EndpointDataReferenceEntry> filterBy(List<Criterion> criteria) {
        return lockManager.readLock(() -> {
            var predicate = criteria.stream()
                    .map(predicateConverter::convert)
                    .reduce(x -> true, Predicate::and);

            return entriesByEdrId.values().stream()
                    .filter(predicate);
        });

    }

    private void freeLease(String id) {
        leases.remove(id);
    }

    private void acquireLease(String id, String lockId) {
        if (!isLeased(id) || isLeasedBy(id, lockId)) {
            leases.put(id, new Lease(lockId, clock.millis(), DEFAULT_LEASE_TIME_MILLIS));
        } else {
            throw new IllegalStateException("Cannot acquire lease, is already leased by someone else!");
        }
    }

    private boolean isLeased(String id) {
        return leases.containsKey(id) && !leases.get(id).isExpired(clock.millis());
    }

    private boolean isLeasedBy(String id, String lockId) {
        return isLeased(id) && leases.get(id).getLeasedBy().equals(lockId);
    }
}
