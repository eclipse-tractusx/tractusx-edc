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
package org.eclipse.tractusx.edc.data.encryption.encrypter;

import java.time.Duration;
import org.eclipse.edc.connector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.tractusx.edc.data.encryption.key.CryptoKeyFactoryImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

@SuppressWarnings("FieldCanBeLocal")
class DataEncrypterFactoryTest {

  private static final String KEY_SET_ALIAS = "keySetAlias";

  DataEncrypterFactory factory;

  // mocks
  private Vault vault;
  private Monitor monitor;

  @BeforeEach
  void setup() {
    vault = Mockito.mock(Vault.class);
    monitor = Mockito.mock(Monitor.class);

    factory = new DataEncrypterFactory(vault, monitor, new CryptoKeyFactoryImpl());
  }

  @ParameterizedTest
  @ValueSource(strings = {DataEncrypterFactory.AES_ALGORITHM, DataEncrypterFactory.NONE})
  void testValidStrategies(String strategy) {
    final AesDataEncrypterConfiguration configuration = newConfiguration(false);
    Assertions.assertDoesNotThrow(() -> factory.createAesEncrypter(configuration));
  }

  @Test
  void testEncrypterWithCaching() {
    Mockito.when(vault.resolveSecret(KEY_SET_ALIAS)).thenReturn("7h6sh6t6tchCmNnHjK2kFA==");

    final AesDataEncrypterConfiguration configuration = newConfiguration(true);
    final DataEncrypter dataEncrypter = factory.createAesEncrypter(configuration);

    final String foo1 = dataEncrypter.encrypt("foo1");
    dataEncrypter.decrypt(foo1);
    final String foo2 = dataEncrypter.encrypt("foo2");
    dataEncrypter.decrypt(foo2);
    final String foo3 = dataEncrypter.encrypt("foo3");
    dataEncrypter.decrypt(foo3);

    // one invoke to get encryption- and one to cache decryption key
    Mockito.verify(vault, Mockito.times(2)).resolveSecret(KEY_SET_ALIAS);
  }

  private AesDataEncrypterConfiguration newConfiguration(boolean isCachingEnabled) {
    return new AesDataEncrypterConfiguration(
        KEY_SET_ALIAS, isCachingEnabled, Duration.ofMinutes(1));
  }
}
