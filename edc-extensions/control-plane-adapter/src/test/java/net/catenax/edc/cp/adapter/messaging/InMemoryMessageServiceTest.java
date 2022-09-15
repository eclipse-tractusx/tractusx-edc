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

package net.catenax.edc.cp.adapter.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class InMemoryMessageServiceTest {
  @Mock Monitor monitor;
  @Mock Listener listener;
  @Mock ListenerService listenerService;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void send_shouldCallListenerOnce() throws InterruptedException {
    // given
    Message message = new Message(null);
    when(listenerService.getListener(any())).thenReturn(listener);
    MessageService messageService = new InMemoryMessageService(monitor, listenerService);

    // when
    messageService.send(Channel.INITIAL, message);

    // then
    Thread.sleep(50);
    verify(listener, times(1)).process(any(Message.class));
  }

  @Test
  public void send_shouldCallListenerWithRetryOnException() throws InterruptedException {
    // given
    Message message = new Message(null);
    when(listenerService.getListener(any())).thenReturn(listener);
    doThrow(new IllegalStateException()).doNothing().when(listener).process(any());
    MessageService messageService = new InMemoryMessageService(monitor, listenerService);

    // when
    messageService.send(Channel.INITIAL, message);

    // then
    Thread.sleep(1000);
    verify(listener, times(2)).process(any(Message.class));
  }
}
