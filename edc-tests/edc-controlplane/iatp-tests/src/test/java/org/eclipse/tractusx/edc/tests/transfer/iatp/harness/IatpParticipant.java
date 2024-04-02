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

package org.eclipse.tractusx.edc.tests.transfer.iatp.harness;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.tractusx.edc.tests.participant.TractusxIatpParticipantBase;

import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.edc.util.io.Ports.getFreePort;

public class IatpParticipant extends TractusxIatpParticipantBase {

    protected final URI csService = URI.create("http://localhost:" + getFreePort() + "/api/resolution");
    protected URI dimUri;

    private DidDocument didDocument;

    public DidDocument getDidDocument() {
        return didDocument;
    }

    public String verificationId() {
        return did + "#" + getKeyId();
    }

    @Override
    public Map<String, String> getConfiguration() {
        var cfg = new HashMap<>(super.getConfiguration());
        cfg.put("web.http.resolution.port", String.valueOf(csService.getPort()));
        cfg.put("web.http.resolution.path", csService.getPath());
        if (dimUri != null) {
            cfg.put("edc.iam.sts.dim.url", dimUri.toString());
        }
        return cfg;
    }

    public static class Builder extends TractusxIatpParticipantBase.Builder<IatpParticipant, Builder> {

        protected Builder() {
            super(new IatpParticipant());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public IatpParticipant build() {
            super.build();
            participant.didDocument = generateDidDocument();
            return participant;
        }

        public Builder dimUri(URI dimUri) {
            participant.dimUri = dimUri;
            return self();
        }

        private DidDocument generateDidDocument() {
            var service = new Service();
            service.setId("#credential-service");
            service.setType("CredentialService");
            service.setServiceEndpoint(participant.csService + "/v1/participants/" + toBase64(participant.did));

            var ecKey = participant.getKeyPairAsJwk();

            var verificationMethod = VerificationMethod.Builder.newInstance()
                    .id(participant.verificationId())
                    .controller(participant.did)
                    .type("JsonWebKey2020")
                    .publicKeyJwk(ecKey.toPublicJWK().toJSONObject())
                    .build();

            return DidDocument.Builder.newInstance()
                    .id(participant.did)
                    .service(List.of(service))
                    .authentication(List.of("#key1"))
                    .verificationMethod(List.of(verificationMethod))
                    .build();
        }


        private String toBase64(String s) {
            return Base64.getUrlEncoder().encodeToString(s.getBytes());
        }

    }
}
