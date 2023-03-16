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

package org.eclipse.tractusx.edc.hashicorpvault;

import java.util.UUID;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HashicorpVaultIT extends AbstractHashicorpIT {

  @Test
  @DisplayName("Resolve a secret that exists")
  void testResolveSecret_exists() {
    Vault vault = getVault();
    String secretValue = vault.resolveSecret(VAULT_ENTRY_KEY);
    Assertions.assertEquals(VAULT_ENTRY_VALUE, secretValue);
  }

  @Test
  @DisplayName("Resolve a secret from a sub directory")
  void testResolveSecret_inASubDirectory() {
    Vault vault = getVault();
    String key = "sub/" + VAULT_ENTRY_KEY;
    String value = key + "value";

    vault.storeSecret(key, value);
    String secretValue = vault.resolveSecret(key);
    Assertions.assertEquals(value, secretValue);
  }

  @ParameterizedTest
  @ValueSource(strings = {"foo!bar", "foo.bar", "foo[bar]", "sub/foo{bar}"})
  @DisplayName("Resolve a secret with url encoded characters")
  void testResolveSecret_withUrlEncodedCharacters(String key) {
    Vault vault = getVault();
    String value = key + "value";

    vault.storeSecret(key, value);
    String secretValue = vault.resolveSecret(key);
    Assertions.assertEquals(value, secretValue);
  }

  @Test
  @DisplayName("Resolve a secret that does not exist")
  void testResolveSecret_doesNotExist() {
    Vault vault = getVault();
    Assertions.assertNull(vault.resolveSecret("wrong_key"));
  }

  @Test
  @DisplayName("Update a secret that exists")
  void testSetSecret_exists() {
    String key = UUID.randomUUID().toString();
    String value1 = UUID.randomUUID().toString();
    String value2 = UUID.randomUUID().toString();

    Vault vault = getVault();
    vault.storeSecret(key, value1);
    vault.storeSecret(key, value2);
    String secretValue = vault.resolveSecret(key);
    Assertions.assertEquals(value2, secretValue);
  }

  @Test
  @DisplayName("Create a secret that does not exist")
  void testSetSecret_doesNotExist() {
    String key = UUID.randomUUID().toString();
    String value = UUID.randomUUID().toString();

    Vault vault = getVault();
    vault.storeSecret(key, value);
    String secretValue = vault.resolveSecret(key);
    Assertions.assertEquals(value, secretValue);
  }

  @Test
  @DisplayName("Delete a secret that exists")
  void testDeleteSecret_exists() {
    String key = UUID.randomUUID().toString();
    String value = UUID.randomUUID().toString();

    Vault vault = getVault();
    vault.storeSecret(key, value);
    vault.deleteSecret(key);

    Assertions.assertNull(vault.resolveSecret(key));
  }

  @Test
  @DisplayName("Try to delete a secret that does not exist")
  void testDeleteSecret_doesNotExist() {
    String key = UUID.randomUUID().toString();

    Vault vault = getVault();
    vault.deleteSecret(key);

    Assertions.assertNull(vault.resolveSecret(key));
  }
}
