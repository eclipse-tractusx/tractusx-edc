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

import org.eclipse.tractusx.edc.edr.spi.EndpointDataReferenceCacheBaseTest;
import org.eclipse.tractusx.edc.edr.spi.store.EndpointDataReferenceCache;

class InMemoryEndpointDataReferenceCacheTest extends EndpointDataReferenceCacheBaseTest {
    private final InMemoryEndpointDataReferenceCache cache = new InMemoryEndpointDataReferenceCache();

    @Override
    protected EndpointDataReferenceCache getStore() {
        return cache;
    }

}
