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
package org.eclipse.tractusx.edc.data.encryption.key;

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


  private static class AesKeyImpl implements AesKey {
    private final byte[] bytes;
    private final String base64;

    private AesKeyImpl(byte[] bytes, String base64) {
      this.bytes = bytes;
      this.base64 = base64;
    }

    @Override
    public byte[] getBytes() {
      return bytes;
    }

    @Override
    public String getBase64() {
      return base64;
    }
  }
}
