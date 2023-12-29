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

import org.eclipse.edc.connector.core.vault.InMemoryVault;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class VaultSeedExtensionTest {
    private VaultSeedExtension extension;
    private ServiceExtensionContext context;
    private Monitor monitor;

    @BeforeEach
    void setup(ServiceExtensionContext context, ObjectFactory factory) {
        this.context = context;
        monitor = mock(Monitor.class);
        context.registerService(Monitor.class, monitor);
        context.registerService(Vault.class, new InMemoryVault(monitor));
        extension = factory.constructInstance(VaultSeedExtension.class);
    }

    @Test
    void name() {
        assertThat(extension.name()).isEqualTo("Vault Seed Extension");
    }

    @ParameterizedTest
    @ValueSource(strings = { "key1:", "key1:value1", "key1:value1;", ";key1:value1", ";sdf;key1:value1" })
    void createInMemVault_validString(String secret) {
        when(context.getSetting(eq(VaultSeedExtension.VAULT_MEMORY_SECRETS_PROPERTY), eq(null))).thenReturn(secret);
        extension.createInMemVault(context);
        verify(monitor, times(1)).debug(anyString());
    }
}
