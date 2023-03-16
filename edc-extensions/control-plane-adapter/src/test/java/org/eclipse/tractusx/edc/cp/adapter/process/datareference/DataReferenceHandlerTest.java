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

public class DataReferenceHandlerTest {
  @Mock Monitor monitor;
  @Mock MessageBus messageBus;
  @Mock DataRefNotificationSyncService notificationSyncService;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void process_shouldNotSendResultWhenDataReferenceNotAvailable() {
    // given
    DataReferenceHandler dataReferenceHandler =
        new DataReferenceHandler(monitor, messageBus, notificationSyncService);
    DataReferenceRetrievalDto dto = new DataReferenceRetrievalDto(getProcessData(), 3);

    // when
    dataReferenceHandler.process(dto);

    // then
    verify(notificationSyncService, times(1)).exchangeDto(eq(dto), any());
    verify(messageBus, times(0)).send(eq(Channel.RESULT), any(Message.class));
  }

  @Test
  public void process_shouldSendResultWhenDataReferenceIsAvailable() {
    // given
    when(notificationSyncService.exchangeDto(any(), any())).thenReturn(getEndpointDataReference());
    DataReferenceHandler dataReferenceHandler =
        new DataReferenceHandler(monitor, messageBus, notificationSyncService);
    DataReferenceRetrievalDto dto = new DataReferenceRetrievalDto(getProcessData(), 3);

    // when
    dataReferenceHandler.process(dto);

    // then
    verify(messageBus, times(1)).send(eq(Channel.RESULT), any(Message.class));
    verify(notificationSyncService, times(1)).removeDataReference(any());
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
