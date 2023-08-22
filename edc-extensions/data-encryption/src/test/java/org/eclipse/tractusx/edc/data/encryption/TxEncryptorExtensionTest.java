/*
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.data.encryption;

import org.eclipse.edc.connector.transfer.dataplane.security.NoopDataEncrypter;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.tractusx.edc.data.encryption.aes.AesEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.data.encryption.TxEncryptorExtension.CACHING_ENABLED;
import static org.eclipse.tractusx.edc.data.encryption.TxEncryptorExtension.CACHING_SECONDS;
import static org.eclipse.tractusx.edc.data.encryption.TxEncryptorExtension.ENCRYPTION_ALGORITHM;
import static org.eclipse.tractusx.edc.data.encryption.TxEncryptorExtension.ENCRYPTION_KEY_ALIAS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class TxEncryptorExtensionTest {

    private final Monitor monitor = mock();
    private TxEncryptorExtension extension;
    private ServiceExtensionContext context;

    @BeforeEach
    void setup(ObjectFactory factory, ServiceExtensionContext c) {
        c.registerService(Monitor.class, monitor);
        context = spy(c);
        when(context.getSetting(ENCRYPTION_KEY_ALIAS, null)).thenReturn("test-key");
        extension = factory.constructInstance(TxEncryptorExtension.class);
    }

    @Test
    void createEncryptor_noConfig_createsDefault() {
        var encryptor = extension.createEncryptor(context);
        assertThat(encryptor).isInstanceOf(AesEncryptor.class);
    }

    @Test
    void createEncryptor_otherAlgorithm_createsNoop() {
        when(context.getSetting(eq(ENCRYPTION_ALGORITHM), any())).thenReturn("some-algorithm");
        var encryptor = extension.createEncryptor(context);
        assertThat(encryptor).isInstanceOf(NoopDataEncrypter.class);
        verify(monitor).warning(eq("Algorithm some-algorithm is not known, will use a NOOP encryptor!"));
    }

    @Test
    void createEncryptor_withPropertyEqualsAes() {
        when(context.getSetting(eq(ENCRYPTION_ALGORITHM), any())).thenReturn("AES");
        var encryptor = extension.createEncryptor(context);
        assertThat(encryptor).isInstanceOf(AesEncryptor.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {CACHING_ENABLED, CACHING_SECONDS})
    void verifyDeprecationWarnings(String deprecatedSetting) {
        when(context.getSetting(eq(deprecatedSetting), any())).thenReturn("doesn't matter");
        extension.createEncryptor(context);
        verify(monitor).warning(startsWith("Caching the secret keys was deprecated because it is unsafe"));
    }
}