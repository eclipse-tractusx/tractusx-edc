/*
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.validation.businesspartner;

import org.eclipse.tractusx.edc.validation.businesspartner.defaults.InMemoryBusinessPartnerStore;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.BusinessPartnerStore;
import org.eclipse.tractusx.edc.validation.businesspartner.store.BusinessPartnerStoreTestBase;

class InMemoryBusinessPartnerStoreTest extends BusinessPartnerStoreTestBase {


    private final InMemoryBusinessPartnerStore store = new InMemoryBusinessPartnerStore();

    @Override
    protected BusinessPartnerStore getStore() {
        return store;
    }
}