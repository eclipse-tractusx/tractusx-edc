/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.tests.catalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.lifecycle.ParticipantRuntime;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static java.lang.String.format;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.MIW_PORT;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.platoSsiConfiguration;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.sokratesSsiConfiguration;

@EndToEndTest
public class SsiCatalogInMemoryTest extends AbstractCatalogTest {

    @RegisterExtension
    protected static final ParticipantRuntime SOKRATES_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:runtime-memory-ssi",
            SOKRATES_NAME,
            SOKRATES_BPN,
            sokratesSsiConfiguration()
    );
    @RegisterExtension
    protected static final ParticipantRuntime PLATO_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:runtime-memory-ssi",
            PLATO_NAME,
            PLATO_BPN,
            platoSsiConfiguration()
    );
    MockWebServer server = new MockWebServer();

    @BeforeEach
    void setup() throws IOException {
        server.start(MIW_PORT);
        server.setDispatcher(new MiwDispatcher(PLATO_BPN));
    }

    @AfterEach
    void teardown() throws IOException {
        server.shutdown();
    }

    private static final class MiwDispatcher extends Dispatcher {

        private static final TypeManager MAPPER = new TypeManager();

        private static final String SUMMARY_JSON;

        static {

            var classloader = Thread.currentThread().getContextClassLoader();

            try (var jsonStream = classloader.getResourceAsStream("summary-vc.json")) {
                Objects.requireNonNull(jsonStream);
                SUMMARY_JSON = new String(jsonStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private final String bpn;

        private final Map<String, Object> summaryVc;

        private MiwDispatcher(String bpn) {
            this.bpn = bpn;
            var json = format(SUMMARY_JSON, bpn);
            summaryVc = MAPPER.readValue(json, new TypeReference<>() {
            });
        }

        @NotNull
        @Override
        public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
            return switch (recordedRequest.getPath().split("\\?")[0]) {
                case "/api/credentials" -> credentialResponse();
                case "/api/presentations" -> presentationResponse();
                default -> new MockResponse().setResponseCode(404);
            };
        }

        private MockResponse credentialResponse() {
            return new MockResponse().setBody(MAPPER.writeValueAsString(List.of(summaryVc)));
        }

        private MockResponse presentationResponse() {
            try {
                var jwt = createJwt(UUID.randomUUID().toString(), createClaims(Instant.now(), Map.of("verifiableCredential", List.of(summaryVc))), testKey().toPrivateKey());
                return new MockResponse().setBody(MAPPER.writeValueAsString(Map.of("vp", jwt)));
            } catch (JOSEException e) {
                throw new RuntimeException(e);
            }
        }

        private JWTClaimsSet createClaims(Instant exp, Map<String, Object> presentation) {
            return new JWTClaimsSet.Builder()
                    .claim("vp", presentation)
                    .expirationTime(Date.from(exp))
                    .build();
        }

        private String createJwt(String publicKeyId, JWTClaimsSet claimsSet, PrivateKey pk) {
            var header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(publicKeyId).build();
            try {
                SignedJWT jwt = new SignedJWT(header, claimsSet);
                jwt.sign(new RSASSASigner(pk));
                return jwt.serialize();
            } catch (JOSEException e) {
                throw new AssertionError(e);
            }
        }

        private RSAKey testKey() throws JOSEException {
            return new RSAKeyGenerator(2048)
                    .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                    .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                    .generate();
        }
    }
}
