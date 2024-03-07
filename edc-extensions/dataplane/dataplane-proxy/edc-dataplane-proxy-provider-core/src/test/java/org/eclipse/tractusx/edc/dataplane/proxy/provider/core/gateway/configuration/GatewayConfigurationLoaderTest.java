/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.dataplane.proxy.provider.core.gateway.configuration;

import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.dataplane.proxy.provider.core.gateway.configuration.GatewayConfigurationLoader.AUTHORIZATION_TYPE;
import static org.eclipse.tractusx.edc.dataplane.proxy.provider.core.gateway.configuration.GatewayConfigurationLoader.PROXIED_PATH;
import static org.eclipse.tractusx.edc.dataplane.proxy.provider.core.gateway.configuration.GatewayConfigurationLoader.TX_GATEWAY_PREFIX;
import static org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.configuration.GatewayConfiguration.NO_AUTHORIZATION;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayConfigurationLoaderTest {

    @Test
    void verify_loadConfiguration() {
        var context = mock(ServiceExtensionContext.class);

        var config = ConfigFactory.fromMap(
                Map.of(format("alias.%s", AUTHORIZATION_TYPE), NO_AUTHORIZATION,
                        format("alias.%s", PROXIED_PATH), "https://test.com"));
        when(context.getConfig(TX_GATEWAY_PREFIX)).thenReturn(config);

        var configurations = GatewayConfigurationLoader.loadConfiguration(context);

        assertThat(configurations).isNotEmpty();
        var configuration = configurations.get(0);

        assertThat(configuration.getAlias()).isEqualTo("alias");
        assertThat(configuration.getAuthorizationType()).isEqualTo(NO_AUTHORIZATION);
        assertThat(configuration.getProxiedPath()).isEqualTo("https://test.com");
    }
}
