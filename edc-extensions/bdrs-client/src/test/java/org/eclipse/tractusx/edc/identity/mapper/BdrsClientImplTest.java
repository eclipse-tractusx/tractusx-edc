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
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.Mockito.mock;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.verify.VerificationTimes.exactly;

class BdrsClientImplTest {

    private final Monitor monitor = mock();
    private final ObjectMapper mapper = new ObjectMapper();
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

        client = new BdrsClientImpl("http://localhost:%d/api".formatted(bdrsServer.getPort()), 1, new EdcHttpClientImpl(new OkHttpClient(), RetryPolicy.ofDefaults(), monitor), monitor, mapper);
    }

    @AfterEach
    void teardown() {
        bdrsServer.stop();
    }

    @Test
    void getData_whenCacheCold_shouldHitServer() {
        var did = client.resolve("bpn1");
        assertThat(did).isEqualTo("did:web:did1");

        bdrsServer.verify(request()
                        .withMethod("GET")
                        .withPath("/api/bpn-directory")
                        .withHeader("Accept-Encoding", "gzip"),
                exactly(1));
    }

    @Test
    void getData_whenCacheHot_shouldNotHitServer() {
        var did1 = client.resolve("bpn1");
        var did2 = client.resolve("bpn2");
        assertThat(did1).isEqualTo("did:web:did1");
        assertThat(did2).isEqualTo("did:web:did2");

        bdrsServer.verify(request()
                        .withMethod("GET")
                        .withPath("/api/bpn-directory")
                        .withHeader("Accept-Encoding", "gzip"),
                exactly(1));
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

                    bdrsServer.verify(request()
                                    .withHeader("Accept-Encoding", "gzip")
                                    .withMethod("GET")
                                    .withPath("/api/bpn-directory"),
                            exactly(2));
                });

    }

    @Test
    void getData_whenNotFound() {
        var did = client.resolve("bpn-notexist");
        assertThat(did).isNull();
        bdrsServer.verify(request()
                        .withMethod("GET")
                        .withPath("/api/bpn-directory")
                        .withHeader("Accept-Encoding", "gzip"),
                exactly(1));
    }

    @ParameterizedTest(name = "HTTP Status {0}")
    @ValueSource(ints = { 400, 401, 403, 404, 405 })
    void getData_bdrsReturnsError(int code) {
        bdrsServer.reset();
        bdrsServer.when(request().withPath("/api/bpn-directory").withMethod("GET"))
                .respond(HttpResponse.response().withStatusCode(code));
        assertThatThrownBy(() -> client.resolve("bpn1")).isInstanceOf(EdcException.class);
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