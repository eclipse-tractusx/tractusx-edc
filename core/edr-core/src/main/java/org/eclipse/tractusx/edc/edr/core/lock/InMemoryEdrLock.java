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

package org.eclipse.tractusx.edc.edr.core.lock;

import org.eclipse.edc.edr.spi.store.EndpointDataReferenceEntryIndex;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.tractusx.edc.edr.spi.index.lock.EndpointDataReferenceLock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class InMemoryEdrLock implements EndpointDataReferenceLock {

    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();

    private final EndpointDataReferenceEntryIndex entryIndex;
    private final TransactionContext transactionContext;
    private final Map<String, ReentrantReadWriteLock> lockedEdrs = new ConcurrentHashMap<>();

    public InMemoryEdrLock(EndpointDataReferenceEntryIndex entryIndex, TransactionContext transactionContext) {
        this.entryIndex = entryIndex;
        this.transactionContext = transactionContext;
    }

    @Override
    public StoreResult<Boolean> acquireLock(String edrId, DataAddress edr) {
        LOCK.writeLock().lock();
        try {
            var rowLock = lockedEdrs.computeIfAbsent(edrId, k -> new ReentrantReadWriteLock());

            LOCK.writeLock().unlock(); // unlocks global lock before waiting
            rowLock.writeLock().lock();
            LOCK.writeLock().lock();
            // inner try loop for the row-level lock
            var edrEntry = transactionContext.execute(() -> entryIndex.findById(edrId));
            if (isExpired(edr, edrEntry)) {
                return StoreResult.success(true); // expired, should refresh
            }

            releaseLock(edrId);
            return StoreResult.success(false); // not expired, no need to refresh
        } finally {
            LOCK.writeLock().unlock();
        }
    }


    @Override
    public void releaseLock(String edrId) {
        LOCK.writeLock().lock();
        try {
            var reentrantReadWriteLock = lockedEdrs.get(edrId);
            if (reentrantReadWriteLock != null && reentrantReadWriteLock.writeLock().isHeldByCurrentThread()) {
                reentrantReadWriteLock.writeLock().unlock();
                if (!reentrantReadWriteLock.hasQueuedThreads()) {
                    lockedEdrs.remove(edrId);
                }
            }
        } finally {
            LOCK.writeLock().unlock();
        }
    }

}
