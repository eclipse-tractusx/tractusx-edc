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

package org.eclipse.tractusx.edc.dataplane.proxy.provider.core;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.dataplane.proxy.provider.core.gateway.auth.AuthorizationHandlerRegistryImpl;
import org.eclipse.tractusx.edc.dataplane.proxy.provider.core.gateway.configuration.GatewayConfigurationRegistryImpl;
import org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.authorization.AuthorizationExtension;
import org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.authorization.AuthorizationHandlerRegistry;
import org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.configuration.GatewayConfigurationRegistry;
import org.jetbrains.annotations.NotNull;

import static java.lang.String.format;
import static org.eclipse.edc.spi.result.Result.success;
import static org.eclipse.tractusx.edc.dataplane.proxy.provider.core.gateway.configuration.GatewayConfigurationLoader.loadConfiguration;
import static org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.configuration.GatewayConfiguration.NO_AUTHORIZATION;

/**
 * Registers default services for the data plane provider proxy implementation.
 */
@Extension(value = ProxyProviderCoreExtension.NAME)
@Provides({ GatewayConfigurationRegistry.class, AuthorizationHandlerRegistry.class })
public class ProxyProviderCoreExtension implements ServiceExtension {
    static final String NAME = "Data Plane Provider Proxy Core";

    @Inject(required = false)
    private AuthorizationExtension authorizationExtension;

    @Inject
    private Vault vault;

    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var configurationRegistry = new GatewayConfigurationRegistryImpl();
        context.registerService(GatewayConfigurationRegistry.class, configurationRegistry);

        if (authorizationExtension == null) {
            context.getMonitor().info("Proxy JWT authorization is configured to only validate tokens and not provide path access control");
            authorizationExtension = (c, p) -> success();
        }

        var authorizationRegistry = createAuthorizationRegistry();
        context.registerService(AuthorizationHandlerRegistry.class, authorizationRegistry);

        loadConfiguration(context).forEach(configuration -> {
            monitor.info(format("Registering gateway configuration alias `%s` to %s", configuration.getAlias(), configuration.getProxiedPath()));
            configurationRegistry.register(configuration);
        });
    }

    @NotNull
    private AuthorizationHandlerRegistryImpl createAuthorizationRegistry() {
        var authorizationRegistry = new AuthorizationHandlerRegistryImpl();

        authorizationRegistry.register(NO_AUTHORIZATION, (t, p) -> success());

        return authorizationRegistry;
    }

}
