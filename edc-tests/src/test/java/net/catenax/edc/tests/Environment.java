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

import java.util.Locale;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Builder(access = AccessLevel.PRIVATE)
@Getter
@ToString
public class Environment {
  private static final String DATA_MANAGEMENT_URL = "DATA_MANAGEMENT_URL";
  private static final String DATA_MANAGEMENT_API_AUTH_KEY = "DATA_MANAGEMENT_API_AUTH_KEY";
  private static final String IDS_URL = "IDS_URL";
  private static final String DATA_PLANE_URL = "DATA_PLANE_URL";
  private static final String BACKEND_SERVICE_BACKEND_API_URL = "BACKEND_SERVICE_BACKEND_API_URL";
  private static final String DATABASE_URL = "DATABASE_URL";
  private static final String DATABASE_USER = "DATABASE_USER";
  private static final String DATABASE_PASSWORD = "DATABASE_PASSWORD";

  @NonNull private final String dataManagementAuthKey;
  @NonNull private final String dataManagementUrl;
  @NonNull private final String idsUrl;
  @NonNull private final String dataPlaneUrl;
  @NonNull private final String backendServiceBackendApiUrl;
  @NonNull private final String databaseUrl;
  @NonNull private final String databaseUser;
  @NonNull private final String databasePassword;

  public static Environment byName(@NonNull final String name) {
    final String prefix = name.toUpperCase(Locale.ROOT);

    return Environment.builder()
        .dataManagementUrl(System.getenv(String.join("_", prefix, DATA_MANAGEMENT_URL)))
        .dataManagementAuthKey(
            System.getenv(String.join("_", prefix, DATA_MANAGEMENT_API_AUTH_KEY)))
        .idsUrl(System.getenv(String.join("_", prefix, IDS_URL)))
        .dataPlaneUrl(System.getenv(String.join("_", prefix, DATA_PLANE_URL)))
        .backendServiceBackendApiUrl(
            System.getenv(String.join("_", prefix, BACKEND_SERVICE_BACKEND_API_URL)))
        .databaseUrl(System.getenv(String.join("_", prefix, DATABASE_URL)))
        .databaseUser(System.getenv(String.join("_", prefix, DATABASE_USER)))
        .databasePassword(System.getenv(String.join("_", prefix, DATABASE_PASSWORD)))
        .build();
  }
}
