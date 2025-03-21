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

package org.eclipse.tractusx.edc.tests.edrv2;


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
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.tractusx.edc.spi.tokenrefresh.dataplane.model.TokenResponse;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.PostgresExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Delay;
import org.mockserver.verify.VerificationTimes;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_EXPIRES_IN;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_REFRESH_AUDIENCE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_REFRESH_ENDPOINT;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_REFRESH_TOKEN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.memoryRuntime;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.StringBody.exact;

/**
 * This End-To-End test spins up a consumer control plane and verifies that the EDR Cache API
 * performs as expected.
 * The provider data plane is mocked with a {@link ClientAndServer}.
 */
@EndToEndTest
public class EdrCacheApiEndToEndTest {

    private static final TransferParticipant CONSUMER = TransferParticipant.Builder.newInstance()
            .name(CONSUMER_NAME)
            .id(CONSUMER_BPN)
            .build();

    abstract static class Tests {
        private final Random random = new Random();
        private final ObjectMapper mapper = new ObjectMapper();
        private ClientAndServer mockedRefreshApi;
        private ECKey providerSigningKey;
        private String refreshEndpoint;
        private String refreshAudience;

        @BeforeEach
        void setup() throws JOSEException {
            providerSigningKey = new ECKeyGenerator(Curve.P_256).keyID("did:web:provider#key-1").generate();
            var port = getFreePort();
            refreshEndpoint = "http://localhost:%s/refresh".formatted(port);
            refreshAudience = "did:web:consumer";
            mockedRefreshApi = startClientAndServer(port);
        }

        @AfterEach
        void teardown() {
            mockedRefreshApi.stop();
        }

        @DisplayName("Verify HTTP 200 response and body when refreshing succeeds")
        @Test
        void getEdrWithRefresh_success() {

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

                storeEdr("test-id", true);
                var edr = CONSUMER.edrs().getEdrWithRefresh("test-id", true)
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

        @DisplayName("When multiple requests to refresh, to different edrs, verify all return non expired token")
        @Test
        void getEdrWithRefresh_subsequentRequestReturn() throws InterruptedException {

            try (var client = new MockServerClient("localhost", mockedRefreshApi.getPort())) {
                var claims = new JWTClaimsSet.Builder().claim("iss", "did:web:provider").build();
                var accessToken = createJwt(providerSigningKey, claims);
                var refreshToken = createJwt(providerSigningKey, new JWTClaimsSet.Builder().build());
                var tokenResponseBodyString = tokenResponseBody(accessToken, refreshToken);

                client.when(request().withMethod("POST").withPath("/refresh/token").withBody(exact("")))
                        .respond(response().withStatusCode(200).withDelay(Delay.milliseconds(5000)).withBody(tokenResponseBodyString));

                storeEdr("test-id-1", true);
                storeEdr("test-id-2", true);
                var numThreads = 50;
                var jitter = 20; // maximum time between threads are spawned
                var latch = new CountDownLatch(numThreads);

                var failed = new AtomicBoolean(false);

                IntStream.range(0, numThreads)
                        .parallel()
                        .forEach(i -> {
                            var wait = random.nextInt(1, jitter);
                            try {
                                Thread.sleep(wait);
                                new Thread(() -> {
                                    var edrNumber = random.nextInt(1, 3);
                                    try {
                                        var tr = CONSUMER.edrs().getEdrWithRefresh("test-id-%s".formatted(edrNumber), true)
                                                .assertThat()
                                                .log().ifValidationFails()
                                                .statusCode(anyOf(equalTo(200), equalTo(409)))
                                                .extract().asString();

                                        assertThat(tr).contains(accessToken);
                                    } catch (AssertionError e) {
                                        failed.set(true);
                                    } finally {
                                        latch.countDown();
                                    }


                                }).start();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        });

                latch.await();
                assertThat(failed.get()).isFalse();

                client.verify(request()
                        .withQueryStringParameter("grant_type", "refresh_token")
                        .withMethod("POST")
                        .withPath("/refresh/token"), VerificationTimes.exactly(2));

            }
        }

        @DisplayName("Verify the refresh endpoint is not called when token not yet expired")
        @Test
        void getEdrWithRefresh_notExpired_shouldNotCallEndpoint() {

            try (var client = new MockServerClient("localhost", mockedRefreshApi.getPort())) {
                // mock the provider dataplane's refresh endpoint

                storeEdr("test-id", false);
                var edr = CONSUMER.edrs().getEdrWithRefresh("test-id", true)
                        .statusCode(200)
                        .extract().body().as(JsonObject.class);
                assertThat(edr).isNotNull();

                // assert the correct endpoint was called
                client.verify(
                        request()
                                .withQueryStringParameter("grant_type", "refresh_token")
                                .withMethod("POST")
                                .withPath("/refresh/token"),
                        VerificationTimes.never());
            }
        }

        @DisplayName("Verify the refresh endpoint is not called when auto_refresh=false")
        @Test
        void getEdrWithRefresh_whenNotAutorefresh_shouldNotCallEndpoint() {

            try (var client = new MockServerClient("localhost", mockedRefreshApi.getPort())) {
                // mock the provider dataplane's refresh endpoint

                storeEdr("test-id", true);
                var edr = CONSUMER.edrs()
                        .getEdrWithRefresh("test-id", false)
                        .statusCode(200)
                        .extract().body().as(JsonObject.class);
                assertThat(edr).isNotNull();

                // assert the correct endpoint was called
                client.verify(
                        request()
                                .withQueryStringParameter("grant_type", "refresh_token")
                                .withMethod("POST")
                                .withPath("/refresh/token"),
                        VerificationTimes.never());
            }
        }

        @DisplayName("Verify HTTP 403 response when refreshing the token is not allowed")
        @Test
        void getEdrWithRefresh_unauthorized() {

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

                storeEdr("test-id", true);
                CONSUMER.edrs().getEdrWithRefresh("test-id", true)
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

        @Test
        void refreshEdr() {
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

                storeEdr("test-id", true);
                var edr = CONSUMER.edrs().refreshEdr("test-id")
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
        void refreshEdr_whenNotFound() {
            CONSUMER.edrs().refreshEdr("does-not-exist")
                    .statusCode(404);
        }

        @Test
        void refreshEdr_whenNotAuthorized() {
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

                storeEdr("test-id", true);
                CONSUMER.edrs().refreshEdr("test-id")
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

        protected abstract EndpointDataReferenceStore getStore();

        private String tokenResponseBody() {
            var claims = new JWTClaimsSet.Builder().claim("iss", "did:web:provider").build();
            var accessToken = createJwt(providerSigningKey, claims);
            var refreshToken = createJwt(providerSigningKey, new JWTClaimsSet.Builder().build());
            return tokenResponseBody(accessToken, refreshToken);
        }

        private String tokenResponseBody(String accessToken, String refreshToken) {
            var response = new TokenResponse(accessToken, refreshToken, 300L, "bearer");
            try {
                return mapper.writeValueAsString(response);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        private void storeEdr(String transferProcessId, boolean isExpired) {
            var claims = new JWTClaimsSet.Builder().claim("iss", "did:web:provider").build();
            var store = getStore();
            var edr = DataAddress.Builder.newInstance()
                    .type("test-type")
                    .property(EDC_NAMESPACE + "authorization", createJwt(providerSigningKey, claims))
                    .property(EDC_NAMESPACE + "authType", "bearer")
                    .property(EDR_PROPERTY_REFRESH_TOKEN, createJwt(providerSigningKey, new JWTClaimsSet.Builder().build()))
                    .property(EDR_PROPERTY_EXPIRES_IN, "300")
                    .property(EDR_PROPERTY_REFRESH_ENDPOINT, refreshEndpoint)
                    .property(EDR_PROPERTY_REFRESH_AUDIENCE, refreshAudience)
                    .build();
            var entry = EndpointDataReferenceEntry.Builder.newInstance()
                    .clock(isExpired ? // defaults to an expired token
                            Clock.fixed(Instant.now().minusSeconds(3600), ZoneId.systemDefault()) :
                            Clock.systemUTC())
                    .agreementId("test-agreement")
                    .assetId("test-asset")
                    .transferProcessId(transferProcessId)
                    .providerId("test-provider")
                    .contractNegotiationId("test-negotiation")
                    .build();
            store.save(entry, edr).orElseThrow(f -> new AssertionError(f.getFailureDetail()));
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

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        protected static final RuntimeExtension CONSUMER_RUNTIME = memoryRuntime(CONSUMER.getName(), CONSUMER.getId(), () ->
                CONSUMER.getConfig().merge(ConfigFactory.fromMap(Map.of("edc.iam.issuer.id", "did:web:consumer"))));

        @Override
        protected EndpointDataReferenceStore getStore() {
            return CONSUMER_RUNTIME.getService(EndpointDataReferenceStore.class);
        }
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @RegisterExtension
        @Order(0)
        private static final PostgresExtension POSTGRES = new PostgresExtension(CONSUMER.getName());

        @RegisterExtension
        protected static final RuntimeExtension CONSUMER_RUNTIME = pgRuntime(CONSUMER, POSTGRES);


        @Override
        protected EndpointDataReferenceStore getStore() {
            return CONSUMER_RUNTIME.getService(EndpointDataReferenceStore.class);
        }
    }
}
