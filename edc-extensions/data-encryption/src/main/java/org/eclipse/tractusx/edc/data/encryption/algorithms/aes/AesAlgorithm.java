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
package org.eclipse.tractusx.edc.data.encryption.algorithms.aes;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.NonNull;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.tractusx.edc.data.encryption.algorithms.CryptoAlgorithm;
import org.eclipse.tractusx.edc.data.encryption.data.CryptoDataFactory;
import org.eclipse.tractusx.edc.data.encryption.data.DecryptedData;
import org.eclipse.tractusx.edc.data.encryption.data.EncryptedData;
import org.eclipse.tractusx.edc.data.encryption.key.AesKey;
import org.eclipse.tractusx.edc.data.encryption.util.ArrayUtil;

public class AesAlgorithm implements CryptoAlgorithm<AesKey> {

  private static final String AES_GCM = "AES/GCM/NoPadding";
  private static final String AES = "AES";
  private static final Object MONITOR = new Object();

  @NonNull private final CryptoDataFactory cryptoDataFactory;
  private AesInitializationVectorIterator initializationVectorIterator;

  public AesAlgorithm(CryptoDataFactory cryptoDataFactory) {
    this.cryptoDataFactory = cryptoDataFactory;
    this.initializationVectorIterator = new AesInitializationVectorIterator();
  }

  @Override
  public synchronized EncryptedData encrypt(DecryptedData data, AesKey key)
      throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException,
          NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {

    final byte[] initializationVector;
    synchronized (MONITOR) {
      if (!initializationVectorIterator.hasNext()) {
        initializationVectorIterator = new AesInitializationVectorIterator();
      }

      initializationVector = initializationVectorIterator.next();
    }

    Cipher cipher = Cipher.getInstance(AES_GCM, new BouncyCastleProvider());
    final SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), AES);
    final GCMParameterSpec gcmParameterSpec =
        new GCMParameterSpec(16 * 8 /* =128 */, initializationVector);
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
    byte[] encrypted = cipher.doFinal(data.getBytes());
    byte[] encryptedWithVector = ArrayUtil.concat(initializationVector, encrypted);

    return cryptoDataFactory.encryptedFromBytes(encryptedWithVector);
  }

  @Override
  public DecryptedData decrypt(EncryptedData data, AesKey key)
      throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException,
          NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
    byte[] encryptedWithVector = data.getBytes();
    byte[] initializationVector = ArrayUtil.subArray(encryptedWithVector, 0, 16);
    byte[] encrypted = ArrayUtil.subArray(encryptedWithVector, 16, encryptedWithVector.length - 16);

    Cipher cipher = Cipher.getInstance(AES_GCM, new BouncyCastleProvider());
    final SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), AES);
    final GCMParameterSpec gcmParameterSpec =
        new GCMParameterSpec(16 * 8 /* =128 */, initializationVector);
    cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
    byte[] decryptedData = cipher.doFinal(encrypted);
    return cryptoDataFactory.decryptedFromBytes(decryptedData);
  }
}
