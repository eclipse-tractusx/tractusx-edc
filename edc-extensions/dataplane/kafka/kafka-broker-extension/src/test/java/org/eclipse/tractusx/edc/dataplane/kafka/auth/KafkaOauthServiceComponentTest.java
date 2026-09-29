/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.dataplane.kafka.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaOauthServiceComponentTest {

    private KafkaOauthServiceImpl oauthService;
    private EdcHttpClient mockHttpClient;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(EdcHttpClient.class);
        oauthService = new KafkaOauthServiceImpl(mockHttpClient, new ObjectMapper());
    }

    @Test
    void shouldSuccessfullyRetrieveAccessToken() throws IOException {
        OauthCredentials credentials = new OauthCredentials(
                "https://example.com/token",
                Optional.empty(),
                "test-client",
                "test-secret"
        );

        Response mockResponse = mock(Response.class);
        ResponseBody mockResponseBody = mock(ResponseBody.class);

        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.code()).thenReturn(200);
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.string()).thenReturn("{\"access_token\":\"test-access-token\",\"token_type\":\"Bearer\"}");

        when(mockHttpClient.execute(any(Request.class)))
                .thenAnswer(invocation -> {
                    Request request = invocation.getArgument(0);

                    assertThat(request.method()).isEqualTo("POST");
                    assertThat(request.url().encodedPath()).isEqualTo("/token");
                    assertThat(request.header("Content-Type")).isEqualTo("application/x-www-form-urlencoded");

                    return mockResponse;
                });

        String accessToken = oauthService.getAccessToken(credentials);

        assertThat(accessToken).isEqualTo("test-access-token");
        verify(mockHttpClient, times(1)).execute(any(Request.class));
    }

    @Test
    void shouldThrow_WhenResponseHasNoAccessToken() throws IOException {
        OauthCredentials credentials = new OauthCredentials(
                "https://example.com/token",
                Optional.empty(),
                "test-client",
                "test-secret"
        );

        Response mockResponse = mock(Response.class);
        ResponseBody mockResponseBody = mock(ResponseBody.class);

        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.code()).thenReturn(200);
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.string()).thenReturn("{\"token_type\":\"Bearer\"}");

        when(mockHttpClient.execute(any(Request.class))).thenReturn(mockResponse);

        assertThatThrownBy(() -> oauthService.getAccessToken(credentials))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("access_token");
    }

    @Test
    void shouldHandleTokenRetrievalFailure() throws IOException {
        OauthCredentials credentials = new OauthCredentials(
                "https://example.com/token",
                Optional.empty(),
                "test-client",
                "test-secret"
        );

        Response mockResponse = mock(Response.class);

        when(mockResponse.isSuccessful()).thenReturn(false);
        when(mockResponse.code()).thenReturn(401);

        when(mockHttpClient.execute(any(Request.class)))
                .thenAnswer(invocation -> {
                    Request request = invocation.getArgument(0);

                    assertThat(request.method()).isEqualTo("POST");
                    assertThat(request.url().encodedPath()).isEqualTo("/token");
                    assertThat(request.header("Content-Type")).isEqualTo("application/x-www-form-urlencoded");

                    return mockResponse;
                });

        assertThatThrownBy(() -> oauthService.getAccessToken(credentials))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Oauth2 token endpoint returned HTTP 401");
        verify(mockHttpClient, times(1)).execute(any(Request.class));
    }
}
