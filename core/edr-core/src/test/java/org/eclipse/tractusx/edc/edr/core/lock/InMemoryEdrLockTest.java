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

import org.eclipse.edc.edr.store.defaults.InMemoryEndpointDataReferenceEntryIndex;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.tractusx.edc.edr.spi.index.lock.EndpointDataReferenceLock;
import org.eclipse.tractusx.edc.edr.spi.testfixtures.index.lock.EndpointDataReferenceLockBaseTest;
import org.junit.jupiter.api.BeforeEach;

@ComponentTest
class InMemoryEdrLockTest extends EndpointDataReferenceLockBaseTest {

    private InMemoryEdrLock edrLock;
    private final TransactionContext transactionContext = new NoopTransactionContext();

    @BeforeEach
    void setUp() {
        var entryIndex = new InMemoryEndpointDataReferenceEntryIndex(CriterionOperatorRegistryImpl.ofDefaults());
        edrLock = new InMemoryEdrLock(entryIndex, transactionContext);
        entryIndex.save(edrEntry("mock", ACQUIRE_LOCK_TP));

    }

    @Override
    protected EndpointDataReferenceLock getStore() {
        return this.edrLock;
    }
}