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

package org.eclipse.tractusx.edc.iam.ssi.miw.oauth2;

/**
 * Configuration of the OAuth2 client
 */
public class MiwOauth2ClientConfiguration {
    private String tokenUrl;
    private String clientId;

    private String clientSecret;
    private String scope;

    public String getScope() {
        return scope;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public static class Builder {
        private final MiwOauth2ClientConfiguration configuration = new MiwOauth2ClientConfiguration();

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder tokenUrl(String url) {
            configuration.tokenUrl = url;
            return this;
        }

        public Builder clientId(String clientId) {
            configuration.clientId = clientId;
            return this;
        }

        public Builder scope(String scope) {
            configuration.scope = scope;
            return this;
        }

        public Builder clientSecret(String clientSecret) {
            configuration.clientSecret = clientSecret;
            return this;
        }

        public MiwOauth2ClientConfiguration build() {
            return configuration;
        }
    }
}
