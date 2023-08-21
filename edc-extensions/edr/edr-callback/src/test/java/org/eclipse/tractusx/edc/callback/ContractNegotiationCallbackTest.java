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

import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationAccepted;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationAgreed;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationEvent;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationInitiated;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationOffered;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationRequested;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationTerminated;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationVerified;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferRequest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.callback.ContractNegotiationCallback.DATA_DESTINATION;
import static org.eclipse.tractusx.edc.callback.TestFunctions.getNegotiationFinalizedEvent;
import static org.eclipse.tractusx.edc.callback.TestFunctions.remoteMessage;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


public class ContractNegotiationCallbackTest {

    TransferProcessService transferProcessService = mock(TransferProcessService.class);

    Monitor monitor = mock(Monitor.class);

    ContractNegotiationCallback callback;

    private static <T extends ContractNegotiationEvent, B extends ContractNegotiationEvent.Builder<T, B>> B baseBuilder(B builder) {
        var callbacks = List.of(CallbackAddress.Builder.newInstance().uri("http://local").events(Set.of("test")).build());
        return builder
                .contractNegotiationId("id")
                .protocol("test")
                .callbackAddresses(callbacks)
                .counterPartyAddress("addr")
                .counterPartyId("provider");
    }

    @BeforeEach
    void setup() {
        callback = new ContractNegotiationCallback(transferProcessService, monitor);
    }

    @Test
    void invoke_shouldStartTransferProcess() {

        var captor = ArgumentCaptor.forClass(TransferRequest.class);

        when(transferProcessService.initiateTransfer(any())).thenReturn(ServiceResult.success(TransferProcess.Builder.newInstance().id("test").build()));

        var event = getNegotiationFinalizedEvent();
        var message = remoteMessage(event);

        var result = callback.invoke(message);

        assertThat(result.succeeded()).isTrue();
        verify(transferProcessService).initiateTransfer(captor.capture());


        var transferRequest = captor.getValue();

        assertThat(transferRequest.getCallbackAddresses()).usingRecursiveFieldByFieldElementComparator().containsAll(event.getCallbackAddresses());

        assertThat(transferRequest).satisfies(tp -> {
            assertThat(tp.getContractId()).isEqualTo(event.getContractAgreement().getId());
            assertThat(tp.getAssetId()).isEqualTo(event.getContractAgreement().getAssetId());
            assertThat(tp.getConnectorAddress()).isEqualTo(event.getCounterPartyAddress());
            assertThat(tp.getConnectorId()).isEqualTo(event.getCounterPartyId());
            assertThat(tp.getProtocol()).isEqualTo(event.getProtocol());
            assertThat(tp.getDataDestination()).usingRecursiveComparison().isEqualTo(DATA_DESTINATION);
        });

    }

    @Test
    void invoke_shouldThrowException_whenTransferRequestFails() {

        when(transferProcessService.initiateTransfer(any())).thenReturn(ServiceResult.badRequest("test"));

        var event = getNegotiationFinalizedEvent();
        var message = remoteMessage(event);


        var result = callback.invoke(message);

        assertThat(result.failed()).isTrue();

    }

    @ParameterizedTest
    @ArgumentsSource(EventInstances.class)
    void invoke_shouldIgnoreOtherEvents(ContractNegotiationEvent event) {
        var message = remoteMessage(event);
        callback.invoke(message);

        verifyNoInteractions(transferProcessService);
    }

    @Test
    void invoke_whenFinalized() {
        var evt = baseBuilder(ContractNegotiationFinalized.Builder.newInstance().contractAgreement(
                ContractAgreement.Builder.newInstance()
                        .providerId("test-provider")
                        .assetId("test-asset")
                        .policy(Policy.Builder.newInstance().build())
                        .id("test-id")
                        .consumerId("test-consumer")
                        .build())).build();
        var message = remoteMessage(evt);
        when(transferProcessService.initiateTransfer(any())).thenReturn(ServiceResult.success(null));

        callback.invoke(message);
        verify(transferProcessService).initiateTransfer(any(TransferRequest.class));
    }

    private static class EventInstances implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    baseBuilder(ContractNegotiationAccepted.Builder.newInstance()).build(),
                    baseBuilder(ContractNegotiationAgreed.Builder.newInstance()).build(),
                    baseBuilder(ContractNegotiationVerified.Builder.newInstance()).build(),
                    baseBuilder(ContractNegotiationInitiated.Builder.newInstance()).build(),
                    baseBuilder(ContractNegotiationOffered.Builder.newInstance()).build(),
                    baseBuilder(ContractNegotiationRequested.Builder.newInstance()).build(),
                    baseBuilder(ContractNegotiationTerminated.Builder.newInstance()).build()
            ).map(Arguments::of);

        }


    }
}
