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
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class HashicorpVaultExtensionTest {

    private static final String VAULT_URL = "https://example.com";
    private static final String VAULT_TOKEN = "token";

    private HashicorpVaultVaultExtension extension;

    // mocks
    private ServiceExtensionContext context;
    private Monitor monitor;
    private HealthCheckService healthCheckService;

    @BeforeEach
    void setUp(ObjectFactory factory, ServiceExtensionContext context) {
        this.context = spy(context);
        context.registerService(HealthCheckService.class, healthCheckService);
        monitor = mock(Monitor.class);
        healthCheckService = mock(HealthCheckService.class);
        extension = factory.constructInstance(HashicorpVaultVaultExtension.class);
    }

    @Test
    void throwsHashicorpVaultExceptionOnVaultUrlUndefined() {
        when(context.getSetting(HashicorpVaultVaultExtension.VAULT_URL, null)).thenReturn(null);

        Assertions.assertThrows(HashicorpVaultException.class, () -> extension.initialize(context));
    }

    @Test
    void throwsHashicorpVaultExceptionOnVaultTokenUndefined() {
        when(context.getSetting(HashicorpVaultVaultExtension.VAULT_TOKEN, null))
                .thenReturn(null);

        Assertions.assertThrows(HashicorpVaultException.class, () -> extension.initialize(context));
    }
}
