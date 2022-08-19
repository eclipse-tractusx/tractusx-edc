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
package net.catenax.edc.data.encryption.key;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CryptoKeyFactoryImplTest {

  @ParameterizedTest
  @ValueSource(ints = {32, 64, 512, 1024, 2048, 4096})
  void throwsIllegalArgumentExceptionWhenInvalidAesKeyLength(int bitLength) {
    CryptoKeyFactory cryptoKeyFactory = new CryptoKeyFactoryImpl();
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> cryptoKeyFactory.fromBytes(new byte[bitLength / Byte.SIZE]));
  }

  @ParameterizedTest
  @ValueSource(ints = {128, 192, 256})
  void throwsNotOnValidAesKeyLength(int bitLength) {
    CryptoKeyFactory cryptoKeyFactory = new CryptoKeyFactoryImpl();
    Assertions.assertDoesNotThrow(
        () -> cryptoKeyFactory.fromBytes(new byte[bitLength / Byte.SIZE]));
  }
}
