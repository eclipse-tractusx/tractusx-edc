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

import static java.util.Objects.isNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.edc.spi.monitor.Monitor;

public class InMemoryMessageBus implements MessageBus {
  private final Monitor monitor;
  private final ListenerService listenerService;
  private final ScheduledExecutorService executorService;

  public InMemoryMessageBus(Monitor monitor, ListenerService listenerService, int threadPoolSize) {
    this.monitor = monitor;
    this.listenerService = listenerService;
    executorService = Executors.newScheduledThreadPool(threadPoolSize);
  }

  @Override
  public void send(Channel name, Message<?> message) {
    if (isNull(message)) {
      monitor.warning(String.format("Message is empty, channel: %s", name));
    } else {
      monitor.info(String.format("[%s] Message sent to channel: %s", message.getTraceId(), name));
      executorService.submit(() -> run(name, message));
    }
  }

  /** Returns 'false' if message processing should be retried. * */
  protected boolean run(Channel name, Message<?> message) {
    try {
      listenerService.getListener(name).process(message);
      message.clearErrors();
      return true;
    } catch (Exception e) {
      monitor.warning(String.format("[%s] Message processing error.", message.getTraceId()), e);
      if (!message.canRetry()) {
        monitor.warning(String.format("[%s] Message reached retry limit!", message.getTraceId()));
        sendMessageToDlq(message, e);
        return true;
      }
      long delayTime = message.unsucceeded();
      executorService.schedule(() -> send(name, message), delayTime, TimeUnit.MILLISECONDS);
      return false;
    }
  }

  private void sendMessageToDlq(Message<?> message, Exception finalException) {
    message.clearErrors();
    message.setFinalException(finalException);
    run(Channel.DLQ, message);
  }
}
