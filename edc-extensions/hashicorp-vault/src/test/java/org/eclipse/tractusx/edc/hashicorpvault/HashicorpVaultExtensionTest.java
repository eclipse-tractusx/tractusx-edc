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

package org.eclipse.tractusx.edc.hashicorpvault;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class HashicorpVaultExtensionTest {

  private static final String VAULT_URL = "https://example.com";
  private static final String VAULT_TOKEN = "token";

  private HashicorpVaultVaultExtension extension;

  // mocks
  private ServiceExtensionContext context;
  private Monitor monitor;
  private HealthCheckService healthCheckService;

  @BeforeEach
  void setup() {
    context = Mockito.mock(ServiceExtensionContext.class);
    monitor = Mockito.mock(Monitor.class);
    healthCheckService = Mockito.mock(HealthCheckService.class);
    extension = new HashicorpVaultVaultExtension();

    Mockito.when(context.getService(HealthCheckService.class)).thenReturn(healthCheckService);
    Mockito.when(context.getMonitor()).thenReturn(monitor);
    Mockito.when(context.getTypeManager()).thenReturn(new TypeManager());
    Mockito.when(context.getSetting(HashicorpVaultVaultExtension.VAULT_URL, null))
        .thenReturn(VAULT_URL);
    Mockito.when(context.getSetting(HashicorpVaultVaultExtension.VAULT_TOKEN, null))
        .thenReturn(VAULT_TOKEN);

    Mockito.when(
            context.getSetting(
                HashicorpVaultVaultExtension.VAULT_API_SECRET_PATH,
                HashicorpVaultVaultExtension.VAULT_API_SECRET_PATH_DEFAULT))
        .thenReturn(HashicorpVaultVaultExtension.VAULT_API_SECRET_PATH_DEFAULT);
    Mockito.when(
            context.getSetting(
                HashicorpVaultVaultExtension.VAULT_API_HEALTH_PATH,
                HashicorpVaultVaultExtension.VAULT_API_HEALTH_PATH_DEFAULT))
        .thenReturn(HashicorpVaultVaultExtension.VAULT_API_HEALTH_PATH_DEFAULT);
    Mockito.when(
            context.getSetting(
                HashicorpVaultVaultExtension.VAULT_HEALTH_CHECK_STANDBY_OK,
                HashicorpVaultVaultExtension.VAULT_HEALTH_CHECK_STANDBY_OK_DEFAULT))
        .thenReturn(HashicorpVaultVaultExtension.VAULT_HEALTH_CHECK_STANDBY_OK_DEFAULT);
  }

  @Test
  void throwsHashicorpVaultExceptionOnVaultUrlUndefined() {
    Mockito.when(context.getSetting(HashicorpVaultVaultExtension.VAULT_URL, null)).thenReturn(null);

    Assertions.assertThrows(HashicorpVaultException.class, () -> extension.initialize(context));
  }

  @Test
  void throwsHashicorpVaultExceptionOnVaultTokenUndefined() {
    Mockito.when(context.getSetting(HashicorpVaultVaultExtension.VAULT_TOKEN, null))
        .thenReturn(null);

    Assertions.assertThrows(HashicorpVaultException.class, () -> extension.initialize(context));
  }
}
