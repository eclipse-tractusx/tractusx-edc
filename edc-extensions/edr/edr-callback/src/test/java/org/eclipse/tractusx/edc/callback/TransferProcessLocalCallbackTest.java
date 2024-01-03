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

package org.eclipse.tractusx.edc.callback;

import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessCompleted;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessDeprovisioned;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessEvent;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessProvisioned;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessRequested;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.tractusx.edc.edr.spi.store.EndpointDataReferenceCache;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.EDR_SIMPLE_TYPE;
import static org.eclipse.tractusx.edc.callback.TestFunctions.getEdr;
import static org.eclipse.tractusx.edc.callback.TestFunctions.getTransferProcessStartedEvent;
import static org.eclipse.tractusx.edc.callback.TestFunctions.getTransferTerminatedEvent;
import static org.eclipse.tractusx.edc.callback.TestFunctions.remoteMessage;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates.REFRESHING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


public class TransferProcessLocalCallbackTest {

    TransferProcessStore transferProcessStore = mock();
    EndpointDataReferenceCache edrCache = mock();

    TransactionContext transactionContext = new NoopTransactionContext();

    TransferProcessLocalCallback callback;

    ContractAgreementService agreementService = mock();

    TypeTransformerRegistry transformerRegistry = mock(TypeTransformerRegistry.class);


    @BeforeEach
    void setup() {
        callback = new TransferProcessLocalCallback(edrCache, transferProcessStore, agreementService, transformerRegistry, transactionContext, mock(Monitor.class));
    }

    @Test
    void invoke_shouldStoreTheEdrInCache_whenDataAddressIsPresent() {

        var transferProcessId = "transferProcessId";
        var assetId = "assetId";
        var contractId = "contractId";


        var edr = getEdr();


        var dataRequest = DataRequest.Builder.newInstance().id(edr.getId())
                .destinationType("HttpProxy")
                .assetId(assetId)
                .contractId(contractId)
                .build();

        var transferProcess = TransferProcess.Builder.newInstance()
                .id(transferProcessId)
                .dataRequest(dataRequest)
                .build();

        var edrEntry = EndpointDataReferenceEntry.Builder.newInstance()
                .agreementId(contractId)
                .transferProcessId(transferProcessId)
                .assetId(assetId)
                .build();

        var negotiation = ContractNegotiation.Builder.newInstance()
                .id(contractId)
                .counterPartyId("providerId")
                .counterPartyAddress("http://test")
                .protocol("protocol")
                .build();

        when(transformerRegistry.transform(any(DataAddress.class), eq(EndpointDataReference.class))).thenReturn(Result.success(edr));
        when(transferProcessStore.findForCorrelationId(edr.getId())).thenReturn(transferProcess);
        when(transferProcessStore.findById(transferProcessId)).thenReturn(transferProcess);
        when(agreementService.findNegotiation(contractId)).thenReturn(negotiation);
        when(edrCache.queryForEntries(any())).thenReturn(Stream.of(edrEntry));


        var event = getTransferProcessStartedEvent(DataAddress.Builder.newInstance().type(EDR_SIMPLE_TYPE).build());

        var cacheEntryCaptor = ArgumentCaptor.forClass(EndpointDataReferenceEntry.class);
        var edrCaptor = ArgumentCaptor.forClass(EndpointDataReference.class);
        var message = remoteMessage(event);

        var result = callback.invoke(message);
        assertThat(result.succeeded()).isTrue();

        verify(edrCache).save(cacheEntryCaptor.capture(), edrCaptor.capture());
        verify(edrCache).update(argThat(entry -> entry.getState() == EndpointDataReferenceEntryStates.EXPIRED.code()));

        assertThat(edrCaptor.getValue()).usingRecursiveComparison().isEqualTo(edr);

    }

    @Test
    void invoke_shouldNotFail_whenDataAddressIsAbsent() {

        var event = getTransferProcessStartedEvent();
        var message = remoteMessage(event);

        var result = callback.invoke(message);
        assertThat(result.succeeded()).isTrue();

        verifyNoInteractions(edrCache);
        verifyNoInteractions(transferProcessStore);
    }

    @Test
    void invoke_shouldNotFail_whenTransferProcessNotFound() {

        var transferProcessId = "transferProcessId";

        var edr = getEdr();

        when(transformerRegistry.transform(any(DataAddress.class), eq(EndpointDataReference.class))).thenReturn(Result.success(edr));

        when(transferProcessStore.findForCorrelationId(edr.getId())).thenReturn(null);

        when(transferProcessStore.findById(transferProcessId)).thenReturn(null);

        var event = getTransferProcessStartedEvent(DataAddress.Builder.newInstance().type(EDR_SIMPLE_TYPE).build());
        var message = remoteMessage(event);

        var result = callback.invoke(message);
        assertThat(result.succeeded()).isFalse();

        verifyNoInteractions(edrCache);
    }

    @Test
    void invoke_shouldFail_withInvalidDataAddress() {

        var event = getTransferProcessStartedEvent(DataAddress.Builder.newInstance().type("HttpProxy").build());

        when(transformerRegistry.transform(any(DataAddress.class), eq(EndpointDataReference.class)))
                .thenReturn(Result.failure("failure"));

        var message = remoteMessage(event);

        var result = callback.invoke(message);
        assertThat(result.failed()).isTrue();

        verifyNoInteractions(edrCache);
        verifyNoInteractions(transferProcessStore);
    }

    @Test
    void invoke_shouldStopEdrNegotiation_whenTerminatedMessageReceived() {

        var transferProcessId = "transferProcessId";
        var assetId = "assetId";
        var contractId = "contractId";
        var edr = getEdr();

        var dataRequest = DataRequest.Builder.newInstance().id(edr.getId())
                .destinationType("HttpProxy")
                .assetId(assetId)
                .contractId(contractId)
                .build();

        var transferProcess = TransferProcess.Builder.newInstance()
                .id(transferProcessId)
                .dataRequest(dataRequest)
                .build();

        var edrEntry = EndpointDataReferenceEntry.Builder.newInstance()
                .agreementId(contractId)
                .transferProcessId(transferProcessId)
                .assetId(assetId)
                .state(REFRESHING.code())
                .build();

        when(transferProcessStore.findById(transferProcessId)).thenReturn(transferProcess);
        when(edrCache.queryForEntries(any())).thenReturn(Stream.of(edrEntry));

        var event = getTransferTerminatedEvent(transferProcessId, "Failure");
        var message = remoteMessage(event);

        var result = callback.invoke(message);
        assertThat(result.succeeded()).isTrue();

        verify(edrCache).update(argThat(entry -> entry.getState() == EndpointDataReferenceEntryStates.ERROR.code()));

    }

    @ParameterizedTest
    @ArgumentsSource(EventInstances.class)
    void invoke_shouldIgnoreOtherEvents(TransferProcessEvent event) {
        var message = remoteMessage(event);
        var result = callback.invoke(message);

        assertThat(result.succeeded()).isTrue();

        verifyNoInteractions(edrCache);
    }

    private static class EventInstances implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    baseBuilder(TransferProcessRequested.Builder.newInstance()).build(),
                    baseBuilder(TransferProcessProvisioned.Builder.newInstance()).build(),
                    baseBuilder(TransferProcessCompleted.Builder.newInstance()).build(),
                    baseBuilder(TransferProcessDeprovisioned.Builder.newInstance()).build()
            ).map(Arguments::of);

        }

        private <T extends TransferProcessEvent, B extends TransferProcessEvent.Builder<T, B>> B baseBuilder(B builder) {
            var callbacks = List.of(CallbackAddress.Builder.newInstance().uri("http://local").events(Set.of("test")).build());
            return builder
                    .transferProcessId(UUID.randomUUID().toString())
                    .callbackAddresses(callbacks);
        }
    }
}
