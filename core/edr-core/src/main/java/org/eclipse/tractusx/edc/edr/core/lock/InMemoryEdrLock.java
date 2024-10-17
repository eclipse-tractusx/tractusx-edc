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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.lang.Thread.sleep;

public class InMemoryEdrLock implements EndpointDataReferenceLock {

    private static final int LOCK_TIMEOUT = 5000;

    private final EndpointDataReferenceEntryIndex entryIndex;
    private final TransactionContext transactionContext;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Set<String> lockedEdrs = new HashSet<>();

    public InMemoryEdrLock(EndpointDataReferenceEntryIndex entryIndex, TransactionContext transactionContext) {
        this.entryIndex = entryIndex;
        this.transactionContext = transactionContext;
    }

    @Override
    public StoreResult<Boolean> acquireLock(String edrId, DataAddress edr) {
        lock.readLock().lock();
        try {
            if (!lockedEdrs.contains(edrId)) {
                lock.readLock().unlock();
                lock.writeLock().lock();
                var edrEntry = transactionContext.execute(() -> entryIndex.findById(edrId));
                try {
                    if (isExpired(edr, edrEntry) && !lockedEdrs.contains(edrId)) {
                        lockedEdrs.add(edrId);
                        return StoreResult.success(true);
                    }
                } finally {
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }
            }

            var timeout = 0;
            while (lockedEdrs.contains(edrId) && timeout <= LOCK_TIMEOUT) {
                //block until row updated
                try {
                    sleep(LOCK_TIMEOUT);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                timeout += 1000;
            }
            return StoreResult.success(false);

        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void releaseLock(String edrId) {
        lockedEdrs.remove(edrId);
    }
}
