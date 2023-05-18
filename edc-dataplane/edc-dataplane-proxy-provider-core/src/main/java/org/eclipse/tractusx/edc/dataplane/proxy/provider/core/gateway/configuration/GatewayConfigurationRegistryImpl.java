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

import org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.configuration.GatewayConfiguration;
import org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.configuration.GatewayConfigurationRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation.
 */
public class GatewayConfigurationRegistryImpl implements GatewayConfigurationRegistry {
    private final Map<String, GatewayConfiguration> configurations = new HashMap<>();

    @Override
    public @Nullable GatewayConfiguration getConfiguration(String alias) {
        return configurations.get(alias);
    }

    @Override
    public void register(GatewayConfiguration configuration) {
        configurations.put(configuration.getAlias(), configuration);
    }
}
