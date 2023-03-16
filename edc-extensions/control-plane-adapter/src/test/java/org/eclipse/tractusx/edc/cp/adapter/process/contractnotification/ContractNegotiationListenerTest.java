/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 * ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

package org.eclipse.tractusx.edc.cp.adapter.process.contractnotification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationListener;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import org.eclipse.tractusx.edc.cp.adapter.dto.ProcessData;
import org.eclipse.tractusx.edc.cp.adapter.messaging.Channel;
import org.eclipse.tractusx.edc.cp.adapter.messaging.Message;
import org.eclipse.tractusx.edc.cp.adapter.messaging.MessageBus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ContractNegotiationListenerTest {
  @Mock Monitor monitor;
  @Mock MessageBus messageBus;
  @Mock ContractNotificationSyncService syncService;
  @Mock DataTransferInitializer dataTransfer;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void confirmed_shouldNotInitiateTransferIfMessageNotAvailable() {
    // given
    ContractNegotiationListener listener =
        new ContractNegotiationListenerImpl(monitor, messageBus, syncService, dataTransfer);
    ContractNegotiation contractNegotiation = getConfirmedContractNegotiation();

    // when
    listener.confirmed(contractNegotiation);

    // then
    verify(syncService, times(1)).exchangeConfirmedContract(any(), any());
    verify(dataTransfer, times(0)).initiate(any());
    verify(messageBus, times(0)).send(eq(Channel.DATA_REFERENCE), any(Message.class));
  }

  @Test
  public void confirmed_shouldInitiateTransferIfMessageIsAvailable() {
    // given
    when(syncService.exchangeConfirmedContract(any(), any()))
        .thenReturn(new DataReferenceRetrievalDto(getProcessData(), 3));
    verify(dataTransfer, times(0)).initiate(any());

    ContractNegotiationListener listener =
        new ContractNegotiationListenerImpl(monitor, messageBus, syncService, dataTransfer);
    ContractNegotiation contractNegotiation = getConfirmedContractNegotiation();

    // when
    listener.confirmed(contractNegotiation);

    // then
    verify(dataTransfer, times(1)).initiate(any());
    verify(messageBus, times(1)).send(eq(Channel.DATA_REFERENCE), any(Message.class));
  }

  @Test
  public void preDeclined_shouldSendErrorResultIfMessageIsAvailable() {
    // given
    when(syncService.exchangeDeclinedContract(any()))
        .thenReturn(new DataReferenceRetrievalDto(getProcessData(), 3));
    ContractNegotiationListener listener =
        new ContractNegotiationListenerImpl(monitor, messageBus, syncService, dataTransfer);
    ContractNegotiation contractNegotiation = getConfirmedContractNegotiation();

    // when
    listener.declined(contractNegotiation);

    // then
    ArgumentCaptor<DataReferenceRetrievalDto> messageArg =
        ArgumentCaptor.forClass(DataReferenceRetrievalDto.class);
    verify(messageBus, times(1)).send(eq(Channel.RESULT), messageArg.capture());
    Assertions.assertEquals(
        Response.Status.BAD_GATEWAY, messageArg.getValue().getPayload().getErrorStatus());
  }

  @Test
  public void preError_shouldSendErrorResultIfMessageIsAvailable() {
    // given
    when(syncService.exchangeErrorContract(any()))
        .thenReturn(new DataReferenceRetrievalDto(getProcessData(), 3));
    ContractNegotiationListener listener =
        new ContractNegotiationListenerImpl(monitor, messageBus, syncService, dataTransfer);
    ContractNegotiation contractNegotiation = getConfirmedContractNegotiation();

    // when
    listener.failed(contractNegotiation);

    // then
    ArgumentCaptor<DataReferenceRetrievalDto> messageArg =
        ArgumentCaptor.forClass(DataReferenceRetrievalDto.class);
    verify(messageBus, times(1)).send(eq(Channel.RESULT), messageArg.capture());
    Assertions.assertEquals(
        Response.Status.BAD_GATEWAY, messageArg.getValue().getPayload().getErrorStatus());
  }

  private ContractNegotiation getConfirmedContractNegotiation() {
    return ContractNegotiation.Builder.newInstance()
        .state(ContractNegotiationStates.CONFIRMED.code())
        .id("contractNegotiationId")
        .counterPartyId("counterPartyId")
        .counterPartyAddress("counterPartyAddress")
        .protocol("protocol")
        .contractAgreement(
            ContractAgreement.Builder.newInstance()
                .id("contractAgreementId")
                .providerAgentId("providerAgentId")
                .consumerAgentId("consumerAgentId")
                .assetId("assetId")
                .policy(Policy.Builder.newInstance().build())
                .build())
        .build();
  }

  private ProcessData getProcessData() {
    return ProcessData.builder().assetId("assetId").provider("provider").build();
  }
}
