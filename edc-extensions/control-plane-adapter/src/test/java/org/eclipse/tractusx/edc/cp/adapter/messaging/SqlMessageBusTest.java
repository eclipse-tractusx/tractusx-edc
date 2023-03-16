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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.tractusx.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import org.eclipse.tractusx.edc.cp.adapter.dto.ProcessData;
import org.eclipse.tractusx.edc.cp.adapter.store.SqlQueueStore;
import org.eclipse.tractusx.edc.cp.adapter.store.model.QueueMessage;
import org.eclipse.tractusx.edc.cp.adapter.store.schema.QueueStatements;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SqlMessageBusTest {
  @Mock Monitor monitor;
  @Mock Listener<DataReferenceRetrievalDto> listener;
  @Mock ListenerService listenerService;
  @Mock SqlQueueStore store;
  @Mock DataSourceRegistry dataSourceRegistry;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void send_shouldCallListenerOnce() throws InterruptedException {
    // given
    Message<ProcessData> message = new DataReferenceRetrievalDto(null, 3);
    when(listenerService.getListener(any())).thenReturn(listener);
    SqlMessageBus messageBus =
        new SqlMessageBus(monitor, listenerService, inMemoryFakeStore(), 2, 10);

    // when
    messageBus.send(Channel.INITIAL, message);
    Thread.sleep(60);
    messageBus.deliverMessages(10);

    // then
    Thread.sleep(60);
    verify(listener, times(1)).process(any(DataReferenceRetrievalDto.class));
  }

  @Test
  public void send_shouldCallListenerWithRetryOnException() throws InterruptedException {
    // given
    Message<ProcessData> message = new DataReferenceRetrievalDto(null, 3);
    when(listenerService.getListener(any())).thenReturn(listener);
    doThrow(new IllegalStateException()).doNothing().when(listener).process(any());
    SqlMessageBus messageBus =
        new SqlMessageBus(monitor, listenerService, inMemoryFakeStore(), 2, 10);

    // when
    messageBus.send(Channel.INITIAL, message);
    messageBus.deliverMessages(10);
    Thread.sleep(60);

    // then
    verify(listener, times(2)).process(any(DataReferenceRetrievalDto.class));
  }

  @Test
  public void send_shouldSendToDlqIfErrorLimitReached() throws InterruptedException {
    // given
    Message<ProcessData> message = new DataReferenceRetrievalDto(null, 3);
    message.setErrorNumber(10);
    when(listenerService.getListener(any())).thenReturn(listener);
    doThrow(new IllegalStateException()).doNothing().when(listener).process(any());
    SqlMessageBus messageBus =
        new SqlMessageBus(monitor, listenerService, inMemoryFakeStore(), 2, 10);

    // when
    messageBus.send(Channel.INITIAL, message);
    Thread.sleep(60);

    // then
    verify(listenerService).getListener(eq(Channel.DLQ));
  }

  private SqlQueueStore inMemoryFakeStore() {
    return new SqlQueueStore(
        dataSourceRegistry,
        "dsname",
        getFakeTransactionContext(),
        new ObjectMapper(),
        getFakeStatements(),
        "cid",
        getFakeClock()) {

      private final Map<String, QueueMessage> map = new HashMap<>();

      @Override
      public void saveMessage(QueueMessage queueMessage) {
        String id = UUID.randomUUID().toString();
        queueMessage.setId(id);
        map.put(id, queueMessage);
      }

      @Override
      public QueueMessage findById(String id) {
        return map.get(id);
      }

      @Override
      public void deleteMessage(String id) {
        map.remove(id);
      }

      @Override
      public void updateMessage(QueueMessage queueMessage) {
        map.remove(queueMessage.getId());
        map.put(queueMessage.getId(), queueMessage);
      }

      @Override
      public List<QueueMessage> findMessagesToSend(int max) {
        return new ArrayList<>(map.values());
      }
    };
  }

  private Clock getFakeClock() {
    return new Clock() {
      @Override
      public ZoneId getZone() {
        return null;
      }

      @Override
      public Clock withZone(ZoneId zone) {
        return null;
      }

      @Override
      public Instant instant() {
        return null;
      }
    };
  }

  @NotNull
  private QueueStatements getFakeStatements() {
    return new QueueStatements() {
      @Override
      public String getAllMessagesTemplate() {
        return null;
      }

      @Override
      public String getMessagesToSendTemplate() {
        return null;
      }

      @Override
      public String getSaveMessageTemplate() {
        return null;
      }

      @Override
      public String getDeleteTemplate() {
        return null;
      }

      @Override
      public String getFindByIdTemplate() {
        return null;
      }

      @Override
      public String getUpdateTemplate() {
        return null;
      }

      @Override
      public String getDeleteLeaseTemplate() {
        return null;
      }

      @Override
      public String getInsertLeaseTemplate() {
        return null;
      }

      @Override
      public String getUpdateLeaseTemplate() {
        return null;
      }

      @Override
      public String getFindLeaseByEntityTemplate() {
        return null;
      }
    };
  }

  private TransactionContext getFakeTransactionContext() {
    return new TransactionContext() {
      @Override
      public void execute(TransactionBlock transactionBlock) {}

      @Override
      public <T> T execute(ResultTransactionBlock<T> resultTransactionBlock) {
        return null;
      }

      @Override
      public void registerSynchronization(TransactionSynchronization transactionSynchronization) {}
    };
  }
}
