package org.eclipse.tractusx.edc.iam.dcp;

import org.eclipse.edc.iam.decentralizedclaims.spi.CredentialServiceClient;
import org.eclipse.edc.iam.decentralizedclaims.spi.CredentialServiceUrlResolver;
import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.tractusx.edc.spi.dcp.VerifiablePresentationCache;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CachePresentationRequestServiceTest {

    private String participantContextId = "participantContextId";
    private String ownDid = "did:web:me";
    private String counterPartyDid = "did:web:other";
    private String counterPartyToken = "abc123";
    private List<String> scopes = List.of("scope1", "scope2");

    private SecureTokenService secureTokenService = mock();
    private CredentialServiceUrlResolver credentialServiceUrlResolver = mock();
    private CredentialServiceClient credentialServiceClient = mock();
    private VerifiablePresentationCache cache = mock();
    private Monitor monitor = mock();

    private CachePresentationRequestService service = new CachePresentationRequestService(secureTokenService,
            credentialServiceUrlResolver, credentialServiceClient, cache, monitor);

    @Test
    void requestPresentation_cachedEntryPresent_returnFromCache() {
        StoreResult<List<VerifiablePresentationContainer>> cacheResult = StoreResult.success();
        when(cache.query(participantContextId, counterPartyDid, scopes)).thenReturn(cacheResult);

        var result = service.requestPresentation(participantContextId, ownDid, counterPartyDid, counterPartyToken, scopes);

        assertThat(result).isSucceeded();
        assertThat(result.getContent()).isEqualTo(cacheResult.getContent());
        verify(secureTokenService, never()).createToken(any(), any(), any());
        verify(credentialServiceUrlResolver, never()).resolve(any());
        verify(credentialServiceClient, never()).requestPresentation(any(), any(), isA(List.class));
    }

    @Test
    void requestPresentation_noCachedEntryPresent_requestAndStoreInCache() {
        var credentialServiceUrl = "http://url";

        when(cache.query(participantContextId, counterPartyDid, scopes)).thenReturn(StoreResult.notFound("404"));
        when(cache.store(any(), any(), any(), any())).thenReturn(StoreResult.success());
        when(secureTokenService.createToken(any(), any(), any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().build()));
        when(credentialServiceUrlResolver.resolve(any())).thenReturn(Result.success(credentialServiceUrl));
        when(credentialServiceClient.requestPresentation(any(), any(), isA(List.class))).thenReturn(Result.success(List.of()));

        var result = service.requestPresentation(participantContextId, ownDid, counterPartyDid, counterPartyToken, scopes);

        assertThat(result).isSucceeded();
        verify(secureTokenService).createToken(eq(participantContextId), any(), eq(null));
        verify(credentialServiceUrlResolver).resolve(counterPartyDid);
        verify(credentialServiceClient).requestPresentation(eq(credentialServiceUrl), any(), eq(scopes));
        verify(cache).store(eq(participantContextId), eq(counterPartyDid), eq(scopes), eq(result.getContent()));
    }

    @Test
    void requestPresentation_storingInCacheFails_logWarning() {
        var credentialServiceUrl = "http://url";

        when(cache.query(participantContextId, counterPartyDid, scopes)).thenReturn(StoreResult.notFound("404"));
        when(cache.store(any(), any(), any(), any())).thenReturn(StoreResult.generalError("error"));
        when(secureTokenService.createToken(any(), any(), any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().build()));
        when(credentialServiceUrlResolver.resolve(any())).thenReturn(Result.success(credentialServiceUrl));
        when(credentialServiceClient.requestPresentation(any(), any(), isA(List.class))).thenReturn(Result.success(List.of()));

        var result = service.requestPresentation(participantContextId, ownDid, counterPartyDid, counterPartyToken, scopes);

        assertThat(result).isSucceeded();
        verify(secureTokenService).createToken(eq(participantContextId), any(), eq(null));
        verify(credentialServiceUrlResolver).resolve(counterPartyDid);
        verify(credentialServiceClient).requestPresentation(eq(credentialServiceUrl), any(), eq(scopes));
        verify(cache).store(eq(participantContextId), eq(counterPartyDid), eq(scopes), eq(result.getContent()));
        verify(monitor).warning(anyString());
    }
}
