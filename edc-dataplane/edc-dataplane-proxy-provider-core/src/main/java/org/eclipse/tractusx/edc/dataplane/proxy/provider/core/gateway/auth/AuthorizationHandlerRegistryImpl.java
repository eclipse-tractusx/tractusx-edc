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

package org.eclipse.tractusx.edc.dataplane.proxy.provider.core.gateway.auth;

import org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.authorization.AuthorizationHandler;
import org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.authorization.AuthorizationHandlerRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of the registry.
 */
public class AuthorizationHandlerRegistryImpl implements AuthorizationHandlerRegistry {
    private final Map<String, AuthorizationHandler> handlers = new HashMap<>();

    @Override
    public @Nullable AuthorizationHandler getHandler(String alias) {
        return handlers.get(alias);
    }

    @Override
    public void register(String alias, AuthorizationHandler handler) {
        handlers.put(alias, handler);
    }
}
