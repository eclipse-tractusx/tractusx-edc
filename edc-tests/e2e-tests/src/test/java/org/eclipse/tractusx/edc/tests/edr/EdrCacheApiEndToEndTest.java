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

package org.eclipse.tractusx.edc.tests.edr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.json.JsonObject;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.tractusx.edc.api.edr.v2.EdrCacheApiController;
import org.eclipse.tractusx.edc.lifecycle.ParticipantRuntime;
import org.eclipse.tractusx.edc.lifecycle.tx.TxParticipant;
import org.eclipse.tractusx.edc.spi.tokenrefresh.dataplane.model.TokenResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.verify.VerificationTimes;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_AUTH_NS;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.StringBody.exact;

/**
 * This End-To-End test spins up a consumer control plane and verifies that the EDR Cache API ({@link EdrCacheApiController})
 * performs as expected.
 * The provider data plane is mocked with a {@link ClientAndServer}.
 */
@EndToEndTest
public class EdrCacheApiEndToEndTest {

    protected static final TxParticipant SOKRATES = TxParticipant.Builder.newInstance()
            .name(SOKRATES_NAME)
            .id(SOKRATES_BPN)
            .build();
    @RegisterExtension
    protected static final ParticipantRuntime SOKRATES_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:runtime-memory",
            SOKRATES.getName(),
            SOKRATES.getBpn(),
            with(SOKRATES.getConfiguration(), Map.of("edc.iam.issuer.id", "did:web:sokrates")));
    public static String refreshEndpoint = "http://fizz.buzz.com";
    private final ObjectMapper mapper = new ObjectMapper();
    private ClientAndServer mockedRefreshApi;
    private ECKey providerSigningKey;

    protected static Map<String, String> with(Map<String, String> original, Map<String, String> additional) {
        var newMap = new HashMap<>(original);
        newMap.putAll(additional);
        return newMap;
    }

    @BeforeEach
    void setup() throws JOSEException {
        providerSigningKey = new ECKeyGenerator(Curve.P_256).keyID("did:web:provider#key-1").generate();
        var port = getFreePort();
        refreshEndpoint = "http://localhost:%s/refresh".formatted(port);
        mockedRefreshApi = startClientAndServer(port);
    }

    @AfterEach
    void teardown() {
        mockedRefreshApi.stop();
    }

    @Test
    void refreshEdr_success() {

        try (var client = new MockServerClient("localhost", mockedRefreshApi.getPort())) {
            // mock the provider dataplane's refresh endpoint
            client.when(request()
                                    .withMethod("POST")
                                    .withPath("/refresh/token")
                                    .withBody(exact("")),
                            exactly(1))
                    .respond(response()
                            .withStatusCode(200)
                            .withBody(tokenResponseBody())
                    );

            storeEdr("test-id");
            var edr = SOKRATES.edrs().getEdrRequestV1("test-id", true)
                    .statusCode(200)
                    .extract().body().as(JsonObject.class);
            assertThat(edr).isNotNull();

            // assert the correct endpoint was called
            client.verify(
                    request()
                            .withQueryStringParameter("grant_type", "refresh_token")
                            .withMethod("POST")
                            .withPath("/refresh/token"),
                    VerificationTimes.exactly(1));

        }
    }

    @Test
    void refreshEdr_unauthorized() {
        try (var client = new MockServerClient("localhost", mockedRefreshApi.getPort())) {
            // mock the provider dataplane's refresh endpoint
            client.when(request()
                                    .withMethod("POST")
                                    .withPath("/refresh/token")
                                    .withBody(exact("")),
                            exactly(1))
                    .respond(response()
                            .withStatusCode(401)
                            .withBody("unauthorized")
                    );

            storeEdr("test-id");
            SOKRATES.edrs().getEdrRequestV1("test-id", true)
                    .statusCode(403);

            // assert the correct endpoint was called
            client.verify(
                    request()
                            .withQueryStringParameter("grant_type", "refresh_token")
                            .withMethod("POST")
                            .withPath("/refresh/token"),
                    VerificationTimes.exactly(1));

        }
    }

    private String tokenResponseBody() {
        var claims = new JWTClaimsSet.Builder().claim("iss", "did:web:provider").build();

        var accessToken = createJwt(providerSigningKey, claims);
        var refreshToken = createJwt(providerSigningKey, new JWTClaimsSet.Builder().build());
        var response = new TokenResponse(accessToken, refreshToken, 300L, "bearer");
        try {
            return mapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Object storeEdr(String transferProcessId) {
        var claims = new JWTClaimsSet.Builder().claim("iss", "did:web:provider").build();
        var store = SOKRATES_RUNTIME.getService(EndpointDataReferenceStore.class);
        var edr = DataAddress.Builder.newInstance()
                .type("test-type")
                .property(EDC_NAMESPACE + "authorization", createJwt(providerSigningKey, claims))
                .property(EDC_NAMESPACE + "authType", "bearer")
                .property(TX_AUTH_NS + "refreshToken", createJwt(providerSigningKey, new JWTClaimsSet.Builder().build()))
                .property(TX_AUTH_NS + "expiresIn", "300")
                .property(TX_AUTH_NS + "refreshEndpoint", refreshEndpoint)
                .build();
        var entry = EndpointDataReferenceEntry.Builder.newInstance()
                .agreementId("test-agreement")
                .assetId("test-asset")
                .transferProcessId(transferProcessId)
                .providerId("test-provider")
                .contractNegotiationId("test-negotiation")
                .build();
        store.save(entry, edr).orElseThrow(f -> new AssertionError(f.getFailureDetail()));
        return edr;
    }


    private String createJwt(ECKey signerKey, JWTClaimsSet claims) {
        var header = new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signerKey.getKeyID()).build();
        var jwt = new SignedJWT(header, claims);
        try {
            jwt.sign(new ECDSASigner(signerKey));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

}
