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
import org.eclipse.edc.jwt.spi.signer.JwsSignerProvider;
import org.eclipse.edc.keys.spi.LocalPublicKeyService;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
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

import java.time.Clock;

import static org.eclipse.tractusx.edc.dataplane.tokenrefresh.core.DataPlaneTokenRefreshServiceExtension.NAME;

@Extension(value = NAME)
public class DataPlaneTokenRefreshServiceExtension implements ServiceExtension {
    public static final String NAME = "DataPlane Token Refresh Service extension";

    private static final int DEFAULT_TOKEN_EXPIRY_TOLERANCE_SECONDS = 5;
    private static final long DEFAULT_TOKEN_EXPIRY_SECONDS = 300L;
    private static final int DEFAULT_WEB_PORT = 8185;
    private static final String DEFAULT_WEB_PATH = "/api/v2/public";

    private static final String TOKEN_SIGNER_PRIVATE_KEY_ALIAS = "edc.transfer.proxy.token.signer.privatekey.alias";
    private static final String TOKEN_VERIFIER_PUBLIC_KEY_ALIAS = "edc.transfer.proxy.token.verifier.publickey.alias";
    private static final String TOKEN_EXPIRY_TOLERANCE_SECONDS_PROPERTY = "tx.edc.dataplane.token.expiry.tolerance";
    private static final String TOKEN_EXPIRY_SECONDS_PROPERTY = "tx.edc.dataplane.token.expiry";
    private static final String REFRESH_ENDPOINT_PROPERTY = "tx.edc.dataplane.token.refresh.endpoint";
    private static final String WEB_PORT = "web.http.public.port";
    private static final String WEB_PATH = "web.http.public.path";

    @Setting(key = REFRESH_ENDPOINT_PROPERTY, description = "The HTTP endpoint where clients can request a renewal of their access token for the public dataplane API", required = false)
    private String refreshEndpointConfig;

    @Setting(key = TOKEN_SIGNER_PRIVATE_KEY_ALIAS, description = "Alias of private key used for signing tokens, retrieved from private key resolver")
    private String tokenSignerPrivateKeyAlias;

    @Setting(key = TOKEN_VERIFIER_PUBLIC_KEY_ALIAS, description = "Alias of public key used for verifying the tokens, retrieved from the vault")
    private String tokenVerifierPublicKeyAlias;

    @Setting(key = TOKEN_EXPIRY_TOLERANCE_SECONDS_PROPERTY, description = "Token expiry tolerance period in seconds to allow for clock skew", defaultValue = DEFAULT_TOKEN_EXPIRY_TOLERANCE_SECONDS + "")
    private int tokenExpiryToleranceSeconds;

    @Setting(key = TOKEN_EXPIRY_SECONDS_PROPERTY, description = "Expiry time of access token in seconds", defaultValue = DEFAULT_TOKEN_EXPIRY_SECONDS + "")
    private long tokenExpirySeconds;

    @Setting (key = WEB_PORT, defaultValue = DEFAULT_WEB_PORT + "")
    private int webPort;

    @Setting (key = WEB_PATH, defaultValue = DEFAULT_WEB_PATH)
    private String webPath;

    @Inject
    private Monitor monitor;
    @Inject
    private TokenValidationService tokenValidationService;
    @Inject
    private DidPublicKeyResolver didPkResolver;
    @Inject
    private LocalPublicKeyService localPublicKeyService;
    @Inject
    private AccessTokenDataStore accessTokenDataStore;
    @Inject
    private Clock clock;
    @Inject
    private Vault vault;
    @Inject
    private TypeManager typeManager;
    @Inject
    private Hostname hostname;
    @Inject
    private JwsSignerProvider jwsSignerProvider;
    @Inject
    private SingleParticipantContextSupplier participantContextSupplier;

    private DataPlaneTokenRefreshServiceImpl tokenRefreshService;

    @Override
    public String name() {
        return NAME;
    }

    // exposes the service as access token service
    @Provider
    public DataPlaneAccessTokenService  createAccessTokenService(ServiceExtensionContext context) {
        return getTokenRefreshService(context);
    }

    // exposes the service as pure refresh service
    @Provider
    public DataPlaneTokenRefreshService createRefreshTokenService(ServiceExtensionContext context) {
        return getTokenRefreshService(context);
    }

    @NotNull
    private DataPlaneTokenRefreshServiceImpl getTokenRefreshService(ServiceExtensionContext context) {
        if (tokenRefreshService == null) {
            var monitor = context.getMonitor().withPrefix("DataPlane Token Refresh");
            var refreshEndpoint = getRefreshEndpointConfig(monitor);
            monitor.debug("Token refresh endpoint: %s".formatted(refreshEndpoint));
            monitor.debug("Token refresh time tolerance: %d s".formatted(tokenExpiryToleranceSeconds));
            tokenRefreshService = new DataPlaneTokenRefreshServiceImpl(clock, tokenValidationService, didPkResolver, localPublicKeyService, accessTokenDataStore, new JwtGenerationService(jwsSignerProvider),
                    () -> tokenSignerPrivateKeyAlias, context.getMonitor(), refreshEndpoint, tokenExpiryToleranceSeconds, tokenExpirySeconds,
                    () -> tokenVerifierPublicKeyAlias, vault, typeManager.getMapper(), participantContextSupplier);
        }
        return tokenRefreshService;
    }

    private String getRefreshEndpointConfig(Monitor monitor) {
        var refreshEndpoint = refreshEndpointConfig;
        if (refreshEndpoint == null) {
            refreshEndpoint = "http://%s:%d%s".formatted(hostname.get(), webPort, webPath);
            monitor.warning("Config property '%s' was not specified, the default '%s' will be used.".formatted(REFRESH_ENDPOINT_PROPERTY, refreshEndpoint));
        }
        return refreshEndpoint;
    }
}
