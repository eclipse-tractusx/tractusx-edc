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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import lombok.RequiredArgsConstructor;
import net.catenax.edc.data.encryption.DataEncryptionExtension;
import net.catenax.edc.data.encryption.algorithms.CryptoAlgorithm;
import net.catenax.edc.data.encryption.data.CryptoDataFactory;
import net.catenax.edc.data.encryption.data.DecryptedData;
import net.catenax.edc.data.encryption.data.EncryptedData;
import net.catenax.edc.data.encryption.key.AesKey;
import net.catenax.edc.data.encryption.provider.KeyProvider;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;

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
                    DataEncryptionExtension.NAME
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
              DataEncryptionExtension.NAME
                  + ": Exception decrypting data using key from rotating key set. %s",
              e.getMessage()));
      throw new EdcException(e);
    }
  }
}
