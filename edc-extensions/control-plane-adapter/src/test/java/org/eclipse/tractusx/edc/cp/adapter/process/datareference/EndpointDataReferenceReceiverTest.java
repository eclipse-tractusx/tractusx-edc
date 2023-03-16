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

import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceReceiver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import org.eclipse.tractusx.edc.cp.adapter.dto.ProcessData;
import org.eclipse.tractusx.edc.cp.adapter.messaging.Channel;
import org.eclipse.tractusx.edc.cp.adapter.messaging.Message;
import org.eclipse.tractusx.edc.cp.adapter.messaging.MessageBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class EndpointDataReferenceReceiverTest {
  @Mock Monitor monitor;
  @Mock MessageBus messageBus;
  @Mock DataRefNotificationSyncService syncService;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void send_shouldNotSendResultWhenMessageNotAvailable() {
    // given
    EndpointDataReferenceReceiver referenceReceiver =
        new EndpointDataReferenceReceiverImpl(monitor, messageBus, syncService);

    // when
    referenceReceiver.send(getEndpointDataReference());

    // then
    verify(messageBus, times(0)).send(eq(Channel.RESULT), any(Message.class));
  }

  @Test
  public void send_shouldSendResultWhenMessageIsAvailable() {
    // given
    DataReferenceRetrievalDto dto = new DataReferenceRetrievalDto(getProcessData(), 3);
    when(syncService.exchangeDataReference(any(), any())).thenReturn(dto);
    EndpointDataReferenceReceiver referenceReceiver =
        new EndpointDataReferenceReceiverImpl(monitor, messageBus, syncService);

    // when
    referenceReceiver.send(getEndpointDataReference());

    // then
    verify(messageBus, times(1)).send(eq(Channel.RESULT), any(Message.class));
    verify(syncService, times(1)).removeDto(any());
  }

  private EndpointDataReference getEndpointDataReference() {
    return EndpointDataReference.Builder.newInstance()
        .endpoint("endpoint")
        .authCode("authCode")
        .authKey("authKey")
        .build();
  }

  private ProcessData getProcessData() {
    return ProcessData.builder().assetId("assetId").provider("provider").build();
  }
}
