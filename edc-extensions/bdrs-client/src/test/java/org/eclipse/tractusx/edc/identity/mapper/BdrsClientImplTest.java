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

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.iam.identitytrust.spi.CredentialServiceClient;
import org.eclipse.edc.iam.identitytrust.spi.SecureTokenService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentation;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.verify.VerificationTimes.exactly;
import static org.mockserver.verify.VerificationTimes.never;

class BdrsClientImplTest {

    public static final String TEST_VP_CONTENT = "test-raw-vp";
    private final Monitor monitor = mock();
    private final ObjectMapper mapper = new ObjectMapper();
    private final SecureTokenService stsMock = mock();
    private final CredentialServiceClient csMock = mock();
    private BdrsClientImpl client;
    private ClientAndServer bdrsServer;

    @BeforeEach
    void setup() {
        bdrsServer = ClientAndServer.startClientAndServer(getFreePort());
        bdrsServer.when(request()
                        .withMethod("GET")
                        .withPath("/api/bpn-directory"))
                .respond(HttpResponse.response()
                        .withHeader("Content-Encoding", "gzip")
                        .withBody(createGzipStream())
                        .withStatusCode(200));

        client = new BdrsClientImpl("http://localhost:%d/api".formatted(bdrsServer.getPort()), 1,
                "did:web:self",
                () -> "http://credential.service",
                new EdcHttpClientImpl(new OkHttpClient(), RetryPolicy.ofDefaults(), monitor),
                monitor,
                mapper,
                stsMock,
                csMock);

        // prime STS and CS
        when(stsMock.createToken(anyMap(), notNull())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("my-fancy-sitoken").build()));
        when(csMock.requestPresentation(anyString(), anyString(), anyList()))
                .thenReturn(Result.success(List.of(new VerifiablePresentationContainer(TEST_VP_CONTENT, CredentialFormat.JWT, VerifiablePresentation.Builder.newInstance().type("VerifiableCredential").build()))));

    }

    @AfterEach
    void teardown() {
        bdrsServer.stop();
    }

    @Test
    void getData_whenCacheCold_shouldHitServer() {
        var did = client.resolve("bpn1");
        assertThat(did).isEqualTo("did:web:did1");

        verifyBdrsRequest(1);
    }

    @Test
    void getData_whenCacheHot_shouldNotHitServer() {
        var did1 = client.resolve("bpn1");
        var did2 = client.resolve("bpn2");
        assertThat(did1).isEqualTo("did:web:did1");
        assertThat(did2).isEqualTo("did:web:did2");

        verifyBdrsRequest(1);
    }

    @Test
    void getData_whenCacheExpired_shouldHitServer() {
        var did1 = client.resolve("bpn1"); // hits server
        assertThat(did1).isEqualTo("did:web:did1");

        await().pollDelay(ofSeconds(2))
                .atMost(ofSeconds(3)) //cache expires
                .untilAsserted(() -> {
                    var did2 = client.resolve("bpn2"); // hits server as well, b/c cache is expired
                    assertThat(did2).isEqualTo("did:web:did2");

                    verifyBdrsRequest(2);
                });

    }

    @Test
    void getData_whenNotFound() {
        var did = client.resolve("bpn-notexist");
        assertThat(did).isNull();
        verifyBdrsRequest(1);
    }

    @ParameterizedTest(name = "HTTP Status {0}")
    @ValueSource(ints = { 400, 401, 403, 404, 405 })
    void getData_bdrsReturnsError(int code) {
        bdrsServer.reset();
        bdrsServer.when(request().withPath("/api/bpn-directory").withMethod("GET"))
                .respond(HttpResponse.response().withStatusCode(code));
        assertThatThrownBy(() -> client.resolve("bpn1")).isInstanceOf(EdcException.class);
    }

    @Test
    void getData_whenStsFails() {
        when(stsMock.createToken(anyMap(), notNull())).thenReturn(Result.failure("test-failure"));
        assertThatThrownBy(() -> client.resolve("bpn1"))
                .isInstanceOf(EdcException.class)
                .hasMessage("test-failure");
        bdrsServer.verify(request(), never());
    }

    @Test
    void getData_whenPresentationQueryFails() {
        when(csMock.requestPresentation(anyString(), anyString(), anyList())).thenReturn(Result.failure("test-failure"));

        assertThatThrownBy(() -> client.resolve("bpn1"))
                .isInstanceOf(EdcException.class)
                .hasMessage("test-failure");
        bdrsServer.verify(request(), never());
    }

    @Test
    void getData_whenPresentationQueryReturnsTooManyVps() {
        var presentations = List.of(
                new VerifiablePresentationContainer(TEST_VP_CONTENT, CredentialFormat.JWT, VerifiablePresentation.Builder.newInstance().type("VerifiableCredential").build()),
                new VerifiablePresentationContainer("test-raw-vp-2", CredentialFormat.JWT, VerifiablePresentation.Builder.newInstance().type("VerifiableCredential").build()));

        when(csMock.requestPresentation(anyString(), anyString(), anyList())).thenReturn(Result.success(presentations));

        assertThatNoException().isThrownBy(() -> client.resolve("bpn1"));
        verifyBdrsRequest(1);
        verify(monitor).warning("Expected exactly 1 VP, but found 2.");
    }

    @Test
    void getData_whenPresentationQueryReturnsEmpty() {

        when(csMock.requestPresentation(anyString(), anyString(), anyList())).thenReturn(Result.success(Collections.emptyList()));

        assertThatThrownBy(() -> client.resolve("bpn1"))
                .isInstanceOf(EdcException.class)
                .hasMessage("Expected exactly 1 VP, but was empty");
        bdrsServer.verify(request(), never());
    }

    private void verifyBdrsRequest(int count) {
        bdrsServer.verify(request()
                        .withMethod("GET")
                        .withPath("/api/bpn-directory")
                        .withHeader("Authorization", "Bearer " + TEST_VP_CONTENT)
                        .withHeader("Accept-Encoding", "gzip"),
                exactly(count));
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

}