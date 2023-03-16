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

package org.eclipse.tractusx.edc.cp.adapter.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import org.eclipse.tractusx.edc.cp.adapter.dto.ProcessData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class InMemoryMessageBusTest {
  @Mock Monitor monitor;
  @Mock Listener<DataReferenceRetrievalDto> listener;
  @Mock ListenerService listenerService;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void send_shouldCallListenerOnce() throws InterruptedException {
    // given
    Message<ProcessData> message = new DataReferenceRetrievalDto(null, 3);
    when(listenerService.getListener(any())).thenReturn(listener);
    MessageBus messageBus = new InMemoryMessageBus(monitor, listenerService, 3);

    // when
    messageBus.send(Channel.INITIAL, message);

    // then
    Thread.sleep(50);
    verify(listener, times(1)).process(any(DataReferenceRetrievalDto.class));
  }

  @Test
  public void send_shouldCallListenerWithRetryOnException() throws InterruptedException {
    // given
    Message<ProcessData> message = new DataReferenceRetrievalDto(null, 3);
    when(listenerService.getListener(any())).thenReturn(listener);
    doThrow(new IllegalStateException()).doNothing().when(listener).process(any());
    MessageBus messageBus = new InMemoryMessageBus(monitor, listenerService, 3);

    // when
    messageBus.send(Channel.INITIAL, message);

    // then
    Thread.sleep(1000);
    verify(listener, times(2)).process(any(DataReferenceRetrievalDto.class));
  }
}
