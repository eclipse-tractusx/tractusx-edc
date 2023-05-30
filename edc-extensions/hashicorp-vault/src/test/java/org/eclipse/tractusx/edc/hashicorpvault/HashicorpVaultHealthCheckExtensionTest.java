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

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class HashicorpVaultHealthCheckExtensionTest {

    private static final String VAULT_URL = "https://example.com";
    private static final String VAULT_TOKEN = "token";
    private final HealthCheckService healthCheckService = mock(HealthCheckService.class);
    private HashicorpVaultHealthExtension extension;
    private ServiceExtensionContext context;

    @BeforeEach
    void setUp(ObjectFactory factory, ServiceExtensionContext context) {
        context.registerService(HealthCheckService.class, healthCheckService);
        this.context = spy(context);
        extension = factory.constructInstance(HashicorpVaultHealthExtension.class);
        when(this.context.getSetting(HashicorpVaultVaultExtension.VAULT_URL, null))
                .thenReturn(VAULT_URL);
        when(this.context.getSetting(HashicorpVaultVaultExtension.VAULT_TOKEN, null))
                .thenReturn(VAULT_TOKEN);
    }
    
    @Test
    void registersHealthCheckIfEnabled() {
        when(context.getSetting(HashicorpVaultHealthExtension.VAULT_HEALTH_CHECK, true))
                .thenReturn(true);

        extension.initialize(context);

        verify(healthCheckService, times(1)).addReadinessProvider(any());
        verify(healthCheckService, times(1)).addLivenessProvider(any());
        verify(healthCheckService, times(1)).addStartupStatusProvider(any());
    }

    @Test
    void registersNoHealthCheckIfDisabled() {
        when(context.getSetting(HashicorpVaultHealthExtension.VAULT_HEALTH_CHECK, true))
                .thenReturn(false);

        extension.initialize(context);

        verify(healthCheckService, times(0)).addReadinessProvider(any());
        verify(healthCheckService, times(0)).addLivenessProvider(any());
        verify(healthCheckService, times(0)).addStartupStatusProvider(any());
    }

    @Test
    void throwsHashicorpVaultExceptionOnVaultUrlUndefined() {
        when(context.getSetting(HashicorpVaultVaultExtension.VAULT_URL, null)).thenReturn(null);

        assertThrows(HashicorpVaultException.class, () -> extension.initialize(context));
    }

    @Test
    void throwsHashicorpVaultExceptionOnVaultTokenUndefined() {
        when(context.getSetting(HashicorpVaultVaultExtension.VAULT_TOKEN, null))
                .thenReturn(null);

        assertThrows(HashicorpVaultException.class, () -> extension.initialize(context));
    }
}
