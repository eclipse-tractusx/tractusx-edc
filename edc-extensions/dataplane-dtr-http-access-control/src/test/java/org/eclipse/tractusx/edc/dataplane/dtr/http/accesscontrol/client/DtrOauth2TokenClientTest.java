/********************************************************************************
 * Copyright (c) 2024 Robert Bosch Manufacturing Solutions GmbH and others
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.client;

import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.AccessControlServerException;
import org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.HttpAccessControlCheckDtrClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.client.DtrOauth2TokenClient.ACCESS_TOKEN;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.client.DtrOauth2TokenClient.MAP_TYPE_REFERENCE;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class DtrOauth2TokenClientTest {
    static final String POST = "POST";
    static final String SCOPE = "aud:dtr";
    static final String ENCODED_SCOPE = URLEncoder.encode(SCOPE, StandardCharsets.UTF_8);
    static final String CLIENT_ID = "client:id";
    static final String ENCODED_CLIENT_ID = URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8);
    static final String CLIENT_SECRET_VAULT_PATH = "client_secret";
    static final String CLIENT_SECRET = "client:secret";
    static final String ENCODED_CLIENT_SECRET = URLEncoder.encode(CLIENT_SECRET, StandardCharsets.UTF_8);
    static final String DUMMY_TOKEN = "dummy_token_value";
    static final String RESPONSE_BODY = "{\"" + ACCESS_TOKEN + "\": \"" + DUMMY_TOKEN + "\"}";
    static final String LOCALHOST_TOKEN_ENDPOINT = "https://localhost/token";
    @Mock
    private Response httpResponse;
    @Mock
    private ResponseBody responseBody;
    @Mock
    private Monitor monitor;
    @Mock
    private EdcHttpClient httpClient;
    @Mock
    private Vault vault;
    @Mock
    private TypeManager typeManager;
    @Mock
    private HttpAccessControlCheckDtrClientConfig dtrConfig;
    @InjectMocks
    private DtrOauth2TokenClient underTest;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @SuppressWarnings({"resource", "ResultOfMethodCallIgnored"})
    @Test
    void test_GetBearerToken_ShouldReturnToken_WhenOauth2ResponseContainsOne() throws IOException {
        //given
        when(dtrConfig.getOauth2ClientId()).thenReturn(CLIENT_ID);
        when(dtrConfig.getOauth2TokenScope()).thenReturn(SCOPE);
        when(dtrConfig.getOauth2ClientSecretPath()).thenReturn(CLIENT_SECRET_VAULT_PATH);
        when(dtrConfig.getOauth2TokenEndpointUrl()).thenReturn(LOCALHOST_TOKEN_ENDPOINT);
        when(vault.resolveSecret(CLIENT_SECRET_VAULT_PATH)).thenReturn(CLIENT_SECRET);

        final var requestMatcher = new RequestMatcher(
                LOCALHOST_TOKEN_ENDPOINT, ENCODED_CLIENT_ID, ENCODED_CLIENT_SECRET, ENCODED_SCOPE);
        when(httpClient.execute(argThat(requestMatcher))).thenReturn(httpResponse);
        when(httpResponse.isSuccessful()).thenReturn(true);
        when(httpResponse.body()).thenReturn(responseBody);
        when(responseBody.string()).thenReturn(RESPONSE_BODY);
        when(typeManager.readValue(RESPONSE_BODY, MAP_TYPE_REFERENCE))
                .thenReturn(new HashMap<>(Map.of(ACCESS_TOKEN, DUMMY_TOKEN)));

        //when
        final String actual = underTest.getBearerToken(SCOPE);

        //then
        assertThat(actual).isEqualTo(DUMMY_TOKEN);
        final var inOrder = inOrder(vault, httpClient, httpResponse, responseBody, typeManager, monitor);
        inOrder.verify(vault).resolveSecret(CLIENT_SECRET_VAULT_PATH);
        inOrder.verify(httpClient).execute(argThat(requestMatcher));
        inOrder.verify(httpResponse).isSuccessful();
        inOrder.verify(httpResponse, atLeastOnce()).body();
        inOrder.verify(responseBody).string();
        inOrder.verify(typeManager).readValue(anyString(), eq(MAP_TYPE_REFERENCE));
    }

    @SuppressWarnings({"resource", "ResultOfMethodCallIgnored"})
    @Test
    void test_GetBearerToken_ShouldReturnNull_WhenOauth2ResponseIsNotSuccessful() throws IOException {
        //given
        when(dtrConfig.getOauth2ClientId()).thenReturn(CLIENT_ID);
        when(dtrConfig.getOauth2TokenScope()).thenReturn(SCOPE);
        when(dtrConfig.getOauth2ClientSecretPath()).thenReturn(CLIENT_SECRET_VAULT_PATH);
        when(dtrConfig.getOauth2TokenEndpointUrl()).thenReturn(LOCALHOST_TOKEN_ENDPOINT);
        when(vault.resolveSecret(CLIENT_SECRET_VAULT_PATH)).thenReturn(CLIENT_SECRET);

        final var requestMatcher = new RequestMatcher(
                LOCALHOST_TOKEN_ENDPOINT, ENCODED_CLIENT_ID, ENCODED_CLIENT_SECRET, ENCODED_SCOPE);
        when(httpClient.execute(argThat(requestMatcher))).thenReturn(httpResponse);
        when(httpResponse.isSuccessful()).thenReturn(false);

        //when
        final String actual = underTest.getBearerToken(SCOPE);

        //then
        assertThat(actual).isNull();
        final var inOrder = inOrder(vault, httpClient, httpResponse, responseBody, typeManager, monitor);
        inOrder.verify(vault).resolveSecret(CLIENT_SECRET_VAULT_PATH);
        inOrder.verify(httpClient).execute(argThat(requestMatcher));
        inOrder.verify(httpResponse).isSuccessful();
        inOrder.verify(monitor).severe(anyString());
        inOrder.verify(httpResponse, never()).body();
        inOrder.verify(responseBody, never()).string();
        inOrder.verify(typeManager, never()).readValue(anyString(), eq(MAP_TYPE_REFERENCE));
    }

    @SuppressWarnings({"resource", "ResultOfMethodCallIgnored"})
    @Test
    void test_GetBearerToken_ShouldReturnNull_WhenOauth2ResponseHasNoBody() throws IOException {
        //given
        when(dtrConfig.getOauth2ClientId()).thenReturn(CLIENT_ID);
        when(dtrConfig.getOauth2TokenScope()).thenReturn(SCOPE);
        when(dtrConfig.getOauth2ClientSecretPath()).thenReturn(CLIENT_SECRET_VAULT_PATH);
        when(dtrConfig.getOauth2TokenEndpointUrl()).thenReturn(LOCALHOST_TOKEN_ENDPOINT);
        when(vault.resolveSecret(CLIENT_SECRET_VAULT_PATH)).thenReturn(CLIENT_SECRET);

        final var requestMatcher = new RequestMatcher(
                LOCALHOST_TOKEN_ENDPOINT, ENCODED_CLIENT_ID, ENCODED_CLIENT_SECRET, ENCODED_SCOPE);
        when(httpClient.execute(argThat(requestMatcher))).thenReturn(httpResponse);
        when(httpResponse.isSuccessful()).thenReturn(true);
        when(httpResponse.body()).thenReturn(null);

        //when
        final String actual = underTest.getBearerToken(SCOPE);

        //then
        assertThat(actual).isNull();
        final var inOrder = inOrder(vault, httpClient, httpResponse, responseBody, typeManager, monitor);
        inOrder.verify(vault).resolveSecret(CLIENT_SECRET_VAULT_PATH);
        inOrder.verify(httpClient).execute(argThat(requestMatcher));
        inOrder.verify(httpResponse).isSuccessful();
        inOrder.verify(httpResponse).body();
        inOrder.verify(monitor).severe(anyString());
        inOrder.verify(responseBody, never()).string();
        inOrder.verify(typeManager, never()).readValue(anyString(), eq(MAP_TYPE_REFERENCE));
    }

    @SuppressWarnings({"resource", "ResultOfMethodCallIgnored"})
    @Test
    void test_GetBearerToken_ShouldReturnNull_WhenOauth2ResponseHasNoTokenBody() throws IOException {
        //given
        when(dtrConfig.getOauth2ClientId()).thenReturn(CLIENT_ID);
        when(dtrConfig.getOauth2TokenScope()).thenReturn(SCOPE);
        when(dtrConfig.getOauth2ClientSecretPath()).thenReturn(CLIENT_SECRET_VAULT_PATH);
        when(dtrConfig.getOauth2TokenEndpointUrl()).thenReturn(LOCALHOST_TOKEN_ENDPOINT);
        when(vault.resolveSecret(CLIENT_SECRET_VAULT_PATH)).thenReturn(CLIENT_SECRET);

        final var requestMatcher = new RequestMatcher(
                LOCALHOST_TOKEN_ENDPOINT, ENCODED_CLIENT_ID, ENCODED_CLIENT_SECRET, ENCODED_SCOPE);
        when(httpClient.execute(argThat(requestMatcher))).thenReturn(httpResponse);
        when(httpResponse.isSuccessful()).thenReturn(true);
        when(httpResponse.body()).thenReturn(responseBody);
        when(responseBody.string()).thenReturn("{}");
        when(typeManager.readValue("{}", MAP_TYPE_REFERENCE))
                .thenReturn(new HashMap<>());

        //when
        final String actual = underTest.getBearerToken(SCOPE);

        //then
        assertThat(actual).isNull();
        final var inOrder = inOrder(vault, httpClient, httpResponse, responseBody, typeManager, monitor);
        inOrder.verify(vault).resolveSecret(CLIENT_SECRET_VAULT_PATH);
        inOrder.verify(httpClient).execute(argThat(requestMatcher));
        inOrder.verify(httpResponse).isSuccessful();
        inOrder.verify(httpResponse).body();
        inOrder.verify(responseBody).string();
        inOrder.verify(typeManager).readValue(anyString(), eq(MAP_TYPE_REFERENCE));
        inOrder.verify(monitor).severe(anyString());
    }

    @SuppressWarnings({"resource", "ResultOfMethodCallIgnored"})
    @Test
    void test_GetBearerToken_ShouldThrowException_WhenSecretCannotBeFetchedFromVault() throws IOException {
        //given
        when(dtrConfig.getOauth2ClientId()).thenReturn(CLIENT_ID);
        when(dtrConfig.getOauth2TokenScope()).thenReturn(SCOPE);
        when(dtrConfig.getOauth2ClientSecretPath()).thenReturn(CLIENT_SECRET_VAULT_PATH);
        when(dtrConfig.getOauth2TokenEndpointUrl()).thenReturn(LOCALHOST_TOKEN_ENDPOINT);
        when(vault.resolveSecret(CLIENT_SECRET_VAULT_PATH)).thenReturn(null);

        //when
        assertThatExceptionOfType(AccessControlServerException.class).isThrownBy(() -> underTest.getBearerToken(SCOPE));

        //then
        final var inOrder = inOrder(vault, httpClient, httpResponse, responseBody, typeManager, monitor);
        inOrder.verify(vault).resolveSecret(CLIENT_SECRET_VAULT_PATH);
        inOrder.verify(httpClient, never()).execute(any());
        inOrder.verify(httpResponse, never()).isSuccessful();
        inOrder.verify(httpResponse, never()).body();
        inOrder.verify(responseBody, never()).string();
        inOrder.verify(typeManager, never()).readValue(anyString(), eq(MAP_TYPE_REFERENCE));
    }

    private record RequestMatcher(String url, String clientId, String clientSecret,
                                  String scope) implements ArgumentMatcher<Request> {
        @SuppressWarnings("DataFlowIssue")
        @Override
        public boolean matches(final Request request) {
            final boolean methodMatched = POST.equals(request.method());
            final boolean urlMatched = url.equals(request.url().url().toString());
            final FormBody body = (FormBody) request.body();
            final boolean contentTypeHeaderFound = body.contentType().toString().equals(request.header(CONTENT_TYPE));
            final Map<String, String> expectedParameterMap = Map.of(
                    "client_id", clientId,
                    "client_secret", clientSecret,
                    "scope", scope,
                    "grant_type", "client_credentials");
            boolean bodyMatched = body.size() == expectedParameterMap.size();
            for (int i = 0; i < body.size(); i++) {
                bodyMatched &= expectedParameterMap.get(body.name(i)).equals(body.encodedValue(i));
            }
            return methodMatched && urlMatched && contentTypeHeaderFound && bodyMatched;
        }
    }

}