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
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentation;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.tractusx.edc.spi.dcp.VerifiablePresentationCacheEntry;
import org.eclipse.tractusx.edc.spi.dcp.VerifiablePresentationCacheStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerifiablePresentationCacheImplTest {

    private final String participantContextId = "participantContextId";
    private final String ownDid = "did:web:me";
    private final String counterPartyDid = "did:web:other";
    private final String credentialType = "SomeCredential";
    private final List<String> scopes = List.of(credentialType + ":read");
    private final VerifiablePresentationContainer vpContainer = mock();
    private final VerifiablePresentation vp = mock();
    private final VerifiableCredential vc = mock();
    private final Instant cachedAt = mock();
    private final Instant expiresAt = mock();
    private final Instant vcIssuedAt = mock();
    private final Instant vcExpiresAt = mock();

    private final int cacheValidity = 123;
    private final Clock clock = mock();
    private final VerifiablePresentationCacheStore store = mock();
    private final VerifiableCredentialValidationService validationService = mock();
    private final Function<String, String> didResolver = pcId -> ownDid;
    private final RevocationServiceRegistry revocationServiceRegistry = mock();

    private final VerifiablePresentationCacheImpl cache = new VerifiablePresentationCacheImpl(cacheValidity,
            clock, store, validationService, didResolver, revocationServiceRegistry, mock());

    @BeforeEach
    void setUp() {
        when(vpContainer.presentation()).thenReturn(vp);
        when(vp.getHolder()).thenReturn(counterPartyDid);
        when(vp.getCredentials()).thenReturn(List.of(vc));
        when(vc.getType()).thenReturn(List.of(credentialType));
        when(vc.getIssuanceDate()).thenReturn(vcIssuedAt);
        when(vc.getExpirationDate()).thenReturn(vcExpiresAt);

        when(cachedAt.plus(anyLong(), eq(ChronoUnit.SECONDS))).thenReturn(expiresAt);
        when(expiresAt.isBefore(any())).thenReturn(false);
        when(vcIssuedAt.isAfter(any())).thenReturn(false);
        when(vcExpiresAt.isBefore(any())).thenReturn(false);

        when(store.remove(participantContextId, counterPartyDid, scopes)).thenReturn(StoreResult.success());
        when(validationService.validate(eq(List.of(vpContainer)), eq(ownDid), eq(emptyList()))).thenReturn(Result.success());
        when(revocationServiceRegistry.checkValidity(vc)).thenReturn(Result.success());
    }

    @Test
    void store_validPresentations_returnSuccess() {
        when(store.store(any())).thenReturn(StoreResult.success());

        var result = cache.store(participantContextId, counterPartyDid, scopes, List.of(vpContainer));

        assertThat(result).isSucceeded();
    }

    @Test
    void store_credentialsDoNotMatchScopes_returnFailure() {
        when(store.store(any())).thenReturn(StoreResult.generalError("error"));
        when(vc.getType()).thenReturn(List.of("SomeOtherCredential"));

        var result = cache.store(participantContextId, counterPartyDid, scopes, List.of(vpContainer));

        assertThat(result).isFailed();
    }

    @Test
    void store_presentationIssuerInvalid_returnFailure() {
        when(store.store(any())).thenReturn(StoreResult.generalError("error"));
        when(vp.getHolder()).thenReturn("did:web:notTheSame");

        var result = cache.store(participantContextId, counterPartyDid, scopes, List.of(vpContainer));

        assertThat(result).isFailed();
    }

    @Test
    void store_credentialValidationFails_returnFailure() {
        when(store.store(any())).thenReturn(StoreResult.generalError("error"));
        when(validationService.validate(eq(List.of(vpContainer)), eq(ownDid), eq(emptyList()))).thenReturn(Result.failure("error"));

        var result = cache.store(participantContextId, counterPartyDid, scopes, List.of(vpContainer));

        assertThat(result).isFailed();
    }

    @Test
    void store_storingFails_returnFailure() {
        when(store.store(any())).thenReturn(StoreResult.generalError("error"));

        var result = cache.store(participantContextId, counterPartyDid, scopes, List.of(vpContainer));

        assertThat(result).isFailed();
    }

    @Test
    void query_validCachedEntry_returnFromStore() {
        var entry = cacheEntry();
        var storeResult = StoreResult.success(entry);
        when(store.query(participantContextId, counterPartyDid, scopes)).thenReturn(storeResult);

        var result = cache.query(participantContextId, counterPartyDid, scopes);

        assertThat(result).isSucceeded();
        assertThat(result.getContent())
                .hasSize(1)
                .isEqualTo(entry.getPresentations());
    }

    @Test
    void query_noCachedEntry_returnNotFound() {
        StoreResult<VerifiablePresentationCacheEntry> storeResult = StoreResult.notFound("404");
        when(store.query(participantContextId, counterPartyDid, scopes)).thenReturn(storeResult);

        var result =  cache.query(participantContextId, counterPartyDid, scopes);

        assertThat(result).isFailed();
        assertThat(result.getFailure().getReason()).isEqualTo(NOT_FOUND);
    }

    @Test
    void query_errorQueryingCacheStore_returnNotFound() {
        StoreResult<VerifiablePresentationCacheEntry> storeResult = StoreResult.generalError("error");
        when(store.query(participantContextId, counterPartyDid, scopes)).thenReturn(storeResult);

        var result =  cache.query(participantContextId, counterPartyDid, scopes);

        assertThat(result).isFailed();
        assertThat(result.getFailure().getReason()).isEqualTo(NOT_FOUND);
    }

    @Test
    void query_cachedEntryExpired_returnNotFound() {
        var storeResult = StoreResult.success(cacheEntry());
        when(store.query(participantContextId, counterPartyDid, scopes)).thenReturn(storeResult);
        when(expiresAt.isBefore(any())).thenReturn(true);

        var result =  cache.query(participantContextId, counterPartyDid, scopes);

        assertThat(result).isFailed();
        assertThat(result.getFailure().getReason()).isEqualTo(NOT_FOUND);

        verify(store).remove(participantContextId, counterPartyDid, scopes);
    }

    @Test
    void query_cachedCredentialExpired_returnNotFound() {
        var entry = cacheEntry();
        var storeResult = StoreResult.success(entry);
        when(store.query(participantContextId, counterPartyDid, scopes)).thenReturn(storeResult);
        when(vcExpiresAt.isBefore(any())).thenReturn(true);

        var result =  cache.query(participantContextId, counterPartyDid, scopes);

        assertThat(result).isFailed();
        assertThat(result.getFailure().getReason()).isEqualTo(NOT_FOUND);

        verify(store).remove(participantContextId, counterPartyDid, scopes);
    }

    @Test
    void query_cachedCredentialRevoked_returnNotFound() {
        var entry = cacheEntry();
        var storeResult = StoreResult.success(entry);
        when(store.query(participantContextId, counterPartyDid, scopes)).thenReturn(storeResult);
        when(revocationServiceRegistry.checkValidity(vc)).thenReturn(Result.failure("revoked"));

        var result =  cache.query(participantContextId, counterPartyDid, scopes);

        assertThat(result).isFailed();
        assertThat(result.getFailure().getReason()).isEqualTo(NOT_FOUND);

        verify(store).remove(participantContextId, counterPartyDid, scopes);
    }

    @Test
    void remove_removingFromStoreSuccessful_returnSuccess() {
        when(store.remove(any(), any())).thenReturn(StoreResult.success());

        var result = cache.remove(participantContextId, counterPartyDid);

        assertThat(result).isSucceeded();
    }

    @Test
    void remove_removingFromStoreFails_returnSuccess() {
        when(store.remove(any(), any())).thenReturn(StoreResult.generalError("error"));

        var result = cache.remove(participantContextId, counterPartyDid);

        assertThat(result).isFailed();
    }

    private VerifiablePresentationCacheEntry cacheEntry() {
        return new VerifiablePresentationCacheEntry(participantContextId, counterPartyDid, scopes, List.of(vpContainer), cachedAt);
    }
}
