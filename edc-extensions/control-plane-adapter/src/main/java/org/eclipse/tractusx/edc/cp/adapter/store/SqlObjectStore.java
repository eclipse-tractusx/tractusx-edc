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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.sql.store.AbstractSqlStore;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.tractusx.edc.cp.adapter.store.model.ObjectEntity;
import org.eclipse.tractusx.edc.cp.adapter.store.schema.ObjectStoreStatements;

public class SqlObjectStore extends AbstractSqlStore {
  private final ObjectStoreStatements statements;

  public SqlObjectStore(
      DataSourceRegistry dataSourceRegistry,
      String dataSourceName,
      TransactionContext transactionContext,
      ObjectMapper objectMapper,
      ObjectStoreStatements statements) {
    super(dataSourceRegistry, dataSourceName, transactionContext, objectMapper);
    this.statements = statements;
  }

  public void saveMessage(ObjectEntity objectEntity) {
    long now = Instant.now().toEpochMilli();
    transactionContext.execute(
        () -> {
          try (var conn = getConnection()) {
            var template = statements.getSaveObjectTemplate();
            executeQuery(
                conn,
                template,
                objectEntity.getId(),
                now,
                objectEntity.getType(),
                objectEntity.getObject());
          } catch (SQLException e) {
            e.printStackTrace();
            throw new EdcPersistenceException(e);
          }
        });
  }

  public ObjectEntity find(String id, String type) {
    return transactionContext.execute(
        () -> {
          try (var connection = getConnection()) {
            var sql = statements.getFindByIdAndTypeTemplate();
            return executeQuerySingle(connection, false, this::mapObjectEntity, sql, id, type);
          } catch (SQLException e) {
            e.printStackTrace();
            throw new EdcPersistenceException(e);
          }
        });
  }

  public List<ObjectEntity> find(String type) {
    return transactionContext.execute(
        () -> {
          try (var connection = getConnection()) {
            var sql = statements.getFindByTypeTemplate();
            Stream<ObjectEntity> stream =
                executeQuery(connection, false, this::mapObjectEntity, sql, type);
            List<ObjectEntity> result = stream.collect(Collectors.toList());
            stream.close();
            return result;
          } catch (SQLException e) {
            e.printStackTrace();
            throw new EdcPersistenceException(e);
          }
        });
  }

  public void deleteMessage(String id, String type) {
    transactionContext.execute(
        () -> {
          try (var connection = getConnection()) {
            var stmt = statements.getDeleteTemplate();
            executeQuery(connection, stmt, id, type);
          } catch (SQLException | IllegalStateException e) {
            e.printStackTrace();
            throw new EdcPersistenceException(e);
          }
        });
  }

  private ObjectEntity mapObjectEntity(ResultSet resultSet) throws SQLException {
    return ObjectEntity.builder()
        .id(resultSet.getString(statements.getIdColumn()))
        .createdAt(resultSet.getLong(statements.getCreatedAtColumn()))
        .type(resultSet.getString(statements.getTypeColumn()))
        .object(resultSet.getString(statements.getObjectColumn()))
        .build();
  }
}
