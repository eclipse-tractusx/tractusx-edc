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

package org.eclipse.tractusx.edc.postgresql.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Objects;
import java.util.Properties;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.sql.ConnectionFactory;

class DriverManagerConnectionFactory implements ConnectionFactory {
  private final String jdbcUrl;
  private final Properties properties;

  public DriverManagerConnectionFactory(final String jdbcUrl, final Properties properties) {
    this.jdbcUrl = Objects.requireNonNull(jdbcUrl);
    this.properties = Objects.requireNonNull(properties);
  }

  @Override
  public Connection create() {
    try {
      return DriverManager.getConnection(jdbcUrl, properties);
    } catch (Exception exception) {
      throw new EdcPersistenceException(exception.getMessage(), exception);
    }
  }
}
