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

package org.eclipse.tractusx.edc.tests.transfer.iatp;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.tractusx.edc.tests.IdentityParticipant;
import org.eclipse.tractusx.edc.tests.TxParticipant;

import java.net.URI;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.tests.transfer.iatp.harness.DataspaceIssuer.DATASPACE_ISSUER;

/**
 * Wrapper of {@link TxParticipant} with IATP specific configurations
 */
public class IatpParticipant extends IdentityParticipant {

    public static final String DID_EXAMPLE = "did:example:";
    protected final URI csService = URI.create("http://localhost:" + getFreePort() + "/api/resolution");
    private final TxParticipant participant;
    private final URI stsUri;
    private final DidDocument didDocument;

    public IatpParticipant(TxParticipant participant, URI stsUri) {
        this.participant = participant;
        this.stsUri = stsUri;
        this.didDocument = generateDidDocument();
    }

    public String getBpn() {
        return participant.getBpn();
    }

    public String getName() {
        return participant.getName();
    }

    public Map<String, String> iatpConfiguration(TxParticipant... others) {
        var did = DID_EXAMPLE + participant.getName().toLowerCase();
        var iatpConfiguration = new HashMap<>(participant.getConfiguration()) {
            {

                put("edc.iam.sts.oauth.token.url", stsUri + "/token");
                put("edc.iam.sts.oauth.client.id", getBpn());
                put("edc.iam.sts.oauth.client.secret.alias", "client_secret_alias");
                put("edc.iam.issuer.id", did);
                put("edc.ih.iam.id", participant.getBpn());
                put("tx.vault.seed.secrets", "client_secret_alias:client_secret");
                put("edc.ih.iam.publickey.alias", getFullKeyId());
                put("web.http.resolution.port", String.valueOf(csService.getPort()));
                put("web.http.resolution.path", csService.getPath());
                put("edc.agent.identity.key", "client_id");
                put("edc.iam.trusted-issuer.issuer.id", DATASPACE_ISSUER);

                put("edc.transfer.proxy.token.signer.privatekey.alias", getPrivateKeyAlias());
                put("edc.transfer.proxy.token.verifier.publickey.alias", getFullKeyId());
            }
        };

        Stream.concat(Stream.of(participant), Arrays.stream(others)).forEach(p -> {
            var prefix = "tx.iam.iatp.audiences.%s".formatted(p.getName().toLowerCase());
            var participantDid = DID_EXAMPLE + p.getName().toLowerCase();
            iatpConfiguration.put("%s_endpoint.from".formatted(prefix), p.getProtocolEndpoint().getUrl().toString());
            iatpConfiguration.put("%s_endpoint.to".formatted(prefix), participantDid);
            iatpConfiguration.put("%s_id.from".formatted(prefix), p.getBpn());
            iatpConfiguration.put("%s_id.to".formatted(prefix), participantDid);
        });
        return iatpConfiguration;
    }

    public String didUrl() {
        return DID_EXAMPLE + participant.getName().toLowerCase();
    }

    public String verificationId() {
        return didUrl() + "#" + getKeyId();
    }

    public DidDocument didDocument() {
        return didDocument;
    }

    @Override
    public String getFullKeyId() {
        return verificationId();
    }


    private DidDocument generateDidDocument() {
        var service = new Service();
        service.setId("#credential-service");
        service.setType("CredentialService");
        service.setServiceEndpoint(csService + "/v1/participants/" + toBase64(didUrl()));

        var ecKey = getKeyPairAsJwk();

        var verificationMethod = VerificationMethod.Builder.newInstance()
                .id(verificationId())
                .controller(didUrl())
                .type("JsonWebKey2020")
                .publicKeyJwk(ecKey.toPublicJWK().toJSONObject())
                .build();

        return DidDocument.Builder.newInstance()
                .id(didUrl())
                .service(List.of(service))
                .authentication(List.of("#key1"))
                .verificationMethod(List.of(verificationMethod))
                .build();
    }

    private String toBase64(String s) {
        return Base64.getUrlEncoder().encodeToString(s.getBytes());
    }
}
