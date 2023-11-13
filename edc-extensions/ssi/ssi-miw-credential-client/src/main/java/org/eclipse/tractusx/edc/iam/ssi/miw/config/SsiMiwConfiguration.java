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

package org.eclipse.tractusx.edc.iam.ssi.miw.config;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class SsiMiwConfiguration {

    protected String url;
    protected String authorityId;
    protected Set<String> authorityIssuers = new HashSet<>();

    public String getAuthorityId() {
        return authorityId;
    }

    public String getUrl() {
        return url;
    }

    public Set<String> getAuthorityIssuers() {
        return authorityIssuers;
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

        public Builder authorityIssuers(Set<String> authorityIssuers) {
            config.authorityIssuers = authorityIssuers;
            return this;
        }
        
        public SsiMiwConfiguration build() {
            Objects.requireNonNull(config.url);
            Objects.requireNonNull(config.authorityIssuers);
            Objects.requireNonNull(config.authorityId);
            return config;
        }
    }
}
