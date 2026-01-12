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

import org.eclipse.edc.iam.verifiablecredentials.rules.IsInValidityPeriod;
import org.eclipse.edc.iam.verifiablecredentials.rules.IsNotRevoked;
import org.eclipse.edc.iam.verifiablecredentials.spi.VerifiableCredentialValidationService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentation;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.CredentialValidationRule;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.tractusx.edc.spi.dcp.VerifiablePresentationCache;
import org.eclipse.tractusx.edc.spi.dcp.VerifiablePresentationCacheEntry;
import org.eclipse.tractusx.edc.spi.dcp.VerifiablePresentationCacheStore;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

import static java.lang.String.format;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * Implementation of the {@link VerifiablePresentationCache}. Performs common tasks like checking
 * VPs and VCs for validity before caching them as well as checking cached entries for expiry or
 * revocation before returning them. For the actual storing of cache entries, an implementation of
 * {@link VerifiablePresentationCacheStore} is used.
 */
public class VerifiablePresentationCacheImpl implements VerifiablePresentationCache {

    public static final long DEFAULT_VP_CACHE_VALIDITY_SECONDS = 86400; //24h

    private final long cacheValidity;
    private final Clock clock;
    private final VerifiablePresentationCacheStore store;
    private final VerifiableCredentialValidationService credentialValidationService;
    private final UnaryOperator<String> didResolver;
    private final RevocationServiceRegistry revocationServiceRegistry;
    private final Monitor monitor;

    public VerifiablePresentationCacheImpl(long cacheValidity, Clock clock, VerifiablePresentationCacheStore store,
                                           VerifiableCredentialValidationService credentialValidationService,
                                           UnaryOperator<String> didResolver, RevocationServiceRegistry revocationServiceRegistry,
                                           Monitor monitor) {
        this.cacheValidity = cacheValidity;
        this.clock = clock;
        this.store = store;
        this.credentialValidationService = credentialValidationService;
        this.didResolver = didResolver;
        this.revocationServiceRegistry = revocationServiceRegistry;
        this.monitor = monitor.withPrefix(getClass().getSimpleName());
    }

    @Override
    public StoreResult<Void> store(String participantContextId, String counterPartyDid, List<String> scopes,
                                   List<VerifiablePresentationContainer> presentations) {
        if (!areCredentialsValid(presentations, participantContextId, counterPartyDid, scopes)) {
            return StoreResult.generalError("VPs/VCs are not valid. Will not cache.");
        }
        var entry = new VerifiablePresentationCacheEntry(participantContextId, counterPartyDid, scopes, presentations, Instant.now(clock));
        return store.store(entry);
    }

    @Override
    public StoreResult<List<VerifiablePresentationContainer>> query(String participantContextId, String counterPartyDid, List<String> scopes) {
        var cacheResult = store.query(participantContextId, counterPartyDid, scopes);

        if (cacheResult.failed()) {
            var msg = "No cached entry found for given participant and scopes.";
            monitor.debug(msg);
            return StoreResult.notFound(msg);
        }

        if (isExpired(cacheResult.getContent()) || expiredOrRevoked(cacheResult.getContent().getPresentations())) {
            var removeResult = store.remove(participantContextId, counterPartyDid, scopes);
            if (removeResult.failed()) {
                monitor.warning(format("Failed to remove expired or invalid entry from cache for %s: %s", counterPartyDid, removeResult.getFailureDetail()));
            }
            var msg = "VPs/VCs are expired or revoked. Removed from cache.";
            monitor.debug(msg);
            return StoreResult.notFound(msg);
        }

        return cacheResult.map(VerifiablePresentationCacheEntry::getPresentations);
    }

    @Override
    public StoreResult<Void> remove(String participantContextId, String counterPartyDid) {
        return store.remove(participantContextId, counterPartyDid);
    }

    private boolean areCredentialsValid(List<VerifiablePresentationContainer> presentations, String participantContextId, String counterPartyDid, List<String> scopes) {
        var ownDid = didResolver.apply(participantContextId);

        return validateRequestedCredentials(presentations, scopes)
                .compose(ignore -> credentialValidationService.validate(presentations, ownDid, Collections.emptyList()))
                .compose(ignore -> verifyPresentationIssuer(counterPartyDid, presentations))
                .succeeded();
    }

    /**
     * Validates that requested scopes and received VCs match by checking that every requested VC
     * has been received.
     */
    private Result<Void> validateRequestedCredentials(List<VerifiablePresentationContainer> presentations, List<String> requestedScopes) {
        var allCreds = presentations.stream()
                .flatMap(p -> p.presentation().getCredentials().stream())
                .toList();

        var types = allCreds.stream().map(VerifiableCredential::getType)
                .flatMap(Collection::stream)
                .distinct()
                .toList();

        if (requestedScopes.size() > allCreds.size()) {
            var msg = "Number of requested credentials does not match the number of returned credentials";
            monitor.debug(msg + ": requested { %s }, returned { %s }".formatted(String.join(",", requestedScopes),
                    String.join(",", types)));
            return Result.failure(msg);
        }

        if (!requestedScopes.stream().allMatch(scope -> types.stream().anyMatch(scope::contains))) {
            var msg = "Not all requested credentials are present in the presentation response";
            monitor.debug(msg + ": requested { %s }, returned { %s }".formatted(String.join(",", requestedScopes),
                    String.join(",", types)));
            return Result.failure(msg);
        }

        return Result.success();
    }

    /**
     * Validates the issuer of received VPs by checking that the issuer of the received SI token
     * is also the issuer of all received VPs.
     */
    private Result<Void> verifyPresentationIssuer(String expectedIssuer, List<VerifiablePresentationContainer> presentationContainers) {
        var issuers = presentationContainers.stream().map(VerifiablePresentationContainer::presentation)
                .map(VerifiablePresentation::getHolder)
                .toList();

        if (issuers.stream().allMatch(expectedIssuer::equals)) {
            return Result.success();
        } else {
            var msg = "Returned presentations contains invalid issuer. Expected %s found %s".formatted(expectedIssuer, issuers);
            monitor.debug(msg);
            return Result.failure(msg);
        }
    }

    private boolean isExpired(VerifiablePresentationCacheEntry entry) {
        return entry.getCachedAt().plus(cacheValidity, ChronoUnit.SECONDS).isBefore(Instant.now(clock));
    }

    private boolean expiredOrRevoked(List<VerifiablePresentationContainer> presentations) {
        var checks = List.of(
                new IsInValidityPeriod(clock),
                new IsNotRevoked(revocationServiceRegistry));

        var credentials = presentations.stream()
                .flatMap(p -> p.presentation().getCredentials().stream())
                .toList();

        return credentials
                .stream()
                .map(credential -> checks.stream()
                        .reduce(t -> success(), CredentialValidationRule::and)
                        .apply(credential))
                .anyMatch(Result::failed);
    }
}
