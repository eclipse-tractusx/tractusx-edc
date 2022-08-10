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

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.catenax.edc.tests.util.DatabaseCleaner;

@RequiredArgsConstructor
public class Connector {

  @NonNull @Getter private final String name;

  @Getter @NonNull private final Environment environment;

  @Getter(lazy = true)
  private final DataManagementAPI dataManagementAPI = loadDataManagementAPI();

  @Getter(lazy = true)
  private final DatabaseCleaner databaseCleaner = loadDatabaseCleaner();

  private DataManagementAPI loadDataManagementAPI() {
    return new DataManagementAPI(
        environment.getDataManagementUrl(), environment.getDataManagementAuthKey());
  }

  private DatabaseCleaner loadDatabaseCleaner() {
    return new DatabaseCleaner(
        environment.getDatabaseUrl(),
        environment.getDatabaseUser(),
        environment.getDatabasePassword());
  }
}
