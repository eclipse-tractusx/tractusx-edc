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

package net.catenax.edc.tests.features;

import io.cucumber.java.AfterAll;
import io.cucumber.java.ParameterType;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import net.catenax.edc.tests.Connector;
import net.catenax.edc.tests.Environment;

public class ParameterTypes {
  private static final Map<String, Connector> CONNECTOR_CACHE = new ConcurrentHashMap<>();

  @ParameterType(".*")
  public Connector connector(@NonNull final String name) {
    return getOrCreateConnector(name);
  }

  private static Connector getOrCreateConnector(@NonNull final String name) {
    return CONNECTOR_CACHE.computeIfAbsent(
        name.toUpperCase(Locale.ROOT), k -> new Connector(name, Environment.byName(name)));
  }

  @AfterAll
  public static void shutDown() {
    CONNECTOR_CACHE.forEach(
        (key, value) -> {
          CONNECTOR_CACHE.remove(key);
          value.close();
        });
  }
}
