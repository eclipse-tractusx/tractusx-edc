/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.lifecycle;

import org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Extension of {@link TractusxParticipantBase} with MIW specific configuration
 */
public class MiwParticipant extends TractusxParticipantBase {

    protected String miwUri;
    protected String oauth2Uri;

    public Map<String, String> getConfiguration() {
        var baseConfiguration = super.getConfiguration();

        var ssiConfiguration = new HashMap<String, String>() {
            {
                put("tx.ssi.miw.url", miwUri);
                put("tx.ssi.oauth.token.url", oauth2Uri);
                put("tx.ssi.oauth.client.id", "miw_private_client");
                put("tx.ssi.oauth.client.secret.alias", "client_secret_alias");
                put("tx.ssi.miw.authority.id", "BPNL000000000000");
                put("tx.ssi.miw.authority.issuer", "did:web:localhost%3A8000:BPNL000000000000");
                put("tx.vault.seed.secrets", "client_secret_alias:miw_private_client");
                put("tx.ssi.endpoint.audience", getProtocolEndpoint().getUrl().toString());
            }
        };
        ssiConfiguration.putAll(baseConfiguration);
        return ssiConfiguration;
    }

    public static class Builder extends TractusxParticipantBase.Builder<MiwParticipant, Builder> {

        protected Builder() {
            super(new MiwParticipant());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder miwUri(String miwUri) {
            participant.miwUri = miwUri;
            return self();
        }

        public Builder oauth2Uri(String oauth2Uri) {
            participant.oauth2Uri = oauth2Uri;
            return self();
        }

        @Override
        public MiwParticipant build() {
            super.build();
            Objects.requireNonNull(participant.miwUri, "MIW URI should not be null");
            return participant;
        }
    }
}
