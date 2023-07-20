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

import org.eclipse.edc.spi.persistence.Lease;
import org.eclipse.tractusx.edc.edr.spi.EndpointDataReferenceCacheTestBase;
import org.eclipse.tractusx.edc.edr.spi.store.EndpointDataReferenceCache;
import org.junit.jupiter.api.BeforeEach;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;

class InMemoryEndpointDataReferenceCacheTest extends EndpointDataReferenceCacheTestBase {
    private final HashMap<String, Lease> leases = new HashMap<>();
    private InMemoryEndpointDataReferenceCache cache;

    @BeforeEach
    void setUp() {
        cache = new InMemoryEndpointDataReferenceCache(CONNECTOR_NAME, Clock.systemUTC(), leases);
    }

    @Override
    protected EndpointDataReferenceCache getStore() {
        return cache;
    }

    @Override
    protected void lockEntity(String negotiationId, String owner, Duration duration) {
        leases.put(negotiationId, new Lease(owner, Clock.systemUTC().millis(), duration.toMillis()));
    }

    @Override
    protected boolean isLockedBy(String negotiationId, String owner) {
        return leases.entrySet().stream().anyMatch(e -> e.getKey().equals(negotiationId) &&
                e.getValue().getLeasedBy().equals(owner) &&
                !isExpired(e.getValue()));
    }

    private boolean isExpired(Lease e) {
        return e.getLeasedAt() + e.getLeaseDuration() < Clock.systemUTC().millis();
    }
}
