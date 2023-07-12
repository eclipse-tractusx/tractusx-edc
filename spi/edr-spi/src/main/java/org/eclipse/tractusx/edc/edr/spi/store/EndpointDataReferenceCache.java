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

package org.eclipse.tractusx.edc.edr.spi.store;

import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

/**
 * Caches and resolves {@link EndpointDataReference}s
 */
public interface EndpointDataReferenceCache {

    /**
     * Resolves an {@link EndpointDataReference} for the transfer process, returning null if one does not exist.
     */
    @Nullable
    EndpointDataReference resolveReference(String transferProcessId);

    /**
     * Resolves the {@link EndpointDataReference}s for the asset.
     */
    @NotNull
    List<EndpointDataReference> referencesForAsset(String assetId, String providerId);

    /**
     * Returns all the EDR entries in the store that are covered by a given {@link QuerySpec}.
     */

    Stream<EndpointDataReferenceEntry> queryForEntries(QuerySpec spec);

    /**
     * Saves an {@link EndpointDataReference} to the cache using upsert semantics.
     */
    void save(EndpointDataReferenceEntry entry, EndpointDataReference edr);

    /**
     * Deletes stored endpoint reference data associated with the given transfer process.
     */
    StoreResult<EndpointDataReferenceEntry> deleteByTransferProcessId(String id);

}
