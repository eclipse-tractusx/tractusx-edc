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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import lombok.RequiredArgsConstructor;
import org.eclipse.edc.connector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.data.encryption.DataEncryptionExtension;
import org.eclipse.tractusx.edc.data.encryption.algorithms.CryptoAlgorithm;
import org.eclipse.tractusx.edc.data.encryption.data.CryptoDataFactory;
import org.eclipse.tractusx.edc.data.encryption.data.DecryptedData;
import org.eclipse.tractusx.edc.data.encryption.data.EncryptedData;
import org.eclipse.tractusx.edc.data.encryption.key.AesKey;
import org.eclipse.tractusx.edc.data.encryption.provider.KeyProvider;

@RequiredArgsConstructor
public class AesDataEncrypterImpl implements DataEncrypter {

  private final CryptoAlgorithm<AesKey> encryptionStrategy;
  private final Monitor monitor;
  private final KeyProvider<AesKey> keyProvider;
  private final CryptoAlgorithm<AesKey> algorithm;
  private final CryptoDataFactory cryptoDataFactory;

  @Override
  public String encrypt(String value) {
    DecryptedData decryptedData = cryptoDataFactory.decryptedFromText(value);
    AesKey key = keyProvider.getEncryptionKey();

    try {
      EncryptedData encryptedData = algorithm.encrypt(decryptedData, key);
      return encryptedData.getBase64();
    } catch (IllegalBlockSizeException
        | BadPaddingException
        | InvalidKeyException
        | InvalidAlgorithmParameterException
        | NoSuchPaddingException
        | NoSuchAlgorithmException e) {
      throw new EdcException(e);
    }
  }

  @Override
  public String decrypt(String value) {
    EncryptedData encryptedData = cryptoDataFactory.encryptedFromBase64(value);

    return keyProvider
        .getDecryptionKeySet()
        .map(key -> decrypt(encryptedData, key))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(DecryptedData::getBytes)
        .map(String::new)
        .findFirst()
        .orElseThrow(
            () ->
                new EdcException(
                    DataEncryptionExtension.EXTENSION_NAME
                        + ": Failed to decrypt data. This can happen if the key set is empty, contains invalid keys, the decryption key rotated out of the key set or because the data was encrypted by a different algorithm."));
  }

  private Optional<DecryptedData> decrypt(EncryptedData data, AesKey key) {
    try {
      return Optional.of(encryptionStrategy.decrypt(data, key));
    } catch (AEADBadTagException e) { // thrown when wrong key is used for decryption
      return Optional.empty();
    } catch (IllegalBlockSizeException
        | BadPaddingException
        | InvalidKeyException
        | NoSuchPaddingException
        | NoSuchAlgorithmException
        | InvalidAlgorithmParameterException e) {
      monitor.warning(
          String.format(
              DataEncryptionExtension.EXTENSION_NAME
                  + ": Exception decrypting data using key from rotating key set. %s",
              e.getMessage()));
      throw new EdcException(e);
    }
  }
}
