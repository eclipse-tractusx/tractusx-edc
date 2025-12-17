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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CachePresentationRequestServiceTest {

    private final String participantContextId = "participantContextId";
    private final String ownDid = "did:web:me";
    private final String counterPartyDid = "did:web:other";
    private final String counterPartyToken = "abc123";
    private final List<String> scopes = List.of("scope1", "scope2");

    private final SecureTokenService secureTokenService = mock();
    private final CredentialServiceUrlResolver credentialServiceUrlResolver = mock();
    private final CredentialServiceClient credentialServiceClient = mock();
    private final VerifiablePresentationCache cache = mock();
    private final Monitor monitor = mock();

    private final CachePresentationRequestService service = new CachePresentationRequestService(secureTokenService,
            credentialServiceUrlResolver, credentialServiceClient, cache, monitor);

    @Test
    void requestPresentation_cachedEntryPresent_returnFromCache() {
        StoreResult<List<VerifiablePresentationContainer>> cacheResult = StoreResult.success();
        when(cache.query(participantContextId, counterPartyDid, scopes)).thenReturn(cacheResult);

        var result = service.requestPresentation(participantContextId, ownDid, counterPartyDid, counterPartyToken, scopes);

        assertThat(result).isSucceeded();
        assertThat(result.getContent()).isEqualTo(cacheResult.getContent());

        verifyNoInteractions(secureTokenService);
        verifyNoInteractions(credentialServiceUrlResolver);
        verifyNoInteractions(credentialServiceClient);
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
