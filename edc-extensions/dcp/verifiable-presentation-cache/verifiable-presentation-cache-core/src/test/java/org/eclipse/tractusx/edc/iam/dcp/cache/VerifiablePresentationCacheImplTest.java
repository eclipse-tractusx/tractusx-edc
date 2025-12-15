package org.eclipse.tractusx.edc.iam.dcp.cache;

import org.eclipse.edc.iam.verifiablecredentials.spi.VerifiableCredentialValidationService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.tractusx.edc.spi.dcp.VerifiablePresentationCacheEntry;
import org.eclipse.tractusx.edc.spi.dcp.VerifiablePresentationCacheStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class VerifiablePresentationCacheImplTest {

    private final String participantContextId = "participantContextId";
    private final String ownDid = "did:web:me";
    private final String counterPartyDid = "did:web:other";
    private final List<String> scopes = List.of("scope1", "scope2");
    private final VerifiablePresentationContainer vp = mock();
    private final Instant cachedAt = mock();
    private final Instant now = mock();

    private final int cacheValidity = 123;
    private final Clock clock = mock();
    private final VerifiablePresentationCacheStore store = mock();
    private final VerifiableCredentialValidationService validationService = mock();
    private final Function<String, String> didResolver = pcId -> ownDid;

    private final VerifiablePresentationCacheImpl cache = new VerifiablePresentationCacheImpl(cacheValidity, clock, store, validationService, didResolver);

    @Test
    void store_success_returnSuccess() {
        when(store.store(any())).thenReturn(StoreResult.success());

        var result = cache.store(participantContextId, counterPartyDid, scopes, List.of(vp));

        assertThat(result).isSucceeded();
    }

    @Test
    void store_failure_returnSuccess() {
        when(store.store(any())).thenReturn(StoreResult.generalError("error"));

        var result = cache.store(participantContextId, counterPartyDid, scopes, List.of(vp));

        assertThat(result).isFailed();
    }

    @Test
    void query_validCachedEntry_returnFromStore() {
        var entry = cacheEntry();
        var storeResult = StoreResult.success(entry);
        when(store.query(participantContextId, counterPartyDid, scopes)).thenReturn(storeResult);
        when(cachedAt.plus(anyLong(), eq(ChronoUnit.SECONDS))).thenReturn(now);
        when(now.isBefore(any())).thenReturn(false);
        when(validationService.validate(entry.getPresentations(), ownDid)).thenReturn(Result.success());

        var result = cache.query(participantContextId, counterPartyDid, scopes);

        assertThat(result).isSucceeded();
        assertThat(result.getContent())
                .hasSize(1)
                .isEqualTo(entry.getPresentations());
        verify(validationService).validate(entry.getPresentations(), ownDid);
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
    void query_errorQueryingCache_returnNotFound() {
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
        when(cachedAt.plus(anyLong(), eq(ChronoUnit.SECONDS))).thenReturn(now);
        when(now.isBefore(any())).thenReturn(true);

        var result =  cache.query(participantContextId, counterPartyDid, scopes);

        assertThat(result).isFailed();
        assertThat(result.getFailure().getReason()).isEqualTo(NOT_FOUND);

        verify(store).remove(participantContextId, counterPartyDid, scopes);
        verifyNoInteractions(validationService);
    }

    @Test
    void query_cachedEntryNotValid_returnNotFound() {
        var entry = cacheEntry();
        var storeResult = StoreResult.success(entry);
        when(store.query(participantContextId, counterPartyDid, scopes)).thenReturn(storeResult);
        when(cachedAt.plus(anyLong(), eq(ChronoUnit.SECONDS))).thenReturn(now);
        when(now.isBefore(any())).thenReturn(false);
        when(validationService.validate(entry.getPresentations(), ownDid)).thenReturn(Result.failure("not valid"));

        var result =  cache.query(participantContextId, counterPartyDid, scopes);

        assertThat(result).isFailed();
        assertThat(result.getFailure().getReason()).isEqualTo(NOT_FOUND);

        verify(store).remove(participantContextId, counterPartyDid, scopes);
        verify(validationService).validate(entry.getPresentations(), ownDid);
    }

    @Test
    void remove_successful_returnSuccess() {
        when(store.remove(any(), any())).thenReturn(StoreResult.success());

        var result = cache.remove(participantContextId, counterPartyDid);

        assertThat(result).isSucceeded();
    }

    @Test
    void remove_failure_returnSuccess() {
        when(store.remove(any(), any())).thenReturn(StoreResult.generalError("error"));

        var result = cache.remove(participantContextId, counterPartyDid);

        assertThat(result).isFailed();
    }

    private VerifiablePresentationCacheEntry cacheEntry() {
        return new VerifiablePresentationCacheEntry(participantContextId, counterPartyDid, scopes, List.of(vp), cachedAt);
    }
}
