/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.edc.tests.util;

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
