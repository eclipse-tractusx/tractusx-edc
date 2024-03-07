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
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.configuration.GatewayConfiguration;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.configuration.GatewayConfiguration.NO_AUTHORIZATION;

/**
 * Loads gateway configuration from the {@link #TX_GATEWAY_PREFIX} prefix.
 */
public class GatewayConfigurationLoader {
    public static final String DEFAULT_FORWARD_EDR_HEADER_KEY = "Edc-Edr";
    static final String TX_GATEWAY_PREFIX = "tx.dpf.proxy.gateway";
    static final String AUTHORIZATION_TYPE = "authorization.type";
    static final String PROXIED_PATH = "proxied.path";
    static final String FORWARD_EDR = "proxied.edr.forward";
    static final String FORWARD_EDR_HEADER_KEY = "proxied.edr.headerKey";

    public static List<GatewayConfiguration> loadConfiguration(ServiceExtensionContext context) {
        var root = context.getConfig(TX_GATEWAY_PREFIX);
        return root.partition().map(GatewayConfigurationLoader::createGatewayConfiguration).collect(toList());
    }

    private static GatewayConfiguration createGatewayConfiguration(Config config) {
        return GatewayConfiguration.Builder.newInstance()
                .alias(config.currentNode())
                .authorizationType(config.getString(AUTHORIZATION_TYPE, NO_AUTHORIZATION))
                .forwardEdrToken(config.getBoolean(FORWARD_EDR, false))
                .forwardEdrTokenHeaderKey(config.getString(FORWARD_EDR_HEADER_KEY, DEFAULT_FORWARD_EDR_HEADER_KEY))

                .proxiedPath(config.getString(PROXIED_PATH))
                .build();
    }
}
