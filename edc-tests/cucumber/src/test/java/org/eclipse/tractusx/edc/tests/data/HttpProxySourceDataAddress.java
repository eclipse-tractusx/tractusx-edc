/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

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
