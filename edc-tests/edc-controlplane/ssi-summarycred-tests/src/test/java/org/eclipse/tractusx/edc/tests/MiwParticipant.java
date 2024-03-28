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

package org.eclipse.tractusx.edc.tests;

import org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.edc.util.io.Ports.getFreePort;

public class MiwParticipant extends TractusxParticipantBase {

    private final URI miwUri = URI.create("http://localhost:" + getFreePort());
    private final URI oauthTokenUri = URI.create("http://localhost:" + getFreePort());

    /**
     * Returns the SSI configuration
     */
    public Map<String, String> getConfiguration() {
        return new HashMap<>(super.getConfiguration()) {
            {
                put("tx.ssi.miw.url", miwUri.toString());
                put("tx.ssi.oauth.token.url", oauthTokenUri.toString());
                put("tx.ssi.oauth.client.id", "client_id");
                put("tx.ssi.oauth.client.secret.alias", "client_secret_alias");
                put("tx.ssi.miw.authority.id", "authorityId");
                put("tx.ssi.miw.authority.issuer", "did:web:example.com");
                put("tx.vault.seed.secrets", "client_secret_alias:client_secret");
                put("tx.ssi.endpoint.audience", getProtocolEndpoint().getUrl().toString());
            }
        };
    }

    /**
     * Returns the MIW endpoint
     */
    public URI miwEndpoint() {
        return miwUri;
    }

    /**
     * Returns the OAuth2 token endpoint
     */
    public URI authTokenEndpoint() {
        return oauthTokenUri;
    }

    public static class Builder extends TractusxParticipantBase.Builder<MiwParticipant, Builder> {

        protected Builder() {
            super(new MiwParticipant());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public MiwParticipant build() {
            super.build();
            return participant;
        }
    }
}