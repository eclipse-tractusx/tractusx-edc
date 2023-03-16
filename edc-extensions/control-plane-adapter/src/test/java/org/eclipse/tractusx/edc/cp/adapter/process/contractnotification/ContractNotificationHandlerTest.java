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
import static org.mockito.Mockito.*;

import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import org.eclipse.tractusx.edc.cp.adapter.dto.ProcessData;
import org.eclipse.tractusx.edc.cp.adapter.messaging.Channel;
import org.eclipse.tractusx.edc.cp.adapter.messaging.Message;
import org.eclipse.tractusx.edc.cp.adapter.messaging.MessageBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ContractNotificationHandlerTest {
  @Mock Monitor monitor;
  @Mock MessageBus messageBus;
  @Mock ContractNegotiationService contractNegotiationService;
  @Mock ContractNotificationSyncService syncService;
  @Mock DataTransferInitializer dataTransfer;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void process_shouldNotInitiateTransferWhenNoContractNotification() {
    // given
    ContractNotificationHandler contractNotificationHandler =
        new ContractNotificationHandler(
            monitor, messageBus, syncService, contractNegotiationService, dataTransfer);
    DataReferenceRetrievalDto dto = new DataReferenceRetrievalDto(getProcessData(), 3);

    // when
    contractNotificationHandler.process(dto);

    // then
    verify(syncService, times(1)).exchangeDto(any());
    verify(dataTransfer, times(0)).initiate(any());
    verify(messageBus, times(0)).send(eq(Channel.DATA_REFERENCE), any(Message.class));
  }

  @Test
  public void process_shouldInitiateTransferWhenContractConfirmedFromCache() {
    // given
    ContractNotificationHandler contractNotificationHandler =
        new ContractNotificationHandler(
            monitor, messageBus, syncService, contractNegotiationService, dataTransfer);
    DataReferenceRetrievalDto dto = new DataReferenceRetrievalDto(getProcessData(), 3);
    dto.getPayload().setContractConfirmed(true);

    // when
    contractNotificationHandler.process(dto);

    // then
    verify(dataTransfer, times(1)).initiate(any());
    verify(messageBus, times(1)).send(eq(Channel.DATA_REFERENCE), any(Message.class));
  }

  @Test
  public void process_shouldInitiateTransferWhenContractAlreadyConfirmedAtProvider() {
    // given
    when(contractNegotiationService.findbyId(any())).thenReturn(getConfirmedContractNegotiation());
    ContractNotificationHandler contractNotificationHandler =
        new ContractNotificationHandler(
            monitor, messageBus, syncService, contractNegotiationService, dataTransfer);
    DataReferenceRetrievalDto dto = new DataReferenceRetrievalDto(getProcessData(), 3);

    // when
    contractNotificationHandler.process(dto);

    // then
    verify(syncService, times(0)).exchangeDto(any());
    verify(dataTransfer, times(1)).initiate(any());
    verify(messageBus, times(1)).send(eq(Channel.DATA_REFERENCE), any(Message.class));
  }

  @Test
  public void process_shouldInitiateTransferWhenContractConfirmedByNotification() {
    // given
    when(syncService.exchangeDto(any()))
        .thenReturn(
            new ContractInfo("confirmedContractAgreementId", ContractInfo.ContractState.CONFIRMED));
    ContractNotificationHandler contractNotificationHandler =
        new ContractNotificationHandler(
            monitor, messageBus, syncService, contractNegotiationService, dataTransfer);
    DataReferenceRetrievalDto dto = new DataReferenceRetrievalDto(getProcessData(), 3);

    // when
    contractNotificationHandler.process(dto);

    // then
    verify(dataTransfer, times(1)).initiate(any());
    verify(messageBus, times(1)).send(eq(Channel.DATA_REFERENCE), any(Message.class));
    verify(syncService, times(1)).removeContractInfo(any());
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
