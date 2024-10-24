/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.edr.core.service;

import org.assertj.core.api.Assertions;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.tractusx.edc.edr.spi.index.lock.EndpointDataReferenceLock;
import org.eclipse.tractusx.edc.spi.tokenrefresh.common.TokenRefreshHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_AUTH_NS;
import static org.eclipse.tractusx.edc.edr.spi.types.RefreshMode.AUTO_REFRESH;
import static org.eclipse.tractusx.edc.edr.spi.types.RefreshMode.FORCE_REFRESH;
import static org.eclipse.tractusx.edc.edr.spi.types.RefreshMode.NO_REFRESH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class EdrServiceImplTest {

    private final TokenRefreshHandler tokenRefreshHandler = mock();
    private final EndpointDataReferenceStore edrStore = mock();
    private final EndpointDataReferenceLock edrLock = mock();
    private EdrServiceImpl edrService;

    @BeforeEach
    void setup() {
        edrService = new EdrServiceImpl(edrStore, tokenRefreshHandler, new NoopTransactionContext(), mock(), edrLock);
    }

    @Test
    void query() {

        when(edrStore.query(any())).thenReturn(StoreResult.success(List.of()));

        assertThat(edrService.query(QuerySpec.max())).isSucceeded().satisfies(results -> {
            Assertions.assertThat(results).isEmpty();
        });
    }

    @Test
    void resolveByTransferProcess_whenNoRefresh() {

        var transferProcess = "tp";
        when(edrStore.resolveByTransferProcess(transferProcess)).thenReturn(StoreResult.success(edr()));

        var result = edrService.resolveByTransferProcess(transferProcess, NO_REFRESH);

        assertThat(result).isSucceeded();

        verify(edrStore).resolveByTransferProcess(transferProcess);

        verifyNoMoreInteractions(edrStore);
        verifyNoInteractions(tokenRefreshHandler, edrLock);
    }

    @Test
    void resolveByTransferProcess_whenRefreshNotExpired() {

        var transferProcess = "tp";
        var assetId = "assetId";
        var edrEntry = edrEntry(assetId, transferProcess);
        var edr = edr("1000");
        when(edrStore.resolveByTransferProcess(transferProcess)).thenReturn(StoreResult.success(edr));
        when(edrStore.findById(transferProcess)).thenReturn(edrEntry);
        when(edrLock.isExpired(edr, edrEntry)).thenReturn(false);

        var result = edrService.resolveByTransferProcess(transferProcess, AUTO_REFRESH);

        assertThat(result).isSucceeded();

        verify(edrStore, times(2)).resolveByTransferProcess(transferProcess);
        verify(edrStore).findById(transferProcess);
        verify(edrLock).isExpired(edr, edrEntry);

        verifyNoMoreInteractions(edrStore, edrLock);
        verifyNoInteractions(tokenRefreshHandler);
    }

    @Test
    void resolveByTransferProcess_whenRefreshExpired() {

        var transferProcess = "tp";
        var assetId = "assetId";
        var entry = edrEntry(assetId, transferProcess);
        var expiredEdr = edr("-1000");
        var refreshedEdr = edr();

        when(edrStore.resolveByTransferProcess(transferProcess)).thenReturn(StoreResult.success(expiredEdr));
        when(edrStore.findById(transferProcess)).thenReturn(entry);
        when(edrLock.isExpired(any(), any())).thenReturn(true);
        when(edrLock.acquireLock(any(), any())).thenReturn(StoreResult.success(true));
        when(tokenRefreshHandler.refreshToken(eq(transferProcess), any())).thenReturn(ServiceResult.success(refreshedEdr));
        when(edrStore.save(any(), eq(refreshedEdr))).thenReturn(StoreResult.success());
        when(edrLock.releaseLock(transferProcess)).thenReturn(StoreResult.success());

        var result = edrService.resolveByTransferProcess(transferProcess, AUTO_REFRESH);

        assertThat(result).isSucceeded();

        var captor = ArgumentCaptor.forClass(EndpointDataReferenceEntry.class);
        verify(edrStore).resolveByTransferProcess(transferProcess);
        verify(edrStore).findById(transferProcess);
        verify(edrStore).save(captor.capture(), eq(refreshedEdr));
        verify(tokenRefreshHandler).refreshToken(eq(transferProcess), any());
        verify(edrLock).isExpired(any(), any());
        verify(edrLock).acquireLock(any(), any());
        verify(edrLock).releaseLock(transferProcess);

        verifyNoMoreInteractions(edrStore, tokenRefreshHandler, edrLock);

        Assertions.assertThat(captor.getValue()).usingRecursiveComparison().ignoringFields("createdAt").isEqualTo(entry);
    }

    @Test
    @DisplayName("Resolve an EDR which initially was expired but was refreshed in the meantime")
    void resolveByTransferProcess_whenRefreshExpiredButWasAlreadyRefreshed() {

        var transferProcess = "tp";
        var assetId = "assetId";
        var entry = edrEntry(assetId, transferProcess);
        var expiredEdr = edr("-1000");
        var refreshedEdr = edr();

        when(edrStore.resolveByTransferProcess(transferProcess)).thenReturn(StoreResult.success(expiredEdr));
        when(edrStore.findById(transferProcess)).thenReturn(entry);
        when(edrLock.isExpired(any(), any())).thenReturn(true);
        when(edrLock.acquireLock(any(), any())).thenReturn(StoreResult.success(false));
        when(edrStore.resolveByTransferProcess(transferProcess)).thenReturn(StoreResult.success(refreshedEdr));
        when(edrLock.releaseLock(transferProcess)).thenReturn(StoreResult.success());

        var result = edrService.resolveByTransferProcess(transferProcess, AUTO_REFRESH);

        assertThat(result).isSucceeded();

        verify(edrStore, times(2)).resolveByTransferProcess(transferProcess);
        verify(edrStore).findById(transferProcess);
        verify(edrLock).isExpired(any(), any());
        verify(edrLock).acquireLock(any(), any());
        verify(edrLock).releaseLock(transferProcess);

        Assertions.assertThat(result.getContent()).isEqualTo(refreshedEdr);

        verifyNoMoreInteractions(edrStore, tokenRefreshHandler, edrLock);
    }

    @Test
    void resolveByTransferProcess_forceRefresh() {

        var transferProcess = "tp";
        var assetId = "assetId";
        var entry = edrEntry(assetId, transferProcess);
        var edr = edr("1000");
        var refreshedEdr = edr();

        when(edrStore.resolveByTransferProcess(transferProcess)).thenReturn(StoreResult.success(edr));
        when(edrStore.findById(transferProcess)).thenReturn(entry);
        when(edrLock.isExpired(any(), any())).thenReturn(false);
        when(edrLock.acquireLock(any(), any())).thenReturn(StoreResult.success(false));
        when(tokenRefreshHandler.refreshToken(eq(transferProcess), any())).thenReturn(ServiceResult.success(refreshedEdr));
        when(edrStore.save(any(), eq(refreshedEdr))).thenReturn(StoreResult.success());
        when(edrLock.releaseLock(transferProcess)).thenReturn(StoreResult.success());

        var result = edrService.resolveByTransferProcess(transferProcess, FORCE_REFRESH);

        assertThat(result).isSucceeded();

        var captor = ArgumentCaptor.forClass(EndpointDataReferenceEntry.class);
        verify(edrStore).resolveByTransferProcess(transferProcess);
        verify(edrStore).findById(transferProcess);
        verify(edrStore).save(captor.capture(), eq(refreshedEdr));
        verify(tokenRefreshHandler).refreshToken(eq(transferProcess), any());
        verify(edrLock).isExpired(any(), any());
        verify(edrLock).acquireLock(any(), any());
        verify(edrLock).releaseLock(transferProcess);

        verifyNoMoreInteractions(edrStore, tokenRefreshHandler, edrLock);

        Assertions.assertThat(captor.getValue()).usingRecursiveComparison().ignoringFields("createdAt").isEqualTo(entry);

    }

    private DataAddress edr(String expireIn) {
        return DataAddress.Builder.newInstance().type("test").property(TX_AUTH_NS + "expiresIn", expireIn).build();
    }

    private DataAddress edr() {
        return edr(null);
    }

    private EndpointDataReferenceEntry edrEntry(String assetId, String transferProcessId) {
        return EndpointDataReferenceEntry.Builder.newInstance()
                .assetId(assetId)
                .transferProcessId(transferProcessId)
                .contractNegotiationId(UUID.randomUUID().toString())
                .agreementId(UUID.randomUUID().toString())
                .providerId(UUID.randomUUID().toString())
                .build();
    }
}
