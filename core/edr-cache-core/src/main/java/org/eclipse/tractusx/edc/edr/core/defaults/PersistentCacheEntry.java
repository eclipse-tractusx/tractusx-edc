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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.edr.spi.EndpointDataReferenceEntry;

/**
 * A wrapper to persist {@link EndpointDataReferenceEntry}s and {@link EndpointDataReference}s.
 */
public class PersistentCacheEntry {
    private EndpointDataReferenceEntry edrEntry;
    private EndpointDataReference edr;

    public PersistentCacheEntry(@JsonProperty("edrEntry") EndpointDataReferenceEntry edrEntry, @JsonProperty("edr") EndpointDataReference edr) {
        this.edrEntry = edrEntry;
        this.edr = edr;
    }

    public EndpointDataReferenceEntry getEdrEntry() {
        return edrEntry;
    }

    public EndpointDataReference getEdr() {
        return edr;
    }
}
