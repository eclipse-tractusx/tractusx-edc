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

package net.catenax.edc.tests.stepdefs;

import io.cucumber.java.en.Given;
import java.sql.SQLException;
import lombok.NonNull;
import net.catenax.edc.tests.Connector;

public class ConnectorStepDefs {

  @Given("'{connector}' has an empty database")
  public void cleanDatabase(@NonNull final Connector connector) throws SQLException {
    connector.getDatabase().clean();
  }
}
