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

import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.tractusx.edc.data.encryption.key.AesKey;
import org.eclipse.tractusx.edc.data.encryption.key.CryptoKeyFactoryImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AesKeyProviderTest {

  private static final String KEY_1 = "dVUjmYJzbwVcntkFZU+lNQ==";
  private static final String KEY_2 = "7h6sh6t6tchCmNnHjK2kFA==";
  private static final String KEY_3 = "uyNfJzhsnvfEe9OtQyR9Og==";

  private static final String KEY_ALIAS = "foo";

  private AesKeyProvider keyProvider;

  // mocks
  private Vault vault;

  @BeforeEach
  void setup() {
    vault = Mockito.mock(Vault.class);
    keyProvider = new AesKeyProvider(vault, KEY_ALIAS, new CryptoKeyFactoryImpl());
  }

  @Test
  void testEncryptionKeyAlwaysFirstKey() {
    Mockito.when(vault.resolveSecret(KEY_ALIAS))
        .thenReturn(String.format("%s,%s,%s", KEY_1, KEY_2, KEY_3));

    AesKey key = keyProvider.getEncryptionKey();

    Assertions.assertEquals(KEY_1, key.getBase64());
  }

  @Test
  void testEncryptionThrowsOnNoKey() {
    Mockito.when(vault.resolveSecret(KEY_ALIAS)).thenReturn(" ");

    Assertions.assertThrows(RuntimeException.class, () -> keyProvider.getEncryptionKey());
  }

  @Test
  void testGetKeys() {
    Mockito.when(vault.resolveSecret(KEY_ALIAS))
        .thenReturn(String.format("%s,  ,,%s,%s", KEY_1, KEY_2, KEY_3));

    List<String> keys =
        keyProvider.getDecryptionKeySet().map(AesKey::getBase64).collect(Collectors.toList());
    List<String> expected = List.of(KEY_1, KEY_2, KEY_3);

    Assertions.assertEquals(expected, keys);
  }
}
