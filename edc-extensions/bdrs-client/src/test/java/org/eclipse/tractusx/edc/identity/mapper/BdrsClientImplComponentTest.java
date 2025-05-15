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

package org.eclipse.tractusx.edc.identity.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.iam.identitytrust.spi.CredentialServiceClient;
import org.eclipse.edc.iam.identitytrust.spi.SecureTokenService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentation;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.verifiablecredentials.jwt.JwtCreationUtils.createJwt;
import static org.eclipse.tractusx.edc.identity.mapper.TestData.VP_CONTENT_EXAMPLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This test creates a {@link BdrsClientImpl} with all its collaborators (using an embedded STS), and spins up a
 * BDRS Server in a test container. In addition, DID documents are hosted by an NGINX server running in another container
 * from where BDRS resolves them.
 */
@Testcontainers
@ComponentTest
class BdrsClientImplComponentTest {
    private static final String NGINX_CONTAINER_NAME = "nginx";
    private static final String BDRS_CONTAINER_NAME = "bdrs";
    private static final String ISSUER_NAME = "some-issuer";
    private static final String HOLDER_NAME = "bdrs-client";
    private static final String ISSUER_DID = "did:web:%s:%s".formatted(NGINX_CONTAINER_NAME, ISSUER_NAME);
    private static final String HOLDER_DID = "did:web:%s:%s".formatted(NGINX_CONTAINER_NAME, HOLDER_NAME);
    private static final Network DOCKER_NETWORK = Network.newNetwork();

    @Container
    private static final GenericContainer<?> BDRS_SERVER_CONTAINER = new GenericContainer<>("tractusx/bdrs-server-memory:0.5.4")
            .withEnv("EDC_HTTP_MANAGEMENT_AUTH_KEY", "password")
            .withEnv("WEB_HTTP_MANAGEMENT_PATH", "/api/management")
            .withEnv("WEB_HTTP_MANAGEMENT_PORT", "8081")
            .withEnv("WEB_HTTP_PATH", "/api")
            .withEnv("WEB_HTTP_PORT", "8080")
            .withEnv("WEB_HTTP_DIRECTORY_PATH", "/api/directory")
            .withEnv("WEB_HTTP_DIRECTORY_PORT", "8082")
            .withEnv("EDC_IAM_ISSUER_ID", "any")
            .withEnv("EDC_IAM_DID_WEB_USE_HTTPS", "false")
            .withEnv("EDC_IAM_TRUSTED-ISSUER_ISSUER_ID", "did:web:%s:%s".formatted(NGINX_CONTAINER_NAME, ISSUER_NAME))
            .withNetwork(DOCKER_NETWORK)
            .withCreateContainerCmdModifier(cmd -> cmd.withName(BDRS_CONTAINER_NAME))
            .withExposedPorts(8080, 8081, 8082);
    private static final String SHARED_TEMP_DIR = new File("src/test/resources/dids").getAbsolutePath();
    @Container
    private static final GenericContainer<?> NGINX_CONTAINER = new GenericContainer<>("nginx")
            .withFileSystemBind(new File("src/test/resources/nginx.conf").getAbsolutePath(), "/etc/nginx/nginx.conf", BindMode.READ_ONLY)
            .withFileSystemBind(SHARED_TEMP_DIR, "/var/www", BindMode.READ_ONLY)
            .withNetwork(DOCKER_NETWORK)
            .withCreateContainerCmdModifier(cmd -> cmd.withName(NGINX_CONTAINER_NAME))
            .withExposedPorts(80);

    private final Monitor monitor = mock();
    private final ObjectMapper mapper = new ObjectMapper();
    private final CredentialServiceClient csMock = mock();

    private BdrsClientImpl client;
    private ECKey vpHolderKey;
    private ECKey vcIssuerKey;

    @BeforeEach
    void setup() throws IOException, ParseException {

        vcIssuerKey = ECKey.parse(Files.readString(Path.of(SHARED_TEMP_DIR, ISSUER_NAME + "/key.json")));
        vpHolderKey = ECKey.parse(Files.readString(Path.of(SHARED_TEMP_DIR, HOLDER_NAME + "/key.json")));

        SecureTokenService secureTokenService = mock();
        when(secureTokenService.createToken(any(), any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token").build()));
        var directoryPort = BDRS_SERVER_CONTAINER.getMappedPort(8082);
        client = new BdrsClientImpl("http://%s:%d/api/directory".formatted(BDRS_SERVER_CONTAINER.getHost(), directoryPort), 1,
                "did:web:self",
                () -> "http://credential.service",
                new EdcHttpClientImpl(new OkHttpClient(), RetryPolicy.ofDefaults(), monitor),
                monitor,
                mapper,
                secureTokenService,
                csMock);

        // need to wait until healthy, otherwise BDRS will respond with a 404
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(BDRS_SERVER_CONTAINER.isHealthy()).isTrue();
            assertThat(NGINX_CONTAINER.isRunning()).isTrue();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "not_a_jwt", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c" })
    void resolve_withInvalidCredential(String token) {
        // prime STS and CS
        when(csMock.requestPresentation(anyString(), anyString(), anyList()))
                .thenReturn(Result.success(List.of(new VerifiablePresentationContainer(token, CredentialFormat.VC1_0_JWT, VerifiablePresentation.Builder.newInstance().type("VerifiableCredential").build()))));

        assertThatThrownBy(() -> client.resolve("BPN1")).isInstanceOf(EdcException.class)
                .hasMessageContaining("code: 401, message: Unauthorized");
    }

    @Test
    void resolve_withSpoofedCredential() throws JOSEException {
        var spoofedKey = new ECKeyGenerator(Curve.P_256).keyID("did:web:nginx:%s#key-1".formatted(ISSUER_NAME)).generate();

        // create VC-JWT (signed by the central issuer)
        var membershipCredential = createJwt(spoofedKey, ISSUER_DID, "membership", HOLDER_DID, Map.of("vc", asMap(TestData.MEMBERSHIP_CREDENTIAL.formatted(HOLDER_DID))));

        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var presentation = createJwt(vpHolderKey, HOLDER_DID, null, "bdrs-server-audience", Map.of("vp", asMap(VP_CONTENT_EXAMPLE.formatted(HOLDER_DID, "\"" + membershipCredential + "\""))));

        when(csMock.requestPresentation(anyString(), anyString(), anyList()))
                .thenReturn(Result.success(List.of(new VerifiablePresentationContainer(presentation, CredentialFormat.VC1_0_JWT, null))));

        assertThatThrownBy(() -> client.resolve("BPN1"))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("code: 401, message: Unauthorized");
    }

    @Test
    void resolve_withSpoofedPresentation() throws JOSEException {
        var spoofedKey = new ECKeyGenerator(Curve.P_256).keyID("did:web:nginx:%s#key-1".formatted(ISSUER_NAME)).generate();

        // create VC-JWT (signed by the central issuer)
        var membershipCredential = createJwt(vcIssuerKey, ISSUER_DID, "membership", HOLDER_DID, Map.of("vc", asMap(TestData.MEMBERSHIP_CREDENTIAL.formatted(HOLDER_DID))));

        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var presentation = createJwt(spoofedKey, HOLDER_DID, null, "bdrs-server-audience", Map.of("vp", asMap(VP_CONTENT_EXAMPLE.formatted(HOLDER_DID, "\"" + membershipCredential + "\""))));

        when(csMock.requestPresentation(anyString(), anyString(), anyList()))
                .thenReturn(Result.success(List.of(new VerifiablePresentationContainer(presentation, CredentialFormat.VC1_0_JWT, null))));

        assertThatThrownBy(() -> client.resolve("BPN1"))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("code: 401, message: Unauthorized");
    }

    @Test
    void resolve_withValidCredential() {

        // create VC-JWT (signed by the central issuer)
        var membershipCredential = createJwt(vcIssuerKey, ISSUER_DID, "membership", HOLDER_DID, Map.of("vc", asMap(TestData.MEMBERSHIP_CREDENTIAL.formatted(HOLDER_DID))));

        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var presentation = createJwt(vpHolderKey, HOLDER_DID, null, "bdrs-server-audience", Map.of("vp", asMap(VP_CONTENT_EXAMPLE.formatted(HOLDER_DID, "\"" + membershipCredential + "\""))));

        when(csMock.requestPresentation(anyString(), anyString(), anyList()))
                .thenReturn(Result.success(List.of(new VerifiablePresentationContainer(presentation, CredentialFormat.VC1_0_JWT, null))));

        assertThatNoException().describedAs(BDRS_SERVER_CONTAINER::getLogs)
                .isThrownBy(() -> client.resolve("BPN1"));
    }

    @Test
    void resolve_withExpiredMembership() {

        // create VC-JWT (signed by the central issuer)
        var membershipCredential = createExpiredJwt(vcIssuerKey, ISSUER_DID, "membership", HOLDER_DID, Map.of("vc", asMap(TestData.MEMBERSHIP_CREDENTIAL_EXPIRED.formatted(HOLDER_DID))));

        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var presentation = createJwt(vpHolderKey, HOLDER_DID, null, "bdrs-server-audience", Map.of("vp", asMap(VP_CONTENT_EXAMPLE.formatted(HOLDER_DID, "\"" + membershipCredential + "\""))));

        when(csMock.requestPresentation(anyString(), anyString(), anyList()))
                .thenReturn(Result.success(List.of(new VerifiablePresentationContainer(presentation, CredentialFormat.VC1_0_JWT, null))));

        assertThatThrownBy(() -> client.resolve("BPN1"))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("code: 401, message: Unauthorized");
    }

    private String createExpiredJwt(ECKey privateKey, String issuerId, String subject, String audience, Map<String, Map<String, Object>> claims) {
        try {
            var signer = new ECDSASigner(privateKey.toECPrivateKey());

            // Prepare JWT with claims set
            var now = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
            var claimsSet = new JWTClaimsSet.Builder()
                    .issuer(issuerId)
                    .subject(subject)
                    .issueTime(now)
                    .audience(audience)
                    .notBeforeTime(now)
                    .claim("jti", UUID.randomUUID().toString())
                    .expirationTime(Date.from(Instant.now().plusSeconds(60)));

            claims.forEach(claimsSet::claim);

            var signedJwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(privateKey.getKeyID()).build(), claimsSet.build());

            signedJwt.sign(signer);

            return signedJwt.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> asMap(String rawContent) {
        try {
            return mapper.readValue(rawContent, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
