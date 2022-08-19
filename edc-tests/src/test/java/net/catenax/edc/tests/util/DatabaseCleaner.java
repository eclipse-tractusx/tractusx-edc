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
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DatabaseCleaner {

  private static final String SQL =
      "DELETE FROM edc_contract_negotiation;\n"
          + "DELETE FROM edc_contract_agreement;\n"
          + "DELETE FROM edc_transfer_process;\n"
          + "DELETE FROM edc_contract_definitions;\n"
          + "DELETE FROM edc_policydefinitions;\n"
          + "DELETE FROM edc_asset;\n"
          + "DELETE FROM edc_lease;";

  private final String url;
  private final String user;
  private final String password;

  public void run() throws SQLException {
    try (Connection con = DriverManager.getConnection(url, user, password)) {
      Statement st = con.createStatement();
      st.executeUpdate(SQL);
    }
  }
}
