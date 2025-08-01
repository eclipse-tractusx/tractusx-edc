/********************************************************************************
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.dataplane.http.pipeline;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.connector.dataplane.http.params.HttpRequestFactory;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static okhttp3.Protocol.HTTP_1_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.http.client.testfixtures.HttpTestUtils.testHttpClient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProxyHttpDataSourceTest {

    private final HttpRequestFactory requestFactory = mock();

    @Test
    void verifyCallSuccess() {
        var responseBody = ResponseBody.create("{}", MediaType.parse("application/json"));

        var interceptor = new CustomInterceptor(200, responseBody, "Test message");
        var params = mock(HttpRequestParams.class);
        var request = dummyRequest();
        var source = defaultBuilder(interceptor).params(params).requestFactory(requestFactory).build();
        when(requestFactory.toRequest(any())).thenReturn(request);

        var parts = source.openPartStream().getContent().toList();

        var interceptedRequest = interceptor.getInterceptedRequest();
        assertThat(interceptedRequest).isEqualTo(request);
        assertThat(parts).hasSize(1).first().satisfies(part -> {
            assertThat(part.mediaType()).startsWith("application/json");
            assertThat(part.openStream()).hasContent("{}");
        });

        verify(requestFactory).toRequest(any());
    }

    @Test
    void close_shouldCloseResponseBodyAndStream() throws IOException {
        InputStream stream = mock();
        var responseBody = spy(ResponseBody.create("{}", MediaType.parse("application/json")));
        when(responseBody.byteStream()).thenReturn(stream);
        var interceptor = new CustomInterceptor(200, responseBody, "Test message");
        var source = defaultBuilder(interceptor).params(mock()).requestFactory(requestFactory).build();
        when(requestFactory.toRequest(any())).thenReturn(dummyRequest());

        source.openPartStream();
        source.close();

        verify(responseBody).close();
        verify(stream).close();
    }

    @NotNull
    private Request dummyRequest() {
        return new Request.Builder().url("http://some.test.url/").get().build();
    }

    private ProxyHttpDataSource.Builder defaultBuilder(Interceptor interceptor) {
        var httpClient = testHttpClient(interceptor);
        return ProxyHttpDataSource.Builder.newInstance()
                .httpClient(httpClient)
                .name("test-name")
                .monitor(mock(Monitor.class))
                .requestId(UUID.randomUUID().toString());
    }

    static final class CustomInterceptor implements Interceptor {
        private final List<Request> requests = new ArrayList<>();
        private final int statusCode;
        private final ResponseBody responseBody;
        private final String message;

        CustomInterceptor(int statusCode, ResponseBody responseBody, String message) {
            this.statusCode = statusCode;
            this.responseBody = responseBody;
            this.message = message;
        }

        @NotNull
        @Override
        public Response intercept(@NotNull Interceptor.Chain chain) {
            requests.add(chain.request());
            return new Response.Builder()
                    .request(chain.request())
                    .protocol(HTTP_1_1)
                    .code(statusCode)
                    .body(responseBody)
                    .message(message)
                    .build();
        }

        public Request getInterceptedRequest() {
            return requests.stream()
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No request intercepted"));
        }
    }
}
