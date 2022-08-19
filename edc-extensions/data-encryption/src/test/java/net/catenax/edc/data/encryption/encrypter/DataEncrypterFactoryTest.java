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
package net.catenax.edc.data.encryption.encrypter;

import java.time.Duration;
import java.util.NoSuchElementException;
import net.catenax.edc.data.encryption.key.CryptoKeyFactoryImpl;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;
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

  @Test
  void testExceptionOnInvalidStrategy() {
    final DataEncrypterConfiguration configuration = newConfiguration("something invalid");
    Assertions.assertThrows(NoSuchElementException.class, () -> factory.create(configuration));
  }

  @ParameterizedTest
  @ValueSource(strings = {DataEncrypterFactory.AES_ALGORITHM, DataEncrypterFactory.NONE})
  void testValidStrategies(String strategy) {
    final DataEncrypterConfiguration configuration = newConfiguration(strategy);
    Assertions.assertDoesNotThrow(() -> factory.create(configuration));
  }

  @Test
  void testEncrypterWithCaching() {
    Mockito.when(vault.resolveSecret(KEY_SET_ALIAS)).thenReturn("7h6sh6t6tchCmNnHjK2kFA==");

    final DataEncrypterConfiguration configuration = newConfiguration(true);
    final DataEncrypter dataEncrypter = factory.create(configuration);

    final String foo1 = dataEncrypter.encrypt("foo1");
    dataEncrypter.decrypt(foo1);
    final String foo2 = dataEncrypter.encrypt("foo2");
    dataEncrypter.decrypt(foo2);
    final String foo3 = dataEncrypter.encrypt("foo3");
    dataEncrypter.decrypt(foo3);

    // one invoke to get encryption- and one to cache decryption key
    Mockito.verify(vault, Mockito.times(2)).resolveSecret(KEY_SET_ALIAS);
  }

  private DataEncrypterConfiguration newConfiguration(String encryptionStrategy) {
    return new DataEncrypterConfiguration(encryptionStrategy, KEY_SET_ALIAS, false, null);
  }

  private DataEncrypterConfiguration newConfiguration(boolean isCachingEnabled) {
    return new DataEncrypterConfiguration(
        DataEncrypterFactory.AES_ALGORITHM, KEY_SET_ALIAS, isCachingEnabled, Duration.ofMinutes(1));
  }
}
