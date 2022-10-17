/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.tractusx.edc.boot.runtime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

class MultiTenantRuntimeTest {

  private final Monitor monitor = mock(Monitor.class);
  private final MultiTenantRuntime runtime =
      new MultiTenantRuntime() {
        @Override
        protected @NotNull Monitor createMonitor() {
          return monitor;
        }
      };

  @Test
  void throwsExceptionIfNoTenantsPropertyProvided() {
    assertThrows(EdcException.class, runtime::boot);
    verify(monitor, never()).info(argThat(connectorIsReady()));
  }

  @Test
  void throwsExceptionIfTenantsFileDoesNotExist() {
    System.setProperty("edc.tenants.path", "unexistentfile");

    assertThrows(EdcException.class, runtime::boot);
    verify(monitor, never()).info(argThat(connectorIsReady()));
  }

  @Test
  void threadForEveryTenant() {
    System.setProperty("edc.tenants.path", "./src/test/resources/tenants.properties");

    runtime.boot();

    verify(monitor, times(2)).info(argThat(connectorIsReady()));
  }

  @NotNull
  private ArgumentMatcher<String> connectorIsReady() {
    return message -> message.endsWith(" ready");
  }
}
