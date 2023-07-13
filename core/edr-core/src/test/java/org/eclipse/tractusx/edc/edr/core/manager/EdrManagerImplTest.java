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

package org.eclipse.tractusx.edc.edr.core.manager;

import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResourceSet;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.tractusx.edc.edr.spi.store.EndpointDataReferenceCache;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.tractusx.edc.edr.core.EdrCoreExtension.DEFAULT_EXPIRING_DURATION;
import static org.eclipse.tractusx.edc.edr.core.fixtures.TestFunctions.getContractNegotiation;
import static org.eclipse.tractusx.edc.edr.core.fixtures.TestFunctions.getNegotiateEdrRequest;
import static org.eclipse.tractusx.edc.edr.core.manager.EdrManagerImpl.LOCAL_CALLBACK;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates.ERROR;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates.EXPIRED;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates.NEGOTIATED;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates.REFRESHING;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EdrManagerImplTest {

    private final EndpointDataReferenceCache edrCache = mock(EndpointDataReferenceCache.class);
    private final ContractNegotiationService negotiationService = mock(ContractNegotiationService.class);
    private final TransferProcessService transferProcessService = mock(TransferProcessService.class);
    private EdrManagerImpl edrManager;

    @BeforeEach
    void setup() {
        edrManager = EdrManagerImpl.Builder.newInstance()
                .contractNegotiationService(negotiationService)
                .transferProcessService(transferProcessService)
                .edrCache(edrCache)
                .monitor(mock(Monitor.class))
                .expiredRetention(Duration.ofSeconds(1))
                .clock(Clock.systemUTC())
                .build();
    }

    @Test
    @DisplayName("Verify that EDR negotiation is initiated")
    void initEdrNegotiation() {

        var captor = ArgumentCaptor.forClass(ContractRequest.class);

        when(negotiationService.initiateNegotiation(any())).thenReturn(getContractNegotiation());

        var negotiateEdrRequest = getNegotiateEdrRequest();

        var result = edrManager.initiateEdrNegotiation(negotiateEdrRequest);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isNotNull();

        verify(negotiationService).initiateNegotiation(captor.capture());

        var msg = captor.getValue();

        assertThat(msg.getCallbackAddresses()).usingRecursiveFieldByFieldElementComparator().containsAll(negotiateEdrRequest.getCallbackAddresses());
        assertThat(msg.getCallbackAddresses()).usingRecursiveFieldByFieldElementComparator().contains(LOCAL_CALLBACK);
        assertThat(msg.getRequestData().getContractOffer()).usingRecursiveComparison().isEqualTo(negotiateEdrRequest.getOffer());
        assertThat(msg.getRequestData().getProtocol()).isEqualTo(negotiateEdrRequest.getProtocol());
        assertThat(msg.getRequestData().getCounterPartyAddress()).isEqualTo(negotiateEdrRequest.getConnectorAddress());

    }

    @Test
    @DisplayName("Verify that EDR state should transition to REFRESHING")
    void initial_shouldTransitionRequesting() {
        var edrEntry = edrEntryBuilder().state(NEGOTIATED.code()).build();
        var transferProcess = createTransferProcessBuilder().build();
        when(edrCache.nextNotLeased(anyInt(), stateIs(NEGOTIATED.code()))).thenReturn(List.of(edrEntry)).thenReturn(emptyList());
        when(edrCache.findByTransferProcessId(edrEntry.getTransferProcessId())).thenReturn(edrEntry);
        when(transferProcessService.findById(edrEntry.getTransferProcessId())).thenReturn(transferProcess);
        when(transferProcessService.initiateTransfer(any())).thenReturn(ServiceResult.success(transferProcess));

        edrManager.start();

        await().untilAsserted(() -> verify(edrCache).update(argThat(p -> p.getState() == REFRESHING.code())));
    }

    @Test
    @DisplayName("Verify that EDR state should not transition to REFRESHING when the token it's not expired")
    void initial_shouldNotTransitionToRefreshing_WhenNotExpired() {
        var expiration = Instant.now().atOffset(ZoneOffset.UTC).toInstant().plusSeconds(DEFAULT_EXPIRING_DURATION + 10);
        var edrEntry = edrEntryBuilder().expirationTimestamp(expiration.toEpochMilli()).state(NEGOTIATED.code()).build();
        var transferProcess = createTransferProcessBuilder().build();
        when(edrCache.nextNotLeased(anyInt(), stateIs(NEGOTIATED.code())))
                .thenReturn(List.of(edrEntry))
                .thenReturn(List.of(edrEntry))
                .thenReturn(emptyList());

        when(edrCache.findByTransferProcessId(edrEntry.getTransferProcessId())).thenReturn(edrEntry);
        when(transferProcessService.findById(edrEntry.getTransferProcessId())).thenReturn(transferProcess);
        when(transferProcessService.initiateTransfer(any())).thenReturn(ServiceResult.success(transferProcess));

        edrManager.start();

        await().untilAsserted(() -> {
            verify(edrCache, atLeast(2)).nextNotLeased(anyInt(), stateIs(NEGOTIATED.code()));
            verify(edrCache, times(0)).update(argThat(p -> p.getState() == REFRESHING.code()));
        });
    }


    @Test
    @DisplayName("Verify that EDR state should transition to ERROR the transfer process is not found")
    void initial_shouldTransitionError_whenTransferProcessNotFound() {
        var edrEntry = edrEntryBuilder().state(NEGOTIATED.code()).build();
        when(edrCache.nextNotLeased(anyInt(), stateIs(NEGOTIATED.code())))
                .thenReturn(List.of(edrEntry))
                .thenReturn(emptyList());

        when(edrCache.findByTransferProcessId(edrEntry.getTransferProcessId())).thenReturn(edrEntry);
        when(transferProcessService.findById(edrEntry.getTransferProcessId())).thenReturn(null);

        edrManager.start();

        await().untilAsserted(() -> verify(edrCache).update(argThat(p -> p.getState() == ERROR.code())));
    }


    @Test
    @DisplayName("Verify that EDR state should not transition to ERROR on transient errors")
    void initial_shouldNotTransitionError_whenInitiatedTransferFailsOnce() {
        var edrEntry = edrEntryBuilder().state(NEGOTIATED.code()).build();
        var transferProcess = createTransferProcessBuilder().build();

        when(edrCache.nextNotLeased(anyInt(), stateIs(NEGOTIATED.code())))
                .thenReturn(List.of(edrEntry))
                .thenReturn(List.of(edrEntry.copy()))
                .thenReturn(emptyList());

        when(edrCache.findByTransferProcessId(edrEntry.getTransferProcessId())).thenReturn(edrEntry);
        when(transferProcessService.findById(edrEntry.getTransferProcessId())).thenReturn(transferProcess);
        when(transferProcessService.initiateTransfer(any()))
                .thenReturn(ServiceResult.badRequest("bad"))
                .thenReturn(ServiceResult.success(transferProcess));


        edrManager.start();

        await().untilAsserted(() -> {
            var captor = ArgumentCaptor.forClass(EndpointDataReferenceEntry.class);
            verify(edrCache, times(2)).update(captor.capture());
            var states = captor.getAllValues().stream().map(EndpointDataReferenceEntry::getState).toList();
            assertThat(states).containsExactly(NEGOTIATED.code(), REFRESHING.code());
        });
    }

    @Test
    @DisplayName("Verify that EDR is deleted when the retention period is over")
    void initial_shouldDeleteTheEntry_whenTheRetentionPeriodIsOver() {
        var expiration = Instant.now().atOffset(ZoneOffset.UTC).toInstant().minusSeconds(DEFAULT_EXPIRING_DURATION + 10);
        var edrEntry = edrEntryBuilder().state(EXPIRED.code()).expirationTimestamp(expiration.toEpochMilli()).build();

        when(edrCache.nextNotLeased(anyInt(), stateIs(EXPIRED.code())))
                .thenReturn(List.of(edrEntry))
                .thenReturn(emptyList());

        when(edrCache.deleteByTransferProcessId(edrEntry.getTransferProcessId())).thenReturn(StoreResult.success(edrEntry));

        edrManager.start();

        await().untilAsserted(() -> {
            verify(edrCache, times(1)).deleteByTransferProcessId(edrEntry.getTransferProcessId());
        });
    }


    private EndpointDataReferenceEntry.Builder edrEntryBuilder() {
        return EndpointDataReferenceEntry.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .agreementId(UUID.randomUUID().toString())
                .transferProcessId(UUID.randomUUID().toString())
                .expirationTimestamp(Instant.now().toEpochMilli())
                .stateTimestamp(Instant.now().toEpochMilli());
    }

    private TransferProcess.Builder createTransferProcessBuilder() {
        var processId = UUID.randomUUID().toString();
        var dataRequest = createDataRequestBuilder()
                .processId(processId)
                .protocol("protocol")
                .connectorAddress("http://an/address")
                .managedResources(false)
                .build();

        return TransferProcess.Builder.newInstance()
                .provisionedResourceSet(ProvisionedResourceSet.Builder.newInstance().build())
                .type(CONSUMER)
                .id("test-process-" + processId)
                .state(TransferProcessStates.COMPLETED.code())
                .dataRequest(dataRequest);
    }

    private DataRequest.Builder createDataRequestBuilder() {
        return DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contractId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .destinationType("test-type");
    }

    private Criterion[] stateIs(int state) {
        return aryEq(new Criterion[]{ hasState(state) });
    }

}
