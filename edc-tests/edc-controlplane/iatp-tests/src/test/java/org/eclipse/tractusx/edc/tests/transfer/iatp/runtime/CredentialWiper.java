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

package org.eclipse.tractusx.edc.tests.transfer.iatp.runtime;

import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.tests.runtimes.DataWiper;

/**
 * Extends the functionality of the {@link DataWiper} by also deleting everything from the {@link CredentialStore}
 */
public class CredentialWiper extends DataWiper {
    public CredentialWiper(ServiceExtensionContext context) {
        super(context);
    }

    @Override
    public void clearPersistence() {
        super.clearPersistence();
        if (context.hasService(CredentialStore.class)) {
            var store = context.getService(CredentialStore.class);
            var creds = store.query(QuerySpec.none()).orElseThrow(f -> new RuntimeException(f.getFailureDetail()));
            var hasFailed = creds.stream().map(cred -> store.deleteById(cred.getId()))
                    .anyMatch(StoreResult::failed);

            if (hasFailed) {
                throw new RuntimeException("Could not delete some credentials!");
            }
        }
    }
}
