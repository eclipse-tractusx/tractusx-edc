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

package org.eclipse.tractusx.edc.data.encryption.encrypter;

import lombok.RequiredArgsConstructor;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.tractusx.edc.data.encryption.algorithms.CryptoAlgorithm;
import org.eclipse.tractusx.edc.data.encryption.algorithms.aes.AesAlgorithm;
import org.eclipse.tractusx.edc.data.encryption.data.CryptoDataFactory;
import org.eclipse.tractusx.edc.data.encryption.data.CryptoDataFactoryImpl;
import org.eclipse.tractusx.edc.data.encryption.key.AesKey;
import org.eclipse.tractusx.edc.data.encryption.key.CryptoKeyFactory;
import org.eclipse.tractusx.edc.data.encryption.provider.AesKeyProvider;
import org.eclipse.tractusx.edc.data.encryption.provider.CachingKeyProvider;
import org.eclipse.tractusx.edc.data.encryption.provider.KeyProvider;

@RequiredArgsConstructor
public class DataEncrypterFactory {

  public static final String AES_ALGORITHM = "AES";
  public static final String NONE = "NONE";

  private final Vault vault;
  private final Monitor monitor;
  private final CryptoKeyFactory keyFactory;

  public DataEncrypter createNoneEncrypter() {
    return new DataEncrypter() {
      @Override
      public String encrypt(String data) {
        return data;
      }

      @Override
      public String decrypt(String data) {
        return data;
      }
    };
  }

  public DataEncrypter createAesEncrypter(AesDataEncrypterConfiguration configuration) {
    KeyProvider<AesKey> keyProvider =
        new AesKeyProvider(vault, configuration.getKeySetAlias(), keyFactory);

    if (configuration.isCachingEnabled()) {
      keyProvider = new CachingKeyProvider<>(keyProvider, configuration.getCachingDuration());
    }

    final CryptoDataFactory cryptoDataFactory = new CryptoDataFactoryImpl();
    final CryptoAlgorithm<AesKey> algorithm = new AesAlgorithm(cryptoDataFactory);

    return new AesDataEncrypterImpl(algorithm, monitor, keyProvider, algorithm, cryptoDataFactory);
  }
}
