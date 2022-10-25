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

package org.eclipse.tractusx.edc.tests;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ConnectorFactory {
  private static final Map<String, Connector> CONNECTOR_CACHE = new HashMap<>();

  public static Connector byName(@NonNull final String name) {
    return CONNECTOR_CACHE.computeIfAbsent(
        name.toUpperCase(Locale.ROOT), k -> createConnector(name));
  }

  private static Connector createConnector(@NonNull final String name) {
    final Environment environment = Environment.byName(name);

    return new Connector(name, environment);
  }
}
