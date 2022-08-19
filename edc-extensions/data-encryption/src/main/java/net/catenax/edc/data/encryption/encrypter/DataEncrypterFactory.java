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

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import net.catenax.edc.data.encryption.DataEncryptionExtension;
import net.catenax.edc.data.encryption.algorithms.CryptoAlgorithm;
import net.catenax.edc.data.encryption.algorithms.aes.AesAlgorithm;
import net.catenax.edc.data.encryption.data.CryptoDataFactory;
import net.catenax.edc.data.encryption.data.CryptoDataFactoryImpl;
import net.catenax.edc.data.encryption.key.AesKey;
import net.catenax.edc.data.encryption.key.CryptoKeyFactory;
import net.catenax.edc.data.encryption.provider.AesKeyProvider;
import net.catenax.edc.data.encryption.provider.CachingKeyProvider;
import net.catenax.edc.data.encryption.provider.KeyProvider;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;

@RequiredArgsConstructor
public class DataEncrypterFactory {

  public static final String AES_ALGORITHM = "AES";
  public static final String NONE = "NONE";

  private final Vault vault;
  private final Monitor monitor;
  private final CryptoKeyFactory keyFactory;

  public DataEncrypter create(DataEncrypterConfiguration configuration) {
    if (configuration.getAlgorithm().equalsIgnoreCase(AES_ALGORITHM)) {
      return createAesEncrypter(configuration);
    } else if (configuration.getAlgorithm().equalsIgnoreCase(NONE)) {
      return createNoneEncrypter();
    } else {
      final String msg =
          String.format(
              DataEncryptionExtension.NAME
                  + ": Unsupported encryption algorithm '%s'. Supported algorithms are '%s',  %s.",
              configuration.getAlgorithm(),
              AES_ALGORITHM,
              NONE);
      throw new NoSuchElementException(msg);
    }
  }

  public DataEncrypter createAesEncrypter(DataEncrypterConfiguration configuration) {

    KeyProvider<AesKey> keyProvider =
        new AesKeyProvider(vault, configuration.getKeySetAlias(), keyFactory);

    if (configuration.isCachingEnabled()) {
      keyProvider = new CachingKeyProvider<>(keyProvider, configuration.getCachingDuration());
    }

    final CryptoDataFactory cryptoDataFactory = new CryptoDataFactoryImpl();
    final CryptoAlgorithm<AesKey> algorithm = new AesAlgorithm(cryptoDataFactory);

    return new AesDataEncrypterImpl(algorithm, monitor, keyProvider, algorithm, cryptoDataFactory);
  }

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
}
