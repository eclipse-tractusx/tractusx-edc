/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.did;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.resolution.DidResolver;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of {@link DidResolver} for `did:example` with local cache (for testing)
 */
public class DidExampleResolver implements DidResolver {

    private final Map<String, DidDocument> cache = new HashMap<>();

    public DidExampleResolver() {
    }

    @Override
    public @NotNull String getMethod() {
        return "example";
    }

    @Override
    public @NotNull Result<DidDocument> resolve(String did) {

        var ix = did.indexOf("#");
        if (ix > 0) {
            did = did.substring(0, ix);
        }

        return Optional.ofNullable(cache.get(did))
                .map(Result::success)
                .orElseGet(() -> Result.failure("Failed to fetch did"));
    }

    public void addCached(String did, DidDocument document) {
        cache.put(did, document);
    }
}
