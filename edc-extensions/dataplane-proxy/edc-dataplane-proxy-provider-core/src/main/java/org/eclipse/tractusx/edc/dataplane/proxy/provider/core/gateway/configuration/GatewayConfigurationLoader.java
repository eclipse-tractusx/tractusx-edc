/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.dataplane.proxy.provider.core.gateway.configuration;

import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.configuration.GatewayConfiguration;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.configuration.GatewayConfiguration.TOKEN_AUTHORIZATION;

/**
 * Loads gateway configuration from the {@link #TX_GATEWAY_PREFIX} prefix.
 */
public class GatewayConfigurationLoader {
    static final String TX_GATEWAY_PREFIX = "tx.dpf.proxy.gateway";
    static final String AUTHORIZATION_TYPE = "authorization.type";
    static final String PROXIED_PATH = "proxied.path";

    public static List<GatewayConfiguration> loadConfiguration(ServiceExtensionContext context) {
        var root = context.getConfig(TX_GATEWAY_PREFIX);
        return root.partition().map(GatewayConfigurationLoader::createGatewayConfiguration).collect(toList());
    }

    private static GatewayConfiguration createGatewayConfiguration(Config config) {
        return GatewayConfiguration.Builder.newInstance()
                .alias(config.currentNode())
                .authorizationType(config.getString(AUTHORIZATION_TYPE, TOKEN_AUTHORIZATION))
                .proxiedPath(config.getString(PROXIED_PATH))
                .build();
    }
}
