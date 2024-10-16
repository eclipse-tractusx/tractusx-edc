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
import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.tractusx.edc.edr.spi.index.lock.EndpointDataReferenceLock;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_EXPIRES_IN;

public class InMemoryEdrLock implements EndpointDataReferenceLock {

    private final EndpointDataReferenceEntryIndex entryIndex;
    private final TransactionContext transactionContext;
    private final ReentrantLock lock = new ReentrantLock();

    public InMemoryEdrLock(EndpointDataReferenceEntryIndex entryIndex, TransactionContext transactionContext) {
        this.entryIndex = entryIndex;
        this.transactionContext = transactionContext;
    }

    @Override
    public StoreResult<Boolean> acquireLock(String edrId, DataAddress edr) {

        try {
            while (!lock.tryLock(1000, TimeUnit.MILLISECONDS)) {
                //wait until lock can be acquired
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        var edrEntry = transactionContext.execute(() -> entryIndex.findById(edrId));

        if (!isExpired(edr, edrEntry)) {
            return StoreResult.success(false);
        }

        return StoreResult.success(true);
    }

    @Override
    public boolean isExpired(DataAddress edr, EndpointDataReferenceEntry metadata) {
        var expiresInString = edr.getStringProperty(EDR_PROPERTY_EXPIRES_IN);
        if (expiresInString == null) {
            return false;
        }
        var expiresIn = Long.parseLong(expiresInString);
        var expiresAt = metadata.getCreatedAt() / 1000L + expiresIn;
        var expiresAtInstant = Instant.ofEpochSecond(expiresAt);

        return expiresAtInstant.isBefore(Instant.now());
    }

    @Override
    public void releaseLock(String edrId) {
        lock.unlock();
    }
}
