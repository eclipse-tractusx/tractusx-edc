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

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.cp.adapter.store.SqlQueueStore;
import org.eclipse.tractusx.edc.cp.adapter.store.model.QueueMessage;

public class SqlMessageBus implements MessageBus {
  private final int maxDelivery;
  private final Monitor monitor;
  private final ListenerService listenerService;
  private final ScheduledExecutorService executorService;
  private final SqlQueueStore store;

  public SqlMessageBus(
      Monitor monitor,
      ListenerService listenerService,
      SqlQueueStore sqlQueueStore,
      int threadPoolSize,
      int maxDelivery) {
    this.monitor = monitor;
    this.listenerService = listenerService;
    this.store = sqlQueueStore;
    this.maxDelivery = maxDelivery;
    this.executorService = Executors.newScheduledThreadPool(threadPoolSize);
  }

  @Override
  public void send(Channel channel, Message<?> message) {
    if (isNull(message)) {
      monitor.warning(String.format("Message is empty, channel: %s", channel));
      return;
    }
    monitor.info(String.format("[%s] Message sent to channel: %s", message.getTraceId(), channel));
    long now = Instant.now().toEpochMilli();
    store.saveMessage(
        QueueMessage.builder().channel(channel.name()).message(message).invokeAfter(now).build());

    deliverMessages(maxDelivery);
  }

  public void deliverMessages(int maxElements) {
    List<QueueMessage> queueMessages = store.findMessagesToSend(maxElements);
    monitor.debug(String.format("Found [%d] messages to send.", queueMessages.size()));
    queueMessages.forEach(
        queueMessage -> executorService.submit(() -> deliverMessage(queueMessage)));
  }

  private void deliverMessage(QueueMessage queueMessage) {
    Channel channel = Channel.valueOf(queueMessage.getChannel());
    Message<?> message = queueMessage.getMessage();

    int currentErrorNumber = message.getErrorNumber();
    message.clearErrors();

    try {
      listenerService.getListener(channel).process(message);
      store.deleteMessage(queueMessage.getId());
      monitor.debug(String.format("[%s] Message sent and removed.", queueMessage.getId()));
    } catch (Exception e) {
      monitor.warning(String.format("[%s] Message processing error.", message.getTraceId()), e);
      message.setErrorNumber(currentErrorNumber);
      if (!message.canRetry()) {
        monitor.warning(String.format("[%s] Message reached retry limit!", message.getTraceId()));
        sendMessageToDlq(message, e);
        store.deleteMessage(queueMessage.getId());
        return;
      }
      long delayTime = message.unsucceeded();
      long now = Instant.now().toEpochMilli();
      queueMessage.setInvokeAfter(now + delayTime);
      message.setException(e);
      store.updateMessage(queueMessage);
    }
  }

  private void sendMessageToDlq(Message<?> message, Exception finalException) {
    message.clearErrors();
    message.setFinalException(finalException);
    send(Channel.DLQ, message);
  }
}
