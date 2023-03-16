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

package org.eclipse.tractusx.edc.cp.adapter.process.datareference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import org.eclipse.tractusx.edc.cp.adapter.dto.ProcessData;
import org.eclipse.tractusx.edc.cp.adapter.messaging.Channel;
import org.eclipse.tractusx.edc.cp.adapter.messaging.Message;
import org.eclipse.tractusx.edc.cp.adapter.messaging.MessageBus;
import org.eclipse.tractusx.edc.cp.adapter.service.objectstore.ObjectStoreService;
import org.eclipse.tractusx.edc.cp.adapter.service.objectstore.ObjectStoreServiceInMemory;
import org.eclipse.tractusx.edc.cp.adapter.service.objectstore.ObjectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DataReferenceErrorHandlerTest {
  @Mock Monitor monitor;
  @Mock MessageBus messageBus;
  @Mock TransferProcessService transferProcessService;
  ObjectStoreService storeService = new ObjectStoreServiceInMemory(new ObjectMapper());

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
    storeService.put("key1", ObjectType.DTO, getDto());
    storeService.put("key2", ObjectType.DTO, getDto());
  }

  @Test
  public void validateActiveProcesses_shouldSkipIfNoError() {
    // given
    when(transferProcessService.getState("transferId"))
        .thenReturn(TransferProcessStates.COMPLETED.name());
    DataReferenceErrorHandler errorHandler =
        new DataReferenceErrorHandler(monitor, messageBus, storeService, transferProcessService);

    // when
    errorHandler.validateActiveProcesses();

    // then
    verify(messageBus, times(0)).send(eq(Channel.RESULT), any(Message.class));
  }

  @Test
  public void validateActiveProcesses_shouldHandleErrorReference() {
    // given
    when(transferProcessService.getState("transferId"))
        .thenReturn(TransferProcessStates.COMPLETED.name())
        .thenReturn(TransferProcessStates.ERROR.name());
    DataReferenceErrorHandler errorHandler =
        new DataReferenceErrorHandler(monitor, messageBus, storeService, transferProcessService);

    // when
    errorHandler.validateActiveProcesses();

    // then
    verify(messageBus, times(1)).send(eq(Channel.RESULT), any(Message.class));
  }

  @Test
  public void validateActiveProcesses_shouldHandleCancelledReference() {
    // given
    when(transferProcessService.getState("transferId"))
        .thenReturn(TransferProcessStates.COMPLETED.name())
        .thenReturn(TransferProcessStates.CANCELLED.name());
    DataReferenceErrorHandler errorHandler =
        new DataReferenceErrorHandler(monitor, messageBus, storeService, transferProcessService);

    // when
    errorHandler.validateActiveProcesses();

    // then
    verify(messageBus, times(1)).send(eq(Channel.RESULT), any(Message.class));
  }

  private DataReferenceRetrievalDto getDto() {
    return new DataReferenceRetrievalDto(getProcessData(), 3);
  }

  private ProcessData getProcessData() {
    return ProcessData.builder()
        .assetId("assetId")
        .provider("provider")
        .contractAgreementId("agreementId")
        .transferProcessId("transferId")
        .build();
  }
}
