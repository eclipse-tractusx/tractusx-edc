/********************************************************************************
 * Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.samples.multitenancy;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MultiTenantRuntimeTest {

    private final Monitor monitor = mock();
    private final MultiTenantRuntime runtime =
            new MultiTenantRuntime() {
                @Override
                protected @NotNull Monitor createMonitor() {
                    return monitor;
                }
            };

    @Test
    void throwsExceptionIfNoTenantsPropertyProvided() {
        assertThrows(EdcException.class, () -> runtime.boot(false));
        verify(monitor, never()).info(argThat(connectorIsReady()));
    }

    @Test
    void throwsExceptionIfTenantsFileDoesNotExist() {
        System.setProperty("edc.tenants.path", "unexistentfile");

        assertThrows(EdcException.class, () -> runtime.boot(false));
        verify(monitor, never()).info(argThat(connectorIsReady()));
    }

    @Test
    void threadForEveryTenant() {
        System.setProperty("edc.tenants.path", "./src/test/resources/tenants.properties");
        when(monitor.withPrefix(anyString())).thenReturn(monitor);
        runtime.boot(false);

        verify(monitor, times(2)).info(argThat(connectorIsReady()));
    }

    @NotNull
    private ArgumentMatcher<String> connectorIsReady() {
        return message -> message.endsWith(" ready");
    }

}
