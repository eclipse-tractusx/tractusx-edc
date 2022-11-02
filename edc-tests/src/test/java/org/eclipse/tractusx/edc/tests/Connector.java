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

package org.eclipse.tractusx.edc.tests;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.eclipse.tractusx.edc.tests.util.DatabaseCleaner;

@RequiredArgsConstructor
public class Connector {

  @NonNull @Getter private final String name;

  @Getter @NonNull private final Environment environment;

  @Getter(lazy = true)
  private final DataManagementAPI dataManagementAPI = loadDataManagementAPI();

  @Getter(lazy = true)
  private final BackendServiceBackendAPI backendServiceBackendAPI = loadBackendServiceBackendAPI();

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

  private BackendServiceBackendAPI loadBackendServiceBackendAPI() {
    return new BackendServiceBackendAPI(environment.getBackendServiceBackendApiUrl());
  }
}
