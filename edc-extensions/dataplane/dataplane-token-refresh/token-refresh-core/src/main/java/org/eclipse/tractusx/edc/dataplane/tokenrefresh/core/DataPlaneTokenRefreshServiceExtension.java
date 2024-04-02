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

package org.eclipse.tractusx.edc.dataplane.tokenrefresh.core;

import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAccessTokenService;
import org.eclipse.edc.connector.dataplane.spi.store.AccessTokenDataStore;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.keys.spi.LocalPublicKeyService;
import org.eclipse.edc.keys.spi.PrivateKeyResolver;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.tractusx.edc.spi.tokenrefresh.dataplane.DataPlaneTokenRefreshService;
import org.jetbrains.annotations.NotNull;

import java.security.PrivateKey;
import java.time.Clock;
import java.util.function.Supplier;

import static org.eclipse.tractusx.edc.dataplane.tokenrefresh.core.DataPlaneTokenRefreshServiceExtension.NAME;

@Extension(value = NAME)
public class DataPlaneTokenRefreshServiceExtension implements ServiceExtension {
    public static final String NAME = "DataPlane Token Refresh Service extension";
    public static final int DEFAULT_TOKEN_EXPIRY_TOLERANCE_SECONDS = 5;
    public static final long DEFAULT_TOKEN_EXPIRY_SECONDS = 300L;
    @Setting(value = "Token expiry tolerance period in seconds to allow for clock skew", defaultValue = "" + DEFAULT_TOKEN_EXPIRY_TOLERANCE_SECONDS)
    public static final String TOKEN_EXPIRY_TOLERANCE_SECONDS_PROPERTY = "edc.dataplane.token.expiry.tolerance";
    @Setting(value = "The HTTP endpoint where clients can request a renewal of their access token for the public dataplane API")
    public static final String REFRESH_ENDPOINT_PROPERTY = "edc.dataplane.token.refresh.endpoint";
    @Setting(value = "Alias of private key used for signing tokens, retrieved from private key resolver")
    public static final String TOKEN_SIGNER_PRIVATE_KEY_ALIAS = "edc.transfer.proxy.token.signer.privatekey.alias";
    @Setting(value = "Alias of public key used for verifying the tokens, retrieved from the vault")
    public static final String TOKEN_VERIFIER_PUBLIC_KEY_ALIAS = "edc.transfer.proxy.token.verifier.publickey.alias";
    @Setting(value = "Expiry time of access token in seconds", defaultValue = DEFAULT_TOKEN_EXPIRY_SECONDS + "")
    public static final String TOKEN_EXPIRY_SECONDS_PROPERTY = "edc.dataplane.token.expiry";
    @Inject
    private TokenValidationService tokenValidationService;
    @Inject
    private DidPublicKeyResolver didPkResolver;
    @Inject
    private LocalPublicKeyService localPublicKeyService;
    @Inject
    private AccessTokenDataStore accessTokenDataStore;
    @Inject
    private PrivateKeyResolver privateKeyResolver;
    @Inject
    private Clock clock;
    @Inject
    private Vault vault;
    @Inject
    private TypeManager typeManager;
    @Inject
    private Hostname hostname;

    private DataPlaneTokenRefreshServiceImpl tokenRefreshService;

    @Override
    public String name() {
        return NAME;
    }

    // exposes the service as access token service
    @Provider
    public DataPlaneAccessTokenService createAccessTokenService(ServiceExtensionContext context) {
        return getTokenRefreshService(context);
    }

    // exposes the service as pure refresh service
    @Provider
    public DataPlaneTokenRefreshService createRefreshTokenService(ServiceExtensionContext context) {
        return getTokenRefreshService(context);
    }

    private int getExpiryToleranceConfig(ServiceExtensionContext context) {
        return context.getConfig().getInteger(TOKEN_EXPIRY_TOLERANCE_SECONDS_PROPERTY, DEFAULT_TOKEN_EXPIRY_TOLERANCE_SECONDS);
    }

    @NotNull
    private DataPlaneTokenRefreshServiceImpl getTokenRefreshService(ServiceExtensionContext context) {
        if (tokenRefreshService == null) {
            var monitor = context.getMonitor().withPrefix("DataPlane Token Refresh");
            var expiryTolerance = getExpiryToleranceConfig(context);
            var refreshEndpoint = getRefreshEndpointConfig(context, monitor);
            var tokenExpiry = getExpiryConfig(context);
            monitor.debug("Token refresh endpoint: %s".formatted(refreshEndpoint));
            monitor.debug("Token refresh time tolerance: %d s".formatted(expiryTolerance));
            tokenRefreshService = new DataPlaneTokenRefreshServiceImpl(clock, tokenValidationService, didPkResolver, localPublicKeyService, accessTokenDataStore, new JwtGenerationService(),
                    getPrivateKeySupplier(context), context.getMonitor(), refreshEndpoint, expiryTolerance, tokenExpiry,
                    () -> context.getConfig().getString(TOKEN_VERIFIER_PUBLIC_KEY_ALIAS), vault, typeManager.getMapper());
        }
        return tokenRefreshService;
    }

    private Long getExpiryConfig(ServiceExtensionContext context) {
        return context.getConfig().getLong(TOKEN_EXPIRY_SECONDS_PROPERTY, DEFAULT_TOKEN_EXPIRY_SECONDS);
    }

    private String getRefreshEndpointConfig(ServiceExtensionContext context, Monitor monitor) {
        var refreshEndpoint = context.getConfig().getString(REFRESH_ENDPOINT_PROPERTY, null);
        if (refreshEndpoint == null) {
            var port = context.getConfig().getInteger("web.http.public.port", 8185);
            var path = context.getConfig().getString("web.http.public.path", "/api/v2/public");
            refreshEndpoint = "http://%s:%d%s".formatted(hostname.get(), port, path);
            monitor.warning("Config property '%s' was not specified, the default '%s' will be used.".formatted(REFRESH_ENDPOINT_PROPERTY, refreshEndpoint));
        }
        return refreshEndpoint;
    }


    @NotNull
    private Supplier<PrivateKey> getPrivateKeySupplier(ServiceExtensionContext context) {
        return () -> {
            var alias = context.getConfig().getString(TOKEN_SIGNER_PRIVATE_KEY_ALIAS);
            return privateKeyResolver.resolvePrivateKey(alias)
                    .orElse(f -> {
                        context.getMonitor().warning("Cannot resolve private key: " + f.getFailureDetail());
                        return null;
                    });
        };
    }
}
