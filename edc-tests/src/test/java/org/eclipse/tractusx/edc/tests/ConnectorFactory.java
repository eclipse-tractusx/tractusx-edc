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
