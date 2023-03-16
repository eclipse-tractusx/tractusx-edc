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

import java.util.UUID;
import lombok.SneakyThrows;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class HashicorpVaultTest {
  private static final String key = "key";

  // mocks
  private HashicorpVaultClient vaultClient;
  private HashicorpVault vault;

  @BeforeEach
  void setup() {
    vaultClient = Mockito.mock(HashicorpVaultClient.class);
    final Monitor monitor = Mockito.mock(Monitor.class);
    vault = new HashicorpVault(vaultClient, monitor);
  }

  @Test
  @SneakyThrows
  void getSecretSuccess() {
    // prepare
    String value = UUID.randomUUID().toString();
    Result<String> result = Mockito.mock(Result.class);
    Mockito.when(vaultClient.getSecretValue(key)).thenReturn(result);
    Mockito.when(result.getContent()).thenReturn(value);
    Mockito.when(result.succeeded()).thenReturn(true);
    Mockito.when(result.failed()).thenReturn(false);

    // invoke
    String returnValue = vault.resolveSecret(key);

    // verify
    Mockito.verify(vaultClient, Mockito.times(1)).getSecretValue(key);
    Assertions.assertEquals(value, returnValue);
  }

  @Test
  @SneakyThrows
  void getSecretFailure() {
    // prepare
    Result<String> result = Mockito.mock(Result.class);
    Mockito.when(vaultClient.getSecretValue(key)).thenReturn(result);
    Mockito.when(result.succeeded()).thenReturn(false);
    Mockito.when(result.failed()).thenReturn(true);

    // invoke
    String returnValue = vault.resolveSecret(key);

    // verify
    Mockito.verify(vaultClient, Mockito.times(1)).getSecretValue(key);
    Assertions.assertNull(returnValue);
  }

  @Test
  @SneakyThrows
  void setSecretSuccess() {
    // prepare
    String value = UUID.randomUUID().toString();
    Result<HashicorpVaultCreateEntryResponsePayload> result = Mockito.mock(Result.class);
    Mockito.when(vaultClient.setSecret(key, value)).thenReturn(result);
    Mockito.when(result.succeeded()).thenReturn(true);
    Mockito.when(result.failed()).thenReturn(false);

    // invoke
    Result<Void> returnValue = vault.storeSecret(key, value);

    // verify
    Mockito.verify(vaultClient, Mockito.times(1)).setSecret(key, value);
    Assertions.assertTrue(returnValue.succeeded());
  }

  @Test
  @SneakyThrows
  void setSecretFailure() {
    // prepare
    String value = UUID.randomUUID().toString();
    Result<HashicorpVaultCreateEntryResponsePayload> result = Mockito.mock(Result.class);
    Mockito.when(vaultClient.setSecret(key, value)).thenReturn(result);
    Mockito.when(result.succeeded()).thenReturn(false);
    Mockito.when(result.failed()).thenReturn(true);

    // invoke
    Result<Void> returnValue = vault.storeSecret(key, value);

    // verify
    Mockito.verify(vaultClient, Mockito.times(1)).setSecret(key, value);
    Assertions.assertTrue(returnValue.failed());
  }

  @Test
  @SneakyThrows
  void destroySecretSuccess() {
    // prepare
    Result<Void> result = Mockito.mock(Result.class);
    Mockito.when(vaultClient.destroySecret(key)).thenReturn(result);
    Mockito.when(result.succeeded()).thenReturn(true);
    Mockito.when(result.failed()).thenReturn(false);

    // invoke
    Result<Void> returnValue = vault.deleteSecret(key);

    // verify
    Mockito.verify(vaultClient, Mockito.times(1)).destroySecret(key);
    Assertions.assertTrue(returnValue.succeeded());
  }

  @Test
  @SneakyThrows
  void destroySecretFailure() {
    // prepare
    Result<Void> result = Mockito.mock(Result.class);
    Mockito.when(vaultClient.destroySecret(key)).thenReturn(result);
    Mockito.when(result.succeeded()).thenReturn(false);
    Mockito.when(result.failed()).thenReturn(true);

    // invoke
    Result<Void> returnValue = vault.deleteSecret(key);

    // verify
    Mockito.verify(vaultClient, Mockito.times(1)).destroySecret(key);
    Assertions.assertTrue(returnValue.failed());
  }
}
