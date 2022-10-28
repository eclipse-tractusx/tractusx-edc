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

import static org.eclipse.tractusx.edc.tests.Constants.BACKEND_SERVICE_BACKEND_API_URL;
import static org.eclipse.tractusx.edc.tests.Constants.DATABASE_PASSWORD;
import static org.eclipse.tractusx.edc.tests.Constants.DATABASE_URL;
import static org.eclipse.tractusx.edc.tests.Constants.DATABASE_USER;
import static org.eclipse.tractusx.edc.tests.Constants.DATA_MANAGEMENT_API_AUTH_KEY;
import static org.eclipse.tractusx.edc.tests.Constants.DATA_MANAGEMENT_URL;
import static org.eclipse.tractusx.edc.tests.Constants.DATA_PLANE_URL;
import static org.eclipse.tractusx.edc.tests.Constants.IDS_URL;

import java.util.Locale;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder(access = AccessLevel.PRIVATE)
@Getter
class Environment {
  @NonNull private final String dataManagementAuthKey;
  @NonNull private final String dataManagementUrl;
  @NonNull private final String idsUrl;
  @NonNull private final String dataPlaneUrl;
  @NonNull private final String backendServiceBackendApiUrl;
  @NonNull private final String databaseUrl;
  @NonNull private final String databaseUser;
  @NonNull private final String databasePassword;

  public static Environment byName(String name) {
    name = name.toUpperCase(Locale.ROOT);

    return Environment.builder()
        .dataManagementUrl(System.getenv(String.join("_", name, DATA_MANAGEMENT_URL)))
        .dataManagementAuthKey(System.getenv(String.join("_", name, DATA_MANAGEMENT_API_AUTH_KEY)))
        .idsUrl(System.getenv(String.join("_", name, IDS_URL)))
        .dataPlaneUrl(System.getenv(String.join("_", name, DATA_PLANE_URL)))
        .backendServiceBackendApiUrl(
            System.getenv(String.join("_", name, BACKEND_SERVICE_BACKEND_API_URL)))
        .databaseUrl(System.getenv(String.join("_", name, DATABASE_URL)))
        .databaseUser(System.getenv(String.join("_", name, DATABASE_USER)))
        .databasePassword(System.getenv(String.join("_", name, DATABASE_PASSWORD)))
        .build();
  }
}
