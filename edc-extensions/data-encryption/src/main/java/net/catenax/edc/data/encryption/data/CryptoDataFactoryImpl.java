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
package net.catenax.edc.data.encryption.data;

import lombok.Value;
import org.bouncycastle.util.encoders.Base64;

public class CryptoDataFactoryImpl implements CryptoDataFactory {

  public DecryptedData decryptedFromText(String text) {
    final byte[] bytes = text.getBytes();
    final String base64 = Base64.toBase64String(bytes);
    return new DecryptedDataImpl(bytes, base64, text);
  }

  public DecryptedData decryptedFromBase64(String base64) {
    final byte[] bytes = Base64.decode(base64);
    final String text = new String(bytes);
    return new DecryptedDataImpl(bytes, base64, text);
  }

  public DecryptedData decryptedFromBytes(byte[] bytes) {
    final String base64 = Base64.toBase64String(bytes);
    final String text = new String(bytes);
    return new DecryptedDataImpl(bytes, base64, text);
  }

  public EncryptedData encryptedFromText(String text) {
    final byte[] bytes = text.getBytes();
    final String base64 = Base64.toBase64String(bytes);
    return new EncryptedDataImpl(bytes, base64, text);
  }

  public EncryptedData encryptedFromBase64(String base64) {
    final byte[] bytes = Base64.decode(base64);
    final String text = new String(bytes);
    return new EncryptedDataImpl(bytes, base64, text);
  }

  public EncryptedData encryptedFromBytes(byte[] bytes) {
    final String base64 = Base64.toBase64String(bytes);
    final String text = new String(bytes);
    return new EncryptedDataImpl(bytes, base64, text);
  }

  @Value
  private static class DecryptedDataImpl implements DecryptedData {
    byte[] bytes;
    String base64;
    String text;
  }

  @Value
  private static class EncryptedDataImpl implements EncryptedData {
    byte[] bytes;
    String base64;
    String text;
  }
}
