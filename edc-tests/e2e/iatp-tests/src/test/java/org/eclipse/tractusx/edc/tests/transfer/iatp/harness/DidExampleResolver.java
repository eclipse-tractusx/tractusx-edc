/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
 ********************************************************************************/

package org.eclipse.tractusx.edc.tests.transfer.iatp.harness;

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
