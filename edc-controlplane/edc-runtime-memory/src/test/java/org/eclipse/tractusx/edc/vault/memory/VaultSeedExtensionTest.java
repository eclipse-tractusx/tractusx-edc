/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2025 Cofinity-X GmbH
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.edc.vault.memory;

import org.eclipse.edc.boot.vault.InMemoryVault;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
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
    private final Monitor monitor = mock();
    private final SingleParticipantContextSupplier participantContextSupplier = () -> ServiceResult.success(
            ParticipantContext.Builder.newInstance().participantContextId("participantContextId").identity("identity").build()
    );

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(Monitor.class, monitor);
        context.registerService(Vault.class, new InMemoryVault(monitor));
        context.registerService(SingleParticipantContextSupplier.class, participantContextSupplier);
    }

    @Test
    void name(VaultSeedExtension extension) {
        assertThat(extension.name()).isEqualTo("Vault Seed Extension");
    }

    @ParameterizedTest
    @ValueSource(strings = { "key1:", "key1:value1", "key1:value1;", ";key1:value1", ";sdf;key1:value1" })
    void createInMemVault_validString(String secret, ServiceExtensionContext context, VaultSeedExtension extension) {
        when(context.getSetting(eq(VaultSeedExtension.VAULT_MEMORY_SECRETS_PROPERTY), eq(null))).thenReturn(secret);
        extension.createInMemVault(context);
        verify(monitor, times(1)).debug(anyString());
    }
}
