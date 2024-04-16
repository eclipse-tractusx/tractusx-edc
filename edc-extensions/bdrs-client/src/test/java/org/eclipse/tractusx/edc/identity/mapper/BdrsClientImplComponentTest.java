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
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.iam.identitytrust.spi.CredentialServiceClient;
import org.eclipse.edc.iam.identitytrust.sts.embedded.EmbeddedSecureTokenService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentation;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.verifiablecredentials.jwt.JwtCreationUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.eclipse.tractusx.edc.identity.mapper.TestData.VP_CONTENT_EXAMPLE;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This test creates a {@link BdrsClientImpl} with all its collaborators (using an embedded STS), and spins up a
 * BDRS Server in a test container.
 * In addition, this test generates DID documents for both the dataspace issuer and the VP holder, and launches an NGINX webserver
 * which then hosts these DID documents, so that BDRS may resolve them.
 */
@Testcontainers
@ComponentTest
class BdrsClientImplComponentTest {
    public static final String NGINX_CONTAINER_NAME = "nginx";
    public static final String BDRS_CONTAINER_NAME = "bdrs";
    private static final Network DOCKER_NETWORK = Network.newNetwork();
    @Container
    private static final GenericContainer<?> BDRS_SERVER_CONTAINER = new GenericContainer<>("tractusx/bdrs-server-memory")
            .withEnv("EDC_API_AUTH_KEY", "password")
            .withEnv("WEB_HTTP_MANAGEMENT_PATH", "/api/management")
            .withEnv("WEB_HTTP_MANAGEMENT_PORT", "8081")
            .withEnv("WEB_HTTP_PATH", "/api")
            .withEnv("WEB_HTTP_PORT", "8080")
            .withEnv("WEB_HTTP_DIRECTORY_PATH", "/api/directory")
            .withEnv("WEB_HTTP_DIRECTORY_PORT", "8082")
            .withEnv("EDC_IAM_DID_WEB_USE_HTTPS", "false")
            .withEnv("EDC_IAM_TRUSTED-ISSUER_ISSUER_ID", "did:web:" + NGINX_CONTAINER_NAME + ":some-issuer")
            .withNetwork(DOCKER_NETWORK)
            .withCreateContainerCmdModifier(cmd -> cmd.withName(BDRS_CONTAINER_NAME))
            .withExposedPorts(8080, 8081, 8082);
    private static final File SHARED_TEMP_DIR = new File("build/dids");
    @Container
    private static final GenericContainer<?> NGINX_CONTAINER = new GenericContainer<>("nginx")
            .withFileSystemBind(new File("src/test/resources/nginx.conf").getAbsolutePath(), "/etc/nginx/nginx.conf", BindMode.READ_ONLY)
            .withFileSystemBind(SHARED_TEMP_DIR.getAbsolutePath(), "/var/www", BindMode.READ_ONLY)
            .withNetwork(DOCKER_NETWORK)
            .withCreateContainerCmdModifier(cmd -> cmd.withName(NGINX_CONTAINER_NAME))
            .withExposedPorts(80);
    private final Monitor monitor = mock();
    private final ObjectMapper mapper = new ObjectMapper();
    private final CredentialServiceClient csMock = mock();
    private String issuerId;
    private String holderId;
    private BdrsClientImpl client;
    private ECKey vpHolderKey;
    private ECKey vcIssuerKey;

    @AfterAll
    static void cleanup() {
        if (SHARED_TEMP_DIR.exists()) {
            SHARED_TEMP_DIR.delete();
        }
    }

    @BeforeEach
    void setup() throws JOSEException {

        var nginxHostname = NGINX_CONTAINER.getContainerName().replace("/", "");

        // need to generate DID such that they point to the nginx container
        issuerId = "did:web:%s:some-issuer".formatted(nginxHostname);
        holderId = "did:web:%s:bdrs-client".formatted(nginxHostname);

        vcIssuerKey = new ECKeyGenerator(Curve.P_256).keyID(issuerId + "#key-1").generate();
        vpHolderKey = new ECKeyGenerator(Curve.P_256).keyID(holderId + "#key-1").generate();

        var pk = vpHolderKey.toPrivateKey();
        var sts = new EmbeddedSecureTokenService(new JwtGenerationService(), () -> pk, () -> vpHolderKey.getKeyID(), Clock.systemUTC(), 10);

        var directoryPort = BDRS_SERVER_CONTAINER.getMappedPort(8082);
        client = new BdrsClientImpl("http://%s:%d/api/directory".formatted(BDRS_SERVER_CONTAINER.getHost(), directoryPort), 1,
                "did:web:self",
                () -> "http://credential.service",
                new EdcHttpClientImpl(new OkHttpClient(), RetryPolicy.ofDefaults(), monitor),
                monitor,
                mapper,
                sts,
                csMock);

        // prepare a mock server hosting the VC issuer's DID and the VP holders DID
        createDidDocument(vcIssuerKey, issuerId);
        createDidDocument(vpHolderKey, holderId);

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
                .thenReturn(Result.success(List.of(new VerifiablePresentationContainer(token, CredentialFormat.JWT, VerifiablePresentation.Builder.newInstance().type("VerifiableCredential").build()))));

        assertThatThrownBy(() -> client.resolve("BPN1")).isInstanceOf(EdcException.class)
                .hasMessageContaining("code: 401, message: Unauthorized");
    }

    @Test
    void resolve_withValidCredential() {

        // create VC-JWT (signed by the central issuer)
        var vcJwt1 = JwtCreationUtils.createJwt(vcIssuerKey, issuerId, "degreeSub", holderId, Map.of("vc", asMap(TestData.MEMBERSHIP_CREDENTIAL.formatted(holderId))));

        // create VP-JWT (signed by the presenter) that contains the VP as a claim
        var vpJwt = JwtCreationUtils.createJwt(vpHolderKey, holderId, null, "bdrs-server-audience", Map.of("vp", asMap(VP_CONTENT_EXAMPLE.formatted(holderId, "\"" + vcJwt1 + "\""))));

        when(csMock.requestPresentation(anyString(), anyString(), anyList()))
                .thenReturn(Result.success(List.of(new VerifiablePresentationContainer(vpJwt, CredentialFormat.JWT, null))));

        assertThatNoException().describedAs(BDRS_SERVER_CONTAINER::getLogs)
                .isThrownBy(() -> client.resolve("BPN1"));
    }

    @AfterEach
    void teardown() {
    }

    private String createDidDocument(ECKey vpHolderKey, String holderId) {
        var didDocument = DidDocument.Builder.newInstance()
                .id("http://tx-test.com/" + UUID.randomUUID())
                .verificationMethod(List.of(VerificationMethod.Builder.newInstance()
                        .id(vpHolderKey.getKeyID())
                        .publicKeyJwk(vpHolderKey.toPublicJWK().toJSONObject())
                        .controller(holderId)
                        .type("JsonWebKey2020")
                        .build()))
                .build();
        holderId = holderId.replace("did:web:", "");
        var subdir = holderId.substring(holderId.indexOf(":") + 1) + "/";
        var tmpDir = new File(SHARED_TEMP_DIR.getAbsoluteFile(), subdir);
        tmpDir.setReadable(true);
        tmpDir.setWritable(true);
        tmpDir.mkdirs();
        if (!tmpDir.exists()) {
            fail("Directory does not exist at: " + tmpDir.getAbsolutePath());
        }
        var file = new File(tmpDir, "did.json");
        try (var fos = new FileOutputStream(file)) {
            mapper.writeValue(fos, didDocument);
            return tmpDir.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] createGzipStream() {
        var data = Map.of("bpn1", "did:web:did1",
                "bpn2", "did:web:did2",
                "bpn3", "did:web:did3");

        var bas = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(bas)) {
            gzip.write(mapper.writeValueAsBytes(data));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bas.toByteArray();
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