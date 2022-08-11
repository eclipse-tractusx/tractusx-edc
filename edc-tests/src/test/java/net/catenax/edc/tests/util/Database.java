/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial Implementation
 *
 */

package net.catenax.edc.tests.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public class Database {
  private static final String SQL_DELETE_FROM_ALL_TABLES =
      String.join(
          System.lineSeparator(),
          "DELETE FROM edc_contract_negotiation;",
          "DELETE FROM edc_contract_agreement;",
          "DELETE FROM edc_transfer_process;",
          "DELETE FROM edc_contract_definitions;",
          "DELETE FROM edc_policydefinitions;",
          "DELETE FROM edc_asset;",
          "DELETE FROM edc_lease;");

  @NonNull private final String url;
  @NonNull private final String user;
  @NonNull private final String password;

  public void clean() throws SQLException {
    try (final Connection connection = getConnection()) {
      final Statement statement = connection.createStatement();
      statement.executeUpdate(SQL_DELETE_FROM_ALL_TABLES);
    }
  }

  @SneakyThrows
  private Connection getConnection() {
    return DriverManager.getConnection(url, user, password);
  }
}
