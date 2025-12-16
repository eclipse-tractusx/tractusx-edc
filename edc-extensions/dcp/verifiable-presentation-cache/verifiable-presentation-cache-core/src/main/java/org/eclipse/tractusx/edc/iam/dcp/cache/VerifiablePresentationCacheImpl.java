/********************************************************************************
 * Copyright (c) 2025 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.iam.dcp.cache;

import org.eclipse.edc.iam.verifiablecredentials.spi.VerifiableCredentialValidationService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.tractusx.edc.spi.dcp.VerifiablePresentationCache;
import org.eclipse.tractusx.edc.spi.dcp.VerifiablePresentationCacheEntry;
import org.eclipse.tractusx.edc.spi.dcp.VerifiablePresentationCacheStore;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;

public class VerifiablePresentationCacheImpl implements VerifiablePresentationCache {

    public static final long DEFAULT_VP_CACHE_VALIDITY_SECONDS = 86400;

    private final long cacheValidity;
    private final Clock clock;
    private final VerifiablePresentationCacheStore store;
    private final VerifiableCredentialValidationService credentialValidationService;
    private final Function<String, String> didResolver;

    public VerifiablePresentationCacheImpl(long cacheValidity, Clock clock, VerifiablePresentationCacheStore store,
                                           VerifiableCredentialValidationService credentialValidationService,
                                           Function<String, String> didResolver) {
        this.cacheValidity = cacheValidity;
        this.clock = clock;
        this.store = store;
        this.credentialValidationService = credentialValidationService;
        this.didResolver = didResolver;
    }

    public StoreResult<Void> store(String participantContextId, String counterPartyDid, List<String> scopes,
                                   List<VerifiablePresentationContainer> presentations) {
        var entry = new VerifiablePresentationCacheEntry(participantContextId, counterPartyDid, scopes, presentations, Instant.now(clock));
        return store.store(entry);
    }

    public StoreResult<List<VerifiablePresentationContainer>> query(String participantContextId, String counterPartyDid, List<String> scopes) {
        var cacheResult = store.query(participantContextId, counterPartyDid, scopes);

        if (cacheResult.failed()) {
            return StoreResult.notFound("No cached entry found for given participant and scopes.");
        }

        if (isExpired(cacheResult.getContent()) || !areCredentialsValid(cacheResult.getContent().getPresentations(), participantContextId)) {
            store.remove(participantContextId, counterPartyDid, scopes);
            return StoreResult.notFound("No cached entry found for given participant and scopes.");
        }

        return cacheResult.map(VerifiablePresentationCacheEntry::getPresentations);
    }

    @Override
    public StoreResult<Void> remove(String participantContextId, String counterPartyDid) {
        return store.remove(participantContextId, counterPartyDid);
    }

    private boolean isExpired(VerifiablePresentationCacheEntry entry) {
        return entry.getCachedAt().plus(cacheValidity, ChronoUnit.SECONDS).isBefore(Instant.now(clock));
    }

    private boolean areCredentialsValid(List<VerifiablePresentationContainer> presentations, String participantContextId) {
        var ownDid = didResolver.apply(participantContextId);
        var validationResult = credentialValidationService.validate(presentations, ownDid);
        return validationResult.succeeded();
    }
}
