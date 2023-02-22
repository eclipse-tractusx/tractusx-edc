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

package org.eclipse.tractusx.edc.cp.adapter.store;

import static org.eclipse.edc.sql.SqlQueryExecutor.executeQuery;
import static org.eclipse.edc.sql.SqlQueryExecutor.executeQuerySingle;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.sql.lease.SqlLeaseContextBuilder;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.tractusx.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import org.eclipse.tractusx.edc.cp.adapter.store.model.QueueMessage;
import org.eclipse.tractusx.edc.cp.adapter.store.schema.QueueStatements;

public class SqlQueueStore extends AbstractSqlStore {
  private final QueueStatements statements;
  private final SqlLeaseContextBuilder leaseContext;

  public SqlQueueStore(
      DataSourceRegistry dataSourceRegistry,
      String dataSourceName,
      TransactionContext transactionContext,
      ObjectMapper objectMapper,
      QueueStatements statements,
      String connectorId,
      Clock clock) {
    super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper);
    this.statements = statements;
    leaseContext = SqlLeaseContextBuilder.with(transactionContext, connectorId, statements, clock);
  }

  public void saveMessage(QueueMessage queueMessage) {
    long now = Instant.now().toEpochMilli();
    transactionContext.execute(
        () -> {
          try (var conn = getConnection()) {
            var template = statements.getSaveMessageTemplate();
            executeQuery(
                conn,
                template,
                now,
                UUID.randomUUID().toString(),
                queueMessage.getChannel(),
                toJson(queueMessage.getMessage()),
                queueMessage.getInvokeAfter());
          } catch (SQLException e) {
            e.printStackTrace();
            throw new EdcPersistenceException(e);
          }
        });
  }

  public QueueMessage findById(String id) {
    return transactionContext.execute(
        () -> {
          try (var connection = getConnection()) {
            var sql = statements.getFindByIdTemplate();
            return executeQuerySingle(connection, false, this::mapQueueMessage, sql, id);
          } catch (SQLException e) {
            e.printStackTrace();
            throw new EdcPersistenceException(e);
          }
        });
  }

  public void deleteMessage(String id) {
    transactionContext.execute(
        () -> {
          var existing = findById(id);

          if (existing != null) {
            try (var connection = getConnection()) {
              breakLease(connection, id);
              var stmt = statements.getDeleteTemplate();
              executeQuery(connection, stmt, id);
            } catch (SQLException | IllegalStateException e) {
              e.printStackTrace();
              throw new EdcPersistenceException(e);
            }
          }
        });
  }

  public void updateMessage(QueueMessage queueMessage) {
    transactionContext.execute(
        () -> {
          var existing = findById(queueMessage.getId());

          if (existing != null) {
            try (var connection = getConnection()) {
              var stmt = statements.getUpdateTemplate();
              breakLease(connection, queueMessage.getId());
              executeQuery(
                  connection,
                  stmt,
                  queueMessage.getChannel(),
                  toJson(queueMessage.getMessage()),
                  queueMessage.getInvokeAfter(),
                  queueMessage.getId());
            } catch (SQLException | IllegalStateException e) {
              e.printStackTrace();
              throw new EdcPersistenceException(e);
            }
          }
        });
  }

  public List<QueueMessage> findMessagesToSend(int max) {
    long now = Instant.now().toEpochMilli();
    return transactionContext.execute(
        () -> {
          try (var connection = getConnection()) {
            var sql = statements.getMessagesToSendTemplate();
            Stream<QueueMessage> stream =
                executeQuery(connection, false, this::mapQueueMessage, sql, now, max);
            List<QueueMessage> result =
                stream
                    .map(queueMessage -> getLeasedQueueMessage(connection, queueMessage))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            stream.close();
            return result;
          } catch (SQLException e) {
            e.printStackTrace();
            throw new EdcPersistenceException(e);
          }
        });
  }

  private QueueMessage getLeasedQueueMessage(Connection connection, QueueMessage queueMessage) {
    try {
      acquireLease(connection, queueMessage.getId());
      return queueMessage;
    } catch (IllegalStateException e) {
      return null;
    }
  }

  private void acquireLease(Connection connection, String id) {
    leaseContext.withConnection(connection).acquireLease(id);
  }

  private void breakLease(Connection connection, String id) {
    leaseContext.withConnection(connection).breakLease(id);
  }

  private QueueMessage mapQueueMessage(ResultSet resultSet) throws SQLException {
    return QueueMessage.builder()
        .id(resultSet.getString(statements.getIdColumn()))
        .message(
            fromJson(
                resultSet.getString(statements.getMessageColumn()),
                DataReferenceRetrievalDto.class))
        .invokeAfter(resultSet.getLong(statements.getInvokeAfterColumn()))
        .createdAt(resultSet.getLong(statements.getCreatedAtColumn()))
        .channel(resultSet.getString(statements.getChannelColumn()))
        .build();
  }
}
