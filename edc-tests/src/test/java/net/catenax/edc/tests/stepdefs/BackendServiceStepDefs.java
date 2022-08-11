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
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package net.catenax.edc.tests.stepdefs;

import io.cucumber.java.en.Given;
import lombok.NonNull;
import net.catenax.edc.tests.Connector;
import net.catenax.edc.tests.api.backendservice.BackendServiceBackendApiClient;

public class BackendServiceStepDefs {

  @Given("'{connector}' has an empty backend")
  public void cleanBackendService(@NonNull final Connector connector) {
    final BackendServiceBackendApiClient backendServiceBackendApiClient =
        connector.getBackendServiceBackendApiClient();

    backendServiceBackendApiClient.list("/").forEach(backendServiceBackendApiClient::delete);
  }
}
