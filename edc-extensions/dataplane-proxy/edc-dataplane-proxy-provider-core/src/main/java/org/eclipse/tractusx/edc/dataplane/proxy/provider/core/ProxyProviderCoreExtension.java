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

package org.eclipse.tractusx.edc.dataplane.proxy.provider.core;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.dataplane.proxy.provider.core.gateway.auth.AuthorizationHandlerRegistryImpl;
import org.eclipse.tractusx.edc.dataplane.proxy.provider.core.gateway.auth.JwtAuthorizationHandler;
import org.eclipse.tractusx.edc.dataplane.proxy.provider.core.gateway.auth.RsaPublicKeyParser;
import org.eclipse.tractusx.edc.dataplane.proxy.provider.core.gateway.configuration.GatewayConfigurationRegistryImpl;
import org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.authorization.AuthorizationExtension;
import org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.authorization.AuthorizationHandler;
import org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.authorization.AuthorizationHandlerRegistry;
import org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.configuration.GatewayConfigurationRegistry;
import org.jetbrains.annotations.NotNull;

import static java.lang.String.format;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;
import static org.eclipse.tractusx.edc.dataplane.proxy.provider.core.gateway.configuration.GatewayConfigurationLoader.loadConfiguration;
import static org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.configuration.GatewayConfiguration.NO_AUTHORIZATION;
import static org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.configuration.GatewayConfiguration.TOKEN_AUTHORIZATION;

/**
 * Registers default services for the data plane provider proxy implementation.
 */
@Extension(value = ProxyProviderCoreExtension.NAME)
@Provides({GatewayConfigurationRegistry.class, AuthorizationHandlerRegistry.class})
public class ProxyProviderCoreExtension implements ServiceExtension {
    static final String NAME = "Data Plane Provider Proxy Core";

    @Setting
    private static final String PUBLIC_KEY = "tx.dpf.data.proxy.public.key";

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

        var authorizationRegistry = creatAuthorizationRegistry();
        context.registerService(AuthorizationHandlerRegistry.class, authorizationRegistry);

        loadConfiguration(context).forEach(configuration -> {
            monitor.info(format("Registering gateway configuration alias `%s` to %s", configuration.getAlias(), configuration.getProxiedPath()));
            configurationRegistry.register(configuration);
        });
    }

    @NotNull
    private AuthorizationHandlerRegistryImpl creatAuthorizationRegistry() {
        var authorizationRegistry = new AuthorizationHandlerRegistryImpl();

        authorizationRegistry.register(NO_AUTHORIZATION, (t, p) -> success());

        authorizationRegistry.register(TOKEN_AUTHORIZATION, createJwtAuthorizationHandler());

        return authorizationRegistry;
    }

    @NotNull
    private AuthorizationHandler createJwtAuthorizationHandler() {
        var publicCertKey = vault.resolveSecret(PUBLIC_KEY);

        if (publicCertKey == null) {
            monitor.warning("Data proxy public key not set in the vault. Disabling JWT authorization for the proxy data.");
            return (t, p) -> failure("Authentication disabled");
        }

        var publicKey = new RsaPublicKeyParser().parsePublicKey(publicCertKey);
        var verifier = new RSASSAVerifier(publicKey);

        return new JwtAuthorizationHandler(verifier, authorizationExtension, monitor);
    }


}
