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

package org.eclipse.tractusx.edc.iam.ssi.miw.config;

import java.util.Objects;

public class SsiMiwConfiguration {

    protected String url;
    protected String authorityId;
    protected String authorityIssuer;

    public String getAuthorityId() {
        return authorityId;
    }

    public String getUrl() {
        return url;
    }

    public String getAuthorityIssuer() {
        return authorityIssuer;
    }

    public static class Builder {
        private final SsiMiwConfiguration config;

        private Builder() {
            config = new SsiMiwConfiguration();
        }

        public static Builder newInstance() {
            return new Builder();
        }
        
        public Builder url(String url) {
            config.url = url;
            return this;
        }

        public Builder authorityId(String authorityId) {
            config.authorityId = authorityId;
            return this;
        }

        public Builder authorityIssuer(String authorityIssuer) {
            config.authorityIssuer = authorityIssuer;
            return this;
        }

        public SsiMiwConfiguration build() {
            Objects.requireNonNull(config.url);
            Objects.requireNonNull(config.authorityIssuer);
            Objects.requireNonNull(config.authorityId);
            return config;
        }
    }
}
