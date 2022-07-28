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

import static net.catenax.edc.tests.Constants.DATA_MANAGEMENT_URL;
import static net.catenax.edc.tests.Constants.DATA_PLANE_URL;
import static net.catenax.edc.tests.Constants.IDS_URL;
import static net.catenax.edc.tests.Constants.PLATO;
import static net.catenax.edc.tests.Constants.SOKRATES;

import java.util.Locale;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder(access = AccessLevel.PRIVATE)
@Getter
class Environment {
  @NonNull private final String dataManagementUrl;
  @NonNull private final String idsUrl;
  @NonNull private final String dataPlaneUrl;

  public static Environment plato() {
    return byName(PLATO);
  }

  public static Environment sokrates() {
    return byName(SOKRATES);
  }

  public static Environment byName(String name) {
    name = name.toUpperCase(Locale.ROOT);

    return Environment.builder()
        .dataManagementUrl(System.getenv(String.join("_", name, DATA_MANAGEMENT_URL)))
        .idsUrl(System.getenv(String.join("_", name, IDS_URL)))
        .dataPlaneUrl(System.getenv(String.join("_", name, DATA_PLANE_URL)))
        .build();
  }
}
