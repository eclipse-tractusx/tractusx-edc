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
package org.eclipse.tractusx.edc.data.encryption.provider;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.util.encoders.Base64;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.tractusx.edc.data.encryption.DataEncryptionExtension;
import org.eclipse.tractusx.edc.data.encryption.key.AesKey;
import org.eclipse.tractusx.edc.data.encryption.key.CryptoKeyFactory;

@RequiredArgsConstructor
public class AesKeyProvider implements KeyProvider<AesKey> {

  private static final String KEY_SEPARATOR = ",";

  private final Vault vault;
  private final String vaultKeyAlias;
  private final CryptoKeyFactory cryptoKeyFactory;

  @Override
  public Stream<AesKey> getDecryptionKeySet() {
    return getKeysStream();
  }

  @Override
  public AesKey getEncryptionKey() {
    return getKeysStream()
        .findFirst()
        .orElseThrow(
            () ->
                new RuntimeException(
                    DataEncryptionExtension.EXTENSION_NAME
                        + ": Vault must contain at least one key."));
  }

  private Stream<AesKey> getKeysStream() {
    return Arrays.stream(getKeys().split(KEY_SEPARATOR))
        .map(String::trim)
        .filter(Predicate.not(String::isEmpty))
        .map(Base64::decode)
        .map(cryptoKeyFactory::fromBytes);
  }

  private String getKeys() {
    String keys = vault.resolveSecret(vaultKeyAlias);
    return keys == null ? "" : keys;
  }
}
