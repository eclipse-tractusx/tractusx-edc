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

package org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.configuration;

import static java.util.Objects.requireNonNull;

/**
 * A configuration that exposes a proxied endpoint via an alias. Each configuration is associated with an extensible {@code authorizationType} such as
 * {@link #NO_AUTHORIZATION} (the default) and {@link #NO_AUTHORIZATION}. The {@code proxiedPath} will be prepended to a request sub-path to create an absolute endpoint
 * URL where data is fetched from.
 */
public class GatewayConfiguration {
    public static final String NO_AUTHORIZATION = "none";

    private String alias;
    private String proxiedPath;
    private String authorizationType = NO_AUTHORIZATION;

    private boolean forwardEdrToken;
    private String forwardEdrTokenHeaderKey;


    private GatewayConfiguration() {
    }

    public String getAlias() {
        return alias;
    }

    public String getProxiedPath() {
        return proxiedPath;
    }

    public String getAuthorizationType() {
        return authorizationType;
    }

    public boolean isForwardEdrToken() {
        return forwardEdrToken;
    }

    public String getForwardEdrTokenHeaderKey() {
        return forwardEdrTokenHeaderKey;
    }

    public static class Builder {

        private final GatewayConfiguration configuration;

        private Builder() {
            configuration = new GatewayConfiguration();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder alias(String alias) {
            this.configuration.alias = alias;
            return this;
        }

        public Builder proxiedPath(String proxiedPath) {
            this.configuration.proxiedPath = proxiedPath;
            return this;
        }

        public Builder authorizationType(String authorizationType) {
            this.configuration.authorizationType = authorizationType;
            return this;
        }

        public Builder forwardEdrToken(boolean forwardEdrToken) {
            this.configuration.forwardEdrToken = forwardEdrToken;
            return this;
        }

        public Builder forwardEdrTokenHeaderKey(String forwardEdrTokenHeaderKey) {
            this.configuration.forwardEdrTokenHeaderKey = forwardEdrTokenHeaderKey;
            return this;
        }

        public GatewayConfiguration build() {
            requireNonNull(configuration.alias, "alias");
            requireNonNull(configuration.proxiedPath, "proxiedPath");
            requireNonNull(configuration.authorizationType, "authorizationType");
            return configuration;
        }
    }
}
