/*******************************************************************************
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
 ******************************************************************************/

package org.eclipse.tractusx.edc.compatibility.tests.fixtures;

import com.nimbusds.jose.jwk.JWK;
import io.restassured.common.mapper.TypeRef;
import org.assertj.core.api.ThrowingConsumer;
import org.eclipse.edc.connector.controlplane.test.system.utils.Participant;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.eclipse.edc.security.token.jwt.CryptoConverter;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.testcontainers.shaded.org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.net.URI;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.ECGenParameterSpec;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.util.io.Ports.getFreePort;

public abstract class BaseParticipant extends Participant {

    protected final LazySupplier<URI> controlPlaneControl = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/control"));
    protected final LazySupplier<URI> dataPlaneControl = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/control"));
    protected final LazySupplier<URI> dataPlanePublic = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/public"));
    protected final LazySupplier<URI> consumerPublic = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/public"));
    protected final LazySupplier<URI> controlPlaneVersion = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/version"));
    protected final LazySupplier<URI> dataPlaneVersion = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/version"));
    protected URI sts;
    protected KeyPair keyPair;
    protected JWK keyPairJwk;
    protected String did;
    protected String trustedIssuer;

    public static KeyPair generateKeyPair() {
        try {
            var gen = KeyPairGenerator.getInstance("EC", new BouncyCastleProvider());
            gen.initialize(new ECGenParameterSpec("secp256r1"));
            return gen.generateKeyPair();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    public JWK getKeyPairJwk() {
        return keyPairJwk;
    }

    public String getKeyId() {
        return "key-1";
    }

    public String getDid() {
        return did;
    }

    public String getFullKeyId() {
        return getDid() + "#" + getKeyId();
    }

    public String getPrivateKeyAlias() {
        return "private." + getFullKeyId();
    }

    public String getPrivateKeyAsString() {
        return keyPairJwk.toJSONString();
    }

    public String getPublicKeyAsString() {
        return keyPairJwk.toPublicJWK().toJSONString();
    }


    /**
     * Pull data from provider using EDR.
     *
     * @param edr           endpoint data reference
     * @param queryParams   query parameters
     * @param bodyAssertion assertion to be verified on the body
     */
    public void pullData(DataAddress edr, Map<String, String> queryParams, ThrowingConsumer<String> bodyAssertion) {
        var data = given()
                .baseUri(edr.getStringProperty("endpoint"))
                .header("Authorization", edr.getStringProperty("authorization"))
                .queryParams(queryParams)
                .when()
                .get()
                .then()
                .log().ifError()
                .statusCode(200)
                .extract().body().asString();

        assertThat(data).satisfies(bodyAssertion);
    }

    public void waitForDataPlane() {
        await().atMost(timeout)
                .untilAsserted(() -> {
                    var jp = baseManagementRequest()
                            .get("/v3/dataplanes")
                            .then()
                            .statusCode(200)
                            .log().ifValidationFails()
                            .extract().body().jsonPath();

                    var state = jp.getString("state");
                    assertThat(state).isIn("[AVAILABLE]", "[null]");
                });

    }

    /**
     * Get the EDR from the EDR cache by transfer process id.
     *
     * @param transferProcessId The transfer process id
     * @return The cached {@link DataAddress}
     */
    public DataAddress getEdr(String transferProcessId) {
        var dataAddressRaw = baseManagementRequest()
                .contentType(JSON)
                .when()
                .get("/v3/edrs/{id}/dataaddress", transferProcessId)
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(JSON)
                .extract().body().as(new TypeRef<Map<String, Object>>() {
                });


        var builder = DataAddress.Builder.newInstance();
        dataAddressRaw.forEach(builder::property);
        return builder.build();

    }

    public static class Builder<P extends BaseParticipant, B extends Participant.Builder<P, B>> extends Participant.Builder<P, B> {

        protected Builder(P participant) {
            super(participant);
        }

        public B sts(URI sts) {
            participant.sts = sts;
            return self();
        }

        public B did(String did) {
            participant.did = did;
            return self();
        }

        public B trustedIssuer(String trustedIssuer) {
            participant.trustedIssuer = trustedIssuer;
            return self();
        }

        @Override
        public P build() {
            super.build();

            if (participant.did == null) {
                participant.did = "did:web:" + participant.name.toLowerCase();
            }

            participant.keyPair = generateKeyPair();

            var jwk = CryptoConverter.createJwk(participant.keyPair).toJSONObject();
            jwk.put("kid", participant.did + "#key-1");
            participant.keyPairJwk = CryptoConverter.create(jwk);
            return this.participant;
        }
    }
}
