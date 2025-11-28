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

import org.eclipse.edc.iam.decentralizedclaims.sts.remote.RemoteSecureTokenService;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.tractusx.edc.iam.dcp.sts.dim.DimSecureTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.iam.dcp.sts.RemoteTokenServiceClientExtension.DIM_URL;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class RemoteTokenServiceClientExtensionTest {

    @Test
    void initialize(ServiceExtensionContext context, RemoteTokenServiceClientExtension extension) {
        var config = mock(Config.class);
        when(context.getConfig()).thenReturn(config);
        when(config.getString(DIM_URL, null)).thenReturn("url");
        assertThat(extension.secureTokenService(context)).isInstanceOf(DimSecureTokenService.class);
    }

    @Test
    void initialize_whenUrlIsMissing_fallsBackToRemoteSts(ServiceExtensionContext context, RemoteTokenServiceClientExtension extension) {
        var monitor = context.getMonitor();
        var prefixeMonitor = mock(Monitor.class);
        when(monitor.withPrefix(anyString())).thenReturn(prefixeMonitor);

        assertThat(extension.secureTokenService(context))
                .isInstanceOf(RemoteSecureTokenService.class);
    }

}
