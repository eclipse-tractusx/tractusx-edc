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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class InMemoryEdrLock implements EndpointDataReferenceLock {

    private static final int LOCK_TIMEOUT = 10000;
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();

    private final EndpointDataReferenceEntryIndex entryIndex;
    private final TransactionContext transactionContext;
    private final Map<String, ReentrantReadWriteLock> lockedEdrs = new ConcurrentHashMap<>();

    public InMemoryEdrLock(EndpointDataReferenceEntryIndex entryIndex, TransactionContext transactionContext) {
        this.entryIndex = entryIndex;
        this.transactionContext = transactionContext;
    }

    /*
    * This InMemory variant tries to mimic the behaviour of a SELECT WITH FOR UPDATE sql query, which enables a row-level lock.
    * The result is either true if the thread acquiring the lock should refresh the token or false if it was already refreshed by another thread.
    * A map contains the locks for each row, which should be created by the first thread to get the rights to create it.
    * The thread that gets the rights to refresh the EDR should leave this method with a row-level lock,
    * which should be terminated by the same thread upon successful refresh.
    * Another lock is used to synchronize the read and write to the row-level locks map.
    *
    * */
    @Override
    public StoreResult<Boolean> acquireLock(String edrId, DataAddress edr) {

        LOCK.writeLock().lock();
        try {
            var rowLock = lockedEdrs.get(edrId);

            if (rowLock == null) {
                rowLock = lockedEdrs.get(edrId);
                if (rowLock != null) {
                    LOCK.writeLock().unlock();
                    rowLock.writeLock().tryLock(LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
                    LOCK.writeLock().lock(); // gets the write lock again because it might need to unlock it.
                } else {
                    var newRowLock = new ReentrantReadWriteLock();
                    newRowLock.writeLock().lock();
                    lockedEdrs.put(edrId, newRowLock);
                }

                var edrEntry = transactionContext.execute(() -> entryIndex.findById(edrId));
                if (isExpired(edr, edrEntry)) {
                    return StoreResult.success(true); // leaves with the row-level write lock
                } else {
                    lockedEdrs.get(edrId).writeLock().unlock();
                    return StoreResult.success(false);
                }
            }
            LOCK.writeLock().unlock();
            rowLock.writeLock().tryLock(LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
            LOCK.writeLock().lock();


            var edrEntry = transactionContext.execute(() -> entryIndex.findById(edrId));
            if (isExpired(edr, edrEntry)) {
                return StoreResult.success(true); // leaves with the row-level write lock
            } else {
                rowLock.writeLock().unlock();
                return StoreResult.success(false);
            }
        } catch (InterruptedException e) {
            Thread.interrupted();
            throw new IllegalStateException(e);
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    @Override
    public void releaseLock(String edrId) {
        lockedEdrs.get(edrId).writeLock().unlock();
    }
}
