/********************************************************************************
 * Copyright (c) 2025 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.interceptor;

import jakarta.ws.rs.core.HttpHeaders;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.V_2025_1_PATH;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OkHttpInterceptorTest {

    private final Interceptor.Chain chain = mock();
    private final Response response = mock();

    private OkHttpInterceptor interceptor;
    private Request.Builder requestBuilder;

    @BeforeEach
    void setUp() throws IOException {
        interceptor = new OkHttpInterceptor();
        requestBuilder = new Request.Builder().url("http://example.com");
        when(chain.proceed(any())).thenReturn(response);
    }

    @Test
    void filter_whenNonBearerToken_shouldNotModifyRequest() throws IOException {
        var request = requestBuilder
                .url("http://example.com/protocol/test")
                .header(HttpHeaders.AUTHORIZATION, "Basic token123")
                .build();
        when(chain.request()).thenReturn(request);

        interceptor.intercept(chain);

        verify(chain).proceed(same(request));
    }

    @Test
    void filter_whenBearerToken_shouldStripBearerPrefix() throws IOException {
        var request = requestBuilder
                .url("http://example.com/protocol/test/catalog/request")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token123")
                .build();
        when(chain.request()).thenReturn(request);

        interceptor.intercept(chain);

        verify(chain).proceed(argThat(modifiedRequest ->
                "token123".equals(modifiedRequest.header(HttpHeaders.AUTHORIZATION))
        ));
    }

    @Test
    void filter_whenNoAuthHeader_shouldNotModifyRequest() throws IOException {
        var request = requestBuilder
                .url("http://example.com/protocol/test")
                .build();
        when(chain.request()).thenReturn(request);

        interceptor.intercept(chain);

        verify(chain).proceed(same(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/catalog/request",
            "/catalog/datasets",
            "/negotiations/",
            "/transfers/",
            ".well-known/dspace-version"
    })
    void filter_whenDspPath_shouldModifyRequest(String path) throws IOException {
        var request = requestBuilder
                .url("http://example.com/" + path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer token123")
                .build();
        when(chain.request()).thenReturn(request);

        interceptor.intercept(chain);

        verify(chain).proceed(argThat(modifiedRequest ->
                "token123".equals(modifiedRequest.header(HttpHeaders.AUTHORIZATION))
        ));
    }

    @Test
    void filter_when2025VersionPath_shouldNotModifyRequest() throws IOException {
        var request = requestBuilder
                .url("http://example.com/protocol/catalog/request" + V_2025_1_PATH + "/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token123")
                .build();
        when(chain.request()).thenReturn(request);

        interceptor.intercept(chain);

        verify(chain).proceed(same(request));
    }

    @Test
    void filter_whenDspace08Schema_shouldModifyRequest() throws IOException {
        var request = requestBuilder
                .url("http://example.com/protocol/catalog/request")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token123")
                .post(okhttp3.RequestBody.create(
                        """
                                {
                                  "@context": {
                                    "dspace": "https://w3id.org/dspace/v0.8/"
                                  },
                                  "@type": "dspace:SomeType"
                                }
                                """,
                        okhttp3.MediaType.parse("application/json")))
                .build();
        when(chain.request()).thenReturn(request);

        interceptor.intercept(chain);

        verify(chain).proceed(argThat(modifiedRequest ->
                "token123".equals(modifiedRequest.header(HttpHeaders.AUTHORIZATION))
        ));
    }
}
