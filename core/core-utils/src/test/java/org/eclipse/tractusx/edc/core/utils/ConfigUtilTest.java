/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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
 */

package org.eclipse.tractusx.edc.core.utils;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ConfigUtilTest {

    private static final String CONFIG = "tx.edc.test.config";
    private static final String CONFIG_DEPRECATED = "tx.edc.test.config.deprecated";
    private final ServiceExtensionContext context = mock(ServiceExtensionContext.class);

    @Test
    void missingProperty_shouldThrowException() {
        assertThatThrownBy(() -> ConfigUtil.missingMandatoryProperty(mock(), "foo"))
                .isInstanceOf(EdcException.class)
                .hasMessage("Mandatory config value missing: 'foo'. This runtime is not operational.");
    }

    @Test
    void propertyCompatibility_whenConfigExists_shouldReturnValue() {
        when(context.getSetting(eq(CONFIG), isNull())).thenReturn("some-val");
        assertThat(ConfigUtil.propertyCompatibility(context, CONFIG, CONFIG_DEPRECATED, "default-value"))
                .isEqualTo("some-val");

        verify(context).getSetting(eq(CONFIG), isNull());
        verifyNoMoreInteractions(context);
    }

    @Test
    void propertyCompatibility_whenDeprecatedConfigExists_shouldReturnOldValue() {
        when(context.getSetting(eq(CONFIG_DEPRECATED), isNull())).thenReturn("some-val");
        var monitorMock = mock(Monitor.class);
        when(context.getMonitor()).thenReturn(monitorMock);

        assertThat(ConfigUtil.propertyCompatibility(context, CONFIG, CONFIG_DEPRECATED, "default-value")).isEqualTo("some-val");

        verify(context).getSetting(eq(CONFIG), isNull());
        verify(context).getSetting(eq(CONFIG_DEPRECATED), isNull());
        verify(monitorMock).warning(anyString());
    }

    @Test
    void propertyCompatibility_whenBothConfigsExists_shouldReturnNewValue() {
        when(context.getSetting(eq(CONFIG), isNull())).thenReturn("some-val");
        when(context.getSetting(eq(CONFIG_DEPRECATED), isNull())).thenReturn("some-old-val");

        assertThat(ConfigUtil.propertyCompatibility(context, CONFIG, CONFIG_DEPRECATED, "default-value")).isEqualTo("some-val");

        verify(context).getSetting(eq(CONFIG), isNull());
        verifyNoMoreInteractions(context);
    }

    @Test
    void propertyCompatibility_whenNoConfigsExists_shouldReturnDefaultValue() {
        assertThat(ConfigUtil.propertyCompatibility(context, CONFIG, CONFIG_DEPRECATED, "default-value")).isEqualTo("default-value");

        verify(context).getSetting(eq(CONFIG), isNull());
        verify(context).getSetting(eq(CONFIG_DEPRECATED), isNull());
        verifyNoMoreInteractions(context);
    }
}