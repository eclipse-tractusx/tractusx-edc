/*
 * Copyright (c) 2023 ZF Friedrichshafen AG
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

import static java.lang.String.format;

import lombok.RequiredArgsConstructor;
import org.eclipse.edc.connector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
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
    final AesAlgorithm algorithm = new AesAlgorithm(cryptoDataFactory);

    monitor.debug(
        format(
            "AES algorithm was initialised with SecureRandom algorithm '%s'",
            algorithm.getAlgorithm()));
    return new AesDataEncrypterImpl(algorithm, monitor, keyProvider, algorithm, cryptoDataFactory);
  }
}
