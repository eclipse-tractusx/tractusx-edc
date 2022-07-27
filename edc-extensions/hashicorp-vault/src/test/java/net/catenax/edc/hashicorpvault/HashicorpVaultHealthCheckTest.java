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
 *       Mercedes-Benz Tech Innovation GmbH - Initial Test
 *
 */

package net.catenax.edc.hashicorpvault;

import java.io.IOException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

class HashicorpVaultHealthCheckTest {

  private HashicorpVaultHealthCheck healthCheck;

  // mocks
  private Monitor monitor;
  private HashicorpVaultClient client;

  @BeforeEach
  void setup() {
    monitor = Mockito.mock(Monitor.class);
    client = Mockito.mock(HashicorpVaultClient.class);

    healthCheck = new HashicorpVaultHealthCheck(client, monitor);
  }

  @ParameterizedTest
  @ValueSource(ints = {200, 409, 472, 473, 501, 503, 999})
  void testResponseFromCode(int code) throws IOException {

    Mockito.when(client.getHealth())
        .thenReturn(
            new HashicorpVaultHealthResponse(new HashicorpVaultHealthResponsePayload(), code));

    final HealthCheckResult result = healthCheck.get();

    if (code == 200) Assertions.assertTrue(result.succeeded());
    else Assertions.assertTrue(result.failed());

    Mockito.verify(monitor, Mockito.times(1)).debug(Mockito.anyString());
  }

  @Test
  void testResponseFromException() throws IOException {
    Mockito.when(client.getHealth()).thenThrow(new IOException());

    final HealthCheckResult result = healthCheck.get();
    Assertions.assertFalse(result.succeeded());
  }
}
