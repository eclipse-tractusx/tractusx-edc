/*******************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2025 Cofinity-X GmbH
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
 ******************************************************************************/

package org.eclipse.tractusx.edc.compatibility.tests.fixtures;

import org.eclipse.edc.junit.utils.LazySupplier;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.eclipse.edc.util.io.Ports.getFreePort;

public class IdentityHubParticipant {

    protected final LazySupplier<URI> sts = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/api/sts"));
    protected final LazySupplier<URI> accountsApi = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/api/accounts"));
    protected final LazySupplier<URI> credentialsApi = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/api/credentials"));
    protected final LazySupplier<URI> identityApi = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/api/identity"));
    protected final LazySupplier<URI> didApi = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/"));
    protected String id;
    protected String name;

    public Config getConfig() {
        Map<String, String> settings = new HashMap<>();

        settings.put("web.http.port", String.valueOf(getFreePort()));
        settings.put("web.http.path", "/api");
        settings.put("web.http.credentials.port", String.valueOf(credentialsApi.get().getPort()));
        settings.put("web.http.credentials.path", credentialsApi.get().getPath());
        settings.put("web.http.identity.port", String.valueOf(identityApi.get().getPort()));
        settings.put("web.http.identity.path", identityApi.get().getPath());
        settings.put("web.http.sts.port", String.valueOf(sts.get().getPort()));
        settings.put("web.http.sts.path", sts.get().getPath());
        settings.put("web.http.accounts.port", String.valueOf(accountsApi.get().getPort()));
        settings.put("web.http.accounts.path", accountsApi.get().getPath());
        settings.put("web.http.did.port", String.valueOf(didApi.get().getPort()));
        settings.put("web.http.did.path", didApi.get().getPath());
        settings.put("edc.iam.did.web.use.https", "false");
        settings.put("edc.api.accounts.key", "password");

        return ConfigFactory.fromMap(settings);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LazySupplier<URI> getSts() {
        return sts;
    }

    public URI getResolutionApi() {
        return credentialsApi.get();
    }

    public String didFor(String participantId) {
        var didUri = didApi.get();
        return "did:web:" + URLEncoder.encode(didUri.getHost() + ":" + didUri.getPort(), StandardCharsets.UTF_8) + ":" + participantId;
    }

    public static class Builder {
        protected final IdentityHubParticipant participant;

        protected Builder() {
            this.participant = new IdentityHubParticipant();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            this.participant.id = id;
            return this;
        }

        public Builder name(String name) {
            this.participant.name = name;
            return this;
        }

        public IdentityHubParticipant build() {
            Objects.requireNonNull(this.participant.id, "id");
            Objects.requireNonNull(this.participant.name, "name");

            return this.participant;
        }
    }
}
