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

package org.eclipse.tractusx.edc.tests.participant;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Specialized version of {@link TractusxParticipantBase} with IATP configurations
 */
public abstract class TractusxIatpParticipantBase extends TractusxParticipantBase {

    protected URI stsUri;
    protected String stsClientId;
    protected String stsClientSecret;
    protected String trustedIssuer;

    public Map<String, String> iatpConfiguration(TractusxIatpParticipantBase... others) {
        var iatpConfiguration = new HashMap<>(getConfiguration()) {
            {

                put("edc.iam.sts.oauth.token.url", stsUri + "/token");
                put("edc.iam.sts.oauth.client.id", stsClientId);
                put("edc.iam.sts.oauth.client.secret.alias", "client_secret_alias");
                put("edc.ih.iam.id", getDid());
                put("tx.edc.vault.seed.secrets", "client_secret_alias:%s".formatted(stsClientSecret));
                put("edc.ih.iam.publickey.alias", getFullKeyId());
                put("edc.agent.identity.key", "client_id");
                put("edc.iam.trusted-issuer.issuer.id", trustedIssuer);
                put("edc.transfer.proxy.token.signer.privatekey.alias", getPrivateKeyAlias());
                put("edc.transfer.proxy.token.verifier.publickey.alias", getFullKeyId());
            }
        };

        Stream.concat(Stream.of(this), Arrays.stream(others)).forEach(p -> {
            var prefix = "tx.edc.iam.iatp.audiences.%s".formatted(p.getName().toLowerCase());
            iatpConfiguration.put("%s.from".formatted(prefix), p.getBpn());
            iatpConfiguration.put("%s.to".formatted(prefix), p.getDid());
        });
        return iatpConfiguration;
    }

    public static class Builder<P extends TractusxIatpParticipantBase, B extends Builder<P, B>> extends TractusxParticipantBase.Builder<P, B> {

        protected Builder(P participant) {
            super(participant);
        }

        public B stsUri(URI stsUri) {
            participant.stsUri = stsUri;
            return self();
        }

        public B stsClientId(String stsClientId) {
            participant.stsClientId = stsClientId;
            return self();
        }

        public B stsClientSecret(String stsClientSecret) {
            participant.stsClientSecret = stsClientSecret;
            return self();
        }

        public B trustedIssuer(String trustedIssuer) {
            participant.trustedIssuer = trustedIssuer;
            return self();
        }

        @Override
        public P build() {
            super.build();
            Objects.requireNonNull(participant.stsUri, "STS URI should not be null");
            Objects.requireNonNull(participant.trustedIssuer, "Trusted issuer cannot be null");

            if (participant.stsClientId == null) {
                participant.stsClientId = participant.id;
            }

            if (participant.stsClientSecret == null) {
                participant.stsClientSecret = "clientSecret";
            }
            return participant;
        }
    }

}
