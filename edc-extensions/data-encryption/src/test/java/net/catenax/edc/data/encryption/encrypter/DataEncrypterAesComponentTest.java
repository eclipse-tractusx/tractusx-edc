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

import lombok.SneakyThrows;
import net.catenax.edc.data.encryption.algorithms.CryptoAlgorithm;
import net.catenax.edc.data.encryption.algorithms.aes.AesAlgorithm;
import net.catenax.edc.data.encryption.data.CryptoDataFactory;
import net.catenax.edc.data.encryption.data.CryptoDataFactoryImpl;
import net.catenax.edc.data.encryption.data.DecryptedData;
import net.catenax.edc.data.encryption.data.EncryptedData;
import net.catenax.edc.data.encryption.key.AesKey;
import net.catenax.edc.data.encryption.key.CryptoKeyFactory;
import net.catenax.edc.data.encryption.key.CryptoKeyFactoryImpl;
import net.catenax.edc.data.encryption.provider.AesKeyProvider;
import net.catenax.edc.data.encryption.provider.KeyProvider;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@SuppressWarnings("FieldCanBeLocal")
class DataEncrypterAesComponentTest {

  private static final String KEY_128_BIT_BASE_64 = "7h6sh6t6tchCmNnHjK2kFA==";
  private static final String KEY_256_BIT_BASE_64 = "OSD+3NcZAmS/6UXbq6NL8UL+aQIAJDLL7BE2rBX5MtA=";

  private DataEncrypter dataEncrypter;
  private CryptoAlgorithm<AesKey> algorithm;
  private KeyProvider<AesKey> keyProvider;
  private CryptoKeyFactory cryptoKeyFactory;
  private CryptoDataFactory cryptoDataFactory;

  // mocks
  private Monitor monitor;
  private Vault vault;

  @BeforeEach
  void setup() {
    monitor = Mockito.mock(Monitor.class);
    vault = Mockito.mock(Vault.class);

    cryptoKeyFactory = new CryptoKeyFactoryImpl();
    cryptoDataFactory = new CryptoDataFactoryImpl();
    algorithm = new AesAlgorithm(cryptoDataFactory);
    keyProvider = new AesKeyProvider(vault, "foo", cryptoKeyFactory);

    dataEncrypter =
        new AesDataEncrypterImpl(algorithm, monitor, keyProvider, algorithm, cryptoDataFactory);
  }

  @Test
  @SneakyThrows
  void testKeyRotation() {
    Mockito.when(vault.resolveSecret(Mockito.anyString()))
        .thenReturn(
            String.format(
                "%s, %s, %s, %s",
                KEY_128_BIT_BASE_64,
                KEY_128_BIT_BASE_64,
                KEY_128_BIT_BASE_64,
                KEY_256_BIT_BASE_64));

    final AesKey key256Bit = cryptoKeyFactory.fromBase64(KEY_256_BIT_BASE_64);
    final String expectedResult = "hello";
    final DecryptedData decryptedResult = cryptoDataFactory.decryptedFromText(expectedResult);
    final EncryptedData encryptedResult = algorithm.encrypt(decryptedResult, key256Bit);

    var result = dataEncrypter.decrypt(encryptedResult.getBase64());

    Assertions.assertEquals(expectedResult, result);
  }

  @Test
  void testEncryption() {
    Mockito.when(vault.resolveSecret(Mockito.anyString())).thenReturn(KEY_128_BIT_BASE_64);

    final String expectedResult = "hello world!";

    var encryptedResult = dataEncrypter.encrypt(expectedResult);
    var result = dataEncrypter.decrypt(encryptedResult);

    Assertions.assertEquals(expectedResult, result);
  }
}
