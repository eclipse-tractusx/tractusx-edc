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

package org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.authorization;

import org.jetbrains.annotations.Nullable;

/**
 * Manages {@link AuthorizationHandler}s.
 */
public interface AuthorizationHandlerRegistry {

    /**
     * Returns a handler for the alias or null if not found.
     */
    @Nullable
    AuthorizationHandler getHandler(String alias);

    /**
     * Registers a handler for the given alias.
     */
    void register(String alias, AuthorizationHandler handler);

}
