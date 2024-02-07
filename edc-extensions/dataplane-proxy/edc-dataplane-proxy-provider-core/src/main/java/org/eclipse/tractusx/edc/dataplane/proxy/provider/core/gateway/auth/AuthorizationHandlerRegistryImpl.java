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
