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

package org.eclipse.tractusx.edc.vault.memory;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class VaultMemoryExtensionTest {
    private VaultMemoryExtension extension;
    private ServiceExtensionContext context;
    private Monitor monitor;

    @BeforeEach
    void setup() {
        extension = new VaultMemoryExtension();
        context = mock(ServiceExtensionContext.class);
        monitor = mock(Monitor.class);
        when(context.getMonitor()).thenReturn(monitor);
    }

    @Test
    void name() {
        assertThat(extension.name()).isEqualTo("In-Memory Vault Extension");
    }

    @ParameterizedTest
    @ValueSource(strings = {"key1:", "key1:value1", "key1:value1;", ";key1:value1", ";sdf;key1:value1"})
    void createInMemVault_validString(String secret) {
        when(context.getSetting(eq(VaultMemoryExtension.VAULT_MEMORY_SECRETS_PROPERTY), eq(null))).thenReturn(secret);
        extension.createInMemVault(context);
        verify(monitor, times(1)).debug(anyString());
    }
}
