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

package net.catenax.edc.tests;

import java.io.Closeable;
import java.io.IOException;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.catenax.edc.tests.api.backendservice.BackendServiceBackendApiClient;
import net.catenax.edc.tests.api.backendservice.BackendServiceBackendApiClientImpl;
import net.catenax.edc.tests.api.datamanagement.DataManagementApiClient;
import net.catenax.edc.tests.api.datamanagement.DataManagementApiClientImpl;
import net.catenax.edc.tests.util.Database;

@Slf4j
@RequiredArgsConstructor
public class Connector implements AutoCloseable {

  @NonNull @Getter private final String name;

  @NonNull @Getter private final Environment environment;

  @Getter(lazy = true)
  private final DataManagementApiClient dataManagementApiClient = loadDataManagementApiClient();

  @Getter(lazy = true)
  private final BackendServiceBackendApiClient backendServiceBackendApiClient =
      loadBackendServiceBackendApiClient();

  @Getter(lazy = true)
  private final Database database = loadDatabase();

  @Override
  public void close() {
    Stream.of(getDataManagementApiClient(), getBackendServiceBackendApiClient())
        .filter(e -> e instanceof Closeable)
        .map(e -> (Closeable) e)
        .forEach(this::closeSilently);
  }

  private DataManagementApiClient loadDataManagementApiClient() {
    return new DataManagementApiClientImpl(
        environment.getDataManagementUrl(), environment.getDataManagementAuthKey());
  }

  private Database loadDatabase() {
    return new Database(
        environment.getDatabaseUrl(),
        environment.getDatabaseUser(),
        environment.getDatabasePassword());
  }

  private BackendServiceBackendApiClient loadBackendServiceBackendApiClient() {
    return new BackendServiceBackendApiClientImpl(environment.getBackendServiceBackendApiUrl());
  }

  private void closeSilently(Closeable closeable) {
    if (closeable == null) {
      return;
    }

    try {
      closeable.close();
    } catch (IOException e) {
      log.warn("Error closing closeable: {}", e.getMessage(), e);
    }
  }
}
