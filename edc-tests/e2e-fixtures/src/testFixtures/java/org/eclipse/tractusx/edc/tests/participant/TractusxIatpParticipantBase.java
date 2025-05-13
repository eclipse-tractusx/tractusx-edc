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

import org.eclipse.edc.connector.controlplane.test.system.utils.LazySupplier;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

/**
 * Specialized version of {@link TractusxParticipantBase} with IATP configurations
 */
public abstract class TractusxIatpParticipantBase extends TractusxParticipantBase {

    protected LazySupplier<URI> stsUri;
    protected String stsClientId;
    protected String trustedIssuer;

    public Config iatpConfig() {
        var additionalSettings = Map.of(
                "edc.iam.sts.oauth.token.url", stsUri.get() + "/token",
                "edc.iam.sts.oauth.client.id", getDid(),
                "edc.iam.sts.oauth.client.secret.alias", "client_secret_alias",
                "edc.ih.iam.id", getDid(),
                "edc.ih.iam.publickey.alias", getFullKeyId(),
                "edc.agent.identity.key", "client_id",
                "edc.iam.trusted-issuer.issuer.id", trustedIssuer,
                "edc.transfer.proxy.token.signer.privatekey.alias", getPrivateKeyAlias(),
                "edc.transfer.proxy.token.verifier.publickey.alias", getFullKeyId(),
                "edc.iam.did.web.use.https", "false"
        );

        return getConfig().merge(ConfigFactory.fromMap(additionalSettings));
    }

    public static class Builder<P extends TractusxIatpParticipantBase, B extends Builder<P, B>> extends TractusxParticipantBase.Builder<P, B> {

        protected Builder(P participant) {
            super(participant);
        }

        public B stsUri(LazySupplier<URI> stsUri) {
            participant.stsUri = stsUri;
            return self();
        }

        public B stsClientId(String stsClientId) {
            participant.stsClientId = stsClientId;
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

            return participant;
        }
    }

}
