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

package org.eclipse.tractusx.edc.lifecycle.tx.iatp;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.tractusx.edc.lifecycle.tx.TxParticipant;

import java.net.URI;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.tractusx.edc.helpers.IatpHelperFunctions.generateKeyPair;
import static org.eclipse.tractusx.edc.helpers.IatpHelperFunctions.toPemEncoded;
import static org.eclipse.tractusx.edc.lifecycle.tx.iatp.DataspaceIssuer.DATASPACE_ISSUER;

/**
 * Wrapper of {@link TxParticipant} with IATP specific configurations
 */
public class IatpParticipant {

    public static final String KEY_ID = "key1";
    public static final String DID_EXAMPLE = "did:example:";
    protected final URI csService = URI.create("http://localhost:" + getFreePort() + "/api/resolution");
    private final TxParticipant participant;
    private final URI stsUri;
    private final KeyPair keyPair;
    private final DidDocument didDocument;
    private final String privateKey;
    private final String publicKey;

    public IatpParticipant(TxParticipant participant, URI stsUri) {
        this.participant = participant;
        this.stsUri = stsUri;
        this.keyPair = generateKeyPair();
        this.didDocument = generateDidDocument();
        this.privateKey = toPemEncoded(keyPair.getPrivate());
        this.publicKey = toPemEncoded(keyPair.getPublic());
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
                put("edc.ih.iam.publickey.pem", publicKey());
                put("web.http.resolution.port", String.valueOf(csService.getPort()));
                put("web.http.resolution.path", csService.getPath());
                put("edc.agent.identity.key", "client_id");
                put("edc.iam.trusted-issuer.issuer.id", DATASPACE_ISSUER);
            }
        };

        Stream.concat(Stream.of(participant), Arrays.stream(others)).forEach(p -> {
            var prefix = format("tx.iam.iatp.audiences.%s", p.getName().toLowerCase());
            var participantDid = DID_EXAMPLE + p.getName().toLowerCase();
            iatpConfiguration.put(prefix + "_endpoint" + ".from", p.getProtocolEndpoint().getUrl().toString());
            iatpConfiguration.put(prefix + "_endpoint" + ".to", participantDid);
            iatpConfiguration.put(prefix + "_id" + ".from", p.getBpn());
            iatpConfiguration.put(prefix + "_id" + ".to", participantDid);
        });
        return iatpConfiguration;
    }

    public String didUrl() {
        return DID_EXAMPLE + participant.getName().toLowerCase();
    }

    public String verificationId() {
        return didUrl() + "#" + KEY_ID;
    }

    public String keyId() {
        return KEY_ID;
    }

    public String privateKey() {
        return privateKey;
    }

    public String publicKey() {
        return publicKey;
    }

    public DidDocument didDocument() {
        return didDocument;
    }


    public KeyPair getKeyPair() {
        return keyPair;
    }

    private DidDocument generateDidDocument() {
        var service = new Service();
        service.setId("#credential-service");
        service.setType("CredentialService");
        service.setServiceEndpoint(csService + "/v1/participants/" + toBase64(didUrl()));

        var ecKey = new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic())
                .privateKey((ECPrivateKey) keyPair.getPrivate())
                .build();

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
