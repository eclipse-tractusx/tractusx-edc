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
package org.eclipse.tractusx.edc.data.encryption.key;

import lombok.Value;
import org.bouncycastle.util.encoders.Base64;

public class CryptoKeyFactoryImpl implements CryptoKeyFactory {

  public AesKey fromBase64(String base64) {
    return fromBytes(Base64.decode(base64));
  }

  public AesKey fromBytes(byte[] key) {
    int bitLength = key.length * Byte.SIZE;
    if (!(bitLength == 128 || bitLength == 192 || bitLength == 256)) {
      throw new IllegalArgumentException("Invalid AES key length: " + bitLength);
    }

    return new AesKeyImpl(key, Base64.toBase64String(key));
  }

  @Value
  private static class AesKeyImpl implements AesKey {
    byte[] bytes;
    String base64;
  }
}
