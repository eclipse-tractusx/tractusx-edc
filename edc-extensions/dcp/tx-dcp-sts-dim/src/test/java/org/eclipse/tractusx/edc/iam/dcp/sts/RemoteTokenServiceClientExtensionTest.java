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

package org.eclipse.tractusx.edc.iam.dcp.sts;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.iam.decentralizedclaims.sts.remote.RemoteSecureTokenService;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.tractusx.edc.iam.dcp.sts.dim.DimSecureTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.iam.dcp.sts.RemoteTokenServiceClientExtension.DIM_URL;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class RemoteTokenServiceClientExtensionTest {

    private static final String DIM_CONFIGURED_LOG = "DIM URL configured, will use DIM STS client";
    private static final String DIM_NOT_CONFIGURED_LOG = "DIM URL not configured, will use the standard EDC Remote STS client";

    private final Monitor monitor = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(Monitor.class, monitor);
    }

    @Test
    void initialize(ServiceExtensionContext context, RemoteTokenServiceClientExtension extension) {
        var config = mock(Config.class);
        when(context.getConfig()).thenReturn(config);
        when(config.getString(DIM_URL, null)).thenReturn("url");
        assertThat(extension.secureTokenService(context)).isInstanceOf(DimSecureTokenService.class);

        verify(monitor).info(DIM_CONFIGURED_LOG);
        verify(monitor, never()).info(DIM_NOT_CONFIGURED_LOG);
    }

    @Test
    void initialize_whenUrlIsMissing_fallsBackToRemoteSts(ServiceExtensionContext context, RemoteTokenServiceClientExtension extension) {
        var monitor = context.getMonitor();
        var prefixeMonitor = mock(Monitor.class);
        when(monitor.withPrefix(anyString())).thenReturn(prefixeMonitor);

        assertThat(extension.secureTokenService(context))
                .isInstanceOf(RemoteSecureTokenService.class);
        verify(monitor).info(DIM_NOT_CONFIGURED_LOG);
        verify(monitor, never()).info(DIM_CONFIGURED_LOG);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "/" })
    void initialize_whenUrlIsBlank_fallsBackToRemoteSts(String divUrl, ServiceExtensionContext context, ObjectFactory f) {
        var config = ConfigFactory.fromMap(Map.of(DIM_URL, divUrl));
        when(context.getConfig()).thenReturn(config);

        var extension = f.constructInstance(RemoteTokenServiceClientExtension.class);

        assertThat(extension.secureTokenService(context))
                .isInstanceOf(RemoteSecureTokenService.class);
        verify(monitor).info(DIM_NOT_CONFIGURED_LOG);
        verify(monitor, never()).info(DIM_CONFIGURED_LOG);
    }

}
