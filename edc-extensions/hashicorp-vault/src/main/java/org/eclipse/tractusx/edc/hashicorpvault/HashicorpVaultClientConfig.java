/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.hashicorpvault;


import java.time.Duration;

class HashicorpVaultClientConfig {
    private String vaultUrl;
    private String vaultToken;
    private String vaultApiSecretPath;
    private String vaultApiHealthPath;
    private Duration timeout;
    private boolean isVaultApiHealthStandbyOk;

    public String getVaultUrl() {
        return vaultUrl;
    }

    public String getVaultToken() {
        return vaultToken;
    }

    public String getVaultApiSecretPath() {
        return vaultApiSecretPath;
    }

    public String getVaultApiHealthPath() {
        return vaultApiHealthPath;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public boolean isVaultApiHealthStandbyOk() {
        return isVaultApiHealthStandbyOk;
    }

    public static final class Builder {
        private final HashicorpVaultClientConfig config;

        private Builder() {
            config = new HashicorpVaultClientConfig();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder vaultUrl(String vaultUrl) {
            this.config.vaultUrl = vaultUrl;
            return this;
        }

        public Builder vaultToken(String vaultToken) {
            this.config.vaultToken = vaultToken;
            return this;
        }

        public Builder vaultApiSecretPath(String vaultApiSecretPath) {
            this.config.vaultApiSecretPath = vaultApiSecretPath;
            return this;
        }

        public Builder vaultApiHealthPath(String vaultApiHealthPath) {
            this.config.vaultApiHealthPath = vaultApiHealthPath;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.config.timeout = timeout;
            return this;
        }

        public Builder isVaultApiHealthStandbyOk(boolean isVaultApiHealthStandbyOk) {
            this.config.isVaultApiHealthStandbyOk = isVaultApiHealthStandbyOk;
            return this;
        }

        public HashicorpVaultClientConfig build() {
            return this.config;
        }
    }
}
