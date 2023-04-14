package org.eclipse.tractusx.edc.tests.data;

import java.util.Objects;


public class HttpProxySourceDataAddress implements DataAddress {
    private final String baseUrl;
    private final Oauth2Provision oauth2Provision;

    public HttpProxySourceDataAddress(String baseUrl, Oauth2Provision oauth2Provision) {
        this.baseUrl = Objects.requireNonNull(baseUrl);
        this.oauth2Provision = oauth2Provision;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Oauth2Provision getOauth2Provision() {
        return oauth2Provision;
    }

    public static class Oauth2Provision {
        private final String tokenUrl;
        private final String clientId;
        private final String clientSecret;
        private final String scope;

        public Oauth2Provision(String tokenUrl, String clientId, String clientSecret, String scope) {
            this.tokenUrl = Objects.requireNonNull(tokenUrl);
            this.clientId = Objects.requireNonNull(clientId);
            this.clientSecret = Objects.requireNonNull(clientSecret);
            this.scope = scope;
        }

        public String getTokenUrl() {
            return tokenUrl;
        }

        public String getScope() {
            return scope;
        }

        public String getClientId() {
            return clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }
    }
}
