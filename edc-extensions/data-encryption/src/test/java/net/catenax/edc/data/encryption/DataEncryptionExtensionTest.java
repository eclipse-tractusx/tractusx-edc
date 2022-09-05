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

package net.catenax.edc.data.encryption;

import net.catenax.edc.data.encryption.encrypter.DataEncrypterFactory;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DataEncryptionExtensionTest {

  private DataEncryptionExtension extension;

  // mocks
  private Monitor monitor;
  private ServiceExtensionContext context;
  private Vault vault;

  @BeforeEach
  void setup() {
    monitor = Mockito.mock(Monitor.class);
    context = Mockito.mock(ServiceExtensionContext.class);
    vault = Mockito.mock(Vault.class);

    extension = new DataEncryptionExtension();

    Mockito.when(context.getMonitor()).thenReturn(monitor);
    Mockito.when(context.getService(Vault.class)).thenReturn(vault);

    Mockito.when(
            context.getSetting(
                Mockito.eq(DataEncryptionExtension.CACHING_ENABLED), Mockito.anyBoolean()))
        .thenAnswer((i) -> i.getArguments()[1]);
    Mockito.when(
            context.getSetting(
                Mockito.eq(DataEncryptionExtension.ENCRYPTION_ALGORITHM), Mockito.anyString()))
        .thenAnswer((i) -> i.getArguments()[1]);
    Mockito.when(
            context.getSetting(
                Mockito.eq(DataEncryptionExtension.CACHING_SECONDS), Mockito.anyInt()))
        .thenAnswer((i) -> i.getArguments()[1]);
  }

  @Test
  void testName() {
    Assertions.assertEquals(DataEncryptionExtension.NAME, extension.name());
  }

  @Test
  void testExceptionOnMissingKeySetAlias() {
    Mockito.when(context.getSetting(DataEncryptionExtension.ENCRYPTION_KEY_SET, null))
        .thenReturn(null);
    Assertions.assertThrows(EdcException.class, () -> extension.initialize(context));
  }

  @Test
  void testStartExceptionOnMissingKeySetInVault() {
    final String keySetAlias = "foo";
    Mockito.when(context.getSetting(DataEncryptionExtension.ENCRYPTION_KEY_SET, null))
        .thenReturn(keySetAlias);
    Mockito.when(vault.resolveSecret(keySetAlias)).thenReturn("");

    extension.initialize(context);

    Assertions.assertThrows(EdcException.class, () -> extension.start());
  }

  @Test
  void testStartExceptionOnStartWithWrongKeySetAlias() {
    final String keySetAlias = "foo";
    Mockito.when(
            context.getSetting(
                DataEncryptionExtension.ENCRYPTION_ALGORITHM, DataEncrypterFactory.AES_ALGORITHM))
        .thenReturn(DataEncrypterFactory.AES_ALGORITHM);
    Mockito.when(context.getSetting(DataEncryptionExtension.ENCRYPTION_KEY_SET, null))
        .thenReturn(keySetAlias);
    Mockito.when(vault.resolveSecret(keySetAlias)).thenReturn("l8b2YHL7VpA=, invalid-key");

    extension.initialize(context);

    Assertions.assertThrows(EdcException.class, () -> extension.start());
  }

  @Test
  void testNonEncrypterRequiresNoOtherSetting() {
    final String keySetAlias = "foo";
    Mockito.when(
            context.getSetting(
                DataEncryptionExtension.ENCRYPTION_ALGORITHM, DataEncrypterFactory.AES_ALGORITHM))
        .thenReturn(DataEncrypterFactory.NONE);
    Mockito.when(context.getSetting(DataEncryptionExtension.ENCRYPTION_KEY_SET, null))
        .thenReturn(null);
    Mockito.when(vault.resolveSecret(keySetAlias)).thenReturn(null);

    Assertions.assertDoesNotThrow(() -> extension.initialize(context));
    Assertions.assertDoesNotThrow(() -> extension.start());
  }
}
